import base64
import hmac
import logging
import time
import uuid
from collections.abc import AsyncIterator, Awaitable, Callable
from contextlib import asynccontextmanager
from datetime import date
from typing import Annotated

from fastapi import Body, Depends, FastAPI, Header, Query, Request, Response
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from slowapi import Limiter
from slowapi.errors import RateLimitExceeded
from slowapi.util import get_remote_address

from .adaptateur import AdaptateurGarminLectureSeule
from .chiffrement import StockageSessionChiffree
from .configuration import Configuration, obtenir_configuration
from .erreurs import ConnecteurDesactive, ErreurConnecteur
from .modeles import (
    Activite,
    DemandeConnexion,
    DemandeMfa,
    ErreurApi,
    ListeActivites,
    ReponseSession,
    Sante,
)

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s %(message)s")
LOGGER = logging.getLogger(__name__)
limiteur = Limiter(key_func=get_remote_address)


def construire_adaptateur(configuration: Configuration) -> AdaptateurGarminLectureSeule:
    stockage = StockageSessionChiffree.depuis_base64(
        configuration.fichier_session,
        configuration.cle_chiffrement_base64.get_secret_value(),
    )
    return AdaptateurGarminLectureSeule(stockage)


configuration = obtenir_configuration()
adaptateur = construire_adaptateur(configuration)


@asynccontextmanager
async def duree_application(_: FastAPI) -> AsyncIterator[None]:
    LOGGER.info("Démarrage du connecteur Garmin personnel")
    yield
    adaptateur.abandonner_mfa()
    LOGGER.info("Arrêt du connecteur Garmin personnel")


app = FastAPI(
    title="Connecteur Garmin personnel de Pace",
    version="0.1.0",
    docs_url=None,
    redoc_url=None,
    lifespan=duree_application,
)
app.state.limiter = limiteur


def verifier_service(
    x_cle_service: str = Header(alias="X-Cle-Service"),
) -> None:
    configuration_courante = obtenir_configuration()
    attendu = configuration_courante.cle_service.get_secret_value()
    if not hmac.compare_digest(x_cle_service, attendu):
        from .erreurs import AuthentificationRefusee

        raise AuthentificationRefusee("L'appelant interne n'est pas autorisé")
    if not configuration_courante.connecteur_actif:
        raise ConnecteurDesactive("Le connecteur Garmin personnel est désactivé")


@app.middleware("http")
async def ajouter_correlation(
    request: Request,
    suivant: Callable[[Request], Awaitable[Response]],
) -> Response:
    correlation = request.headers.get("X-Correlation-Id", str(uuid.uuid4()))[:64]
    debut = time.monotonic()
    request.state.correlation_id = correlation
    reponse = await suivant(request)
    reponse.headers["X-Correlation-Id"] = correlation
    LOGGER.info(
        "Appel interne terminé : méthode=%s chemin=%s statut=%s durée_ms=%s correlation=%s",
        request.method,
        request.url.path,
        reponse.status_code,
        round((time.monotonic() - debut) * 1000),
        correlation,
    )
    return reponse


@app.exception_handler(ErreurConnecteur)
async def traiter_erreur(request: Request, erreur: ErreurConnecteur) -> JSONResponse:
    corps = ErreurApi(
        code=erreur.code,
        type=erreur.type_erreur,
        message=erreur.message,
        correlation_id=request.state.correlation_id,
    )
    return JSONResponse(status_code=erreur.statut_http, content=corps.model_dump())


@app.exception_handler(RateLimitExceeded)
async def traiter_limite(request: Request, _: RateLimitExceeded) -> JSONResponse:
    corps = ErreurApi(
        code="LIMITE_INTERNE",
        type="LIMITE_FREQUENCE",
        message="La limite prudente du connecteur est atteinte",
        correlation_id=request.state.correlation_id,
    )
    return JSONResponse(status_code=429, content=corps.model_dump())


@app.exception_handler(RequestValidationError)
async def traiter_validation(request: Request, erreur: RequestValidationError) -> JSONResponse:
    details = sorted(
        {
            f"{'.'.join(str(element) for element in entree['loc'])} ({entree['type']})"
            for entree in erreur.errors()
        }
    )
    corps = ErreurApi(
        code="REQUETE_INVALIDE",
        type="VALIDATION",
        message=f"Requête invalide : {', '.join(details)}",
        correlation_id=request.state.correlation_id,
    )
    return JSONResponse(status_code=422, content=corps.model_dump())


@app.get("/interne/v1/sante", response_model=Sante)
def sante() -> Sante:
    return Sante(connecteur_actif=configuration.connecteur_actif)


@app.get(
    "/interne/v1/session",
    response_model=ReponseSession,
    dependencies=[Depends(verifier_service)],
)
def etat_session() -> ReponseSession:
    return ReponseSession(etat=adaptateur.etat(), message="État de la session locale")


@app.post(
    "/interne/v1/session/connexion",
    response_model=ReponseSession,
    dependencies=[Depends(verifier_service)],
)
@limiteur.limit(lambda: f"{configuration.requetes_par_minute}/minute")
def connexion(
    request: Request,
    demande: Annotated[DemandeConnexion, Body()],
) -> ReponseSession:
    del request
    etat = adaptateur.commencer_connexion(
        demande.courriel,
        demande.mot_de_passe.get_secret_value(),
    )
    return ReponseSession(etat=etat, message="Session Garmin créée")


@app.post(
    "/interne/v1/session/mfa",
    response_model=ReponseSession,
    dependencies=[Depends(verifier_service)],
)
@limiteur.limit(lambda: f"{configuration.requetes_par_minute}/minute")
def mfa(
    request: Request,
    demande: Annotated[DemandeMfa, Body()],
) -> ReponseSession:
    del request
    etat = adaptateur.terminer_mfa(demande.code.get_secret_value())
    return ReponseSession(etat=etat, message="Double authentification terminée")


@app.delete("/interne/v1/session", status_code=204, dependencies=[Depends(verifier_service)])
def supprimer_session() -> Response:
    adaptateur.supprimer_session_locale()
    return Response(status_code=204)


@app.get(
    "/interne/v1/activites",
    response_model=ListeActivites,
    dependencies=[Depends(verifier_service)],
)
@limiteur.limit(lambda: f"{configuration.requetes_par_minute}/minute")
def activites(
    request: Request,
    limite: int = Query(default=20, ge=1, le=100),
    debut: date | None = None,
    fin: date | None = None,
) -> ListeActivites:
    del request
    resultat = adaptateur.activites(limite, debut, fin)
    return ListeActivites(activites=resultat, limite=limite, debut=debut, fin=fin)


@app.get("/interne/v1/activites/{identifiant}", dependencies=[Depends(verifier_service)])
@limiteur.limit(lambda: f"{configuration.requetes_par_minute}/minute")
def detail(request: Request, identifiant: str) -> Activite:
    del request
    return adaptateur.detail(identifiant)


@app.get("/interne/v1/activites/{identifiant}/fit", dependencies=[Depends(verifier_service)])
@limiteur.limit(lambda: f"{configuration.requetes_par_minute}/minute")
def fit(request: Request, identifiant: str) -> Response:
    del request
    contenu = adaptateur.fit(identifiant)
    empreinte = base64.urlsafe_b64encode(__import__("hashlib").sha256(contenu).digest()).decode()
    return Response(
        content=contenu,
        media_type="application/vnd.ant.fit",
        headers={"ETag": f'"sha256-{empreinte}"'},
    )
