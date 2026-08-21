import io
import json
import zipfile
from datetime import date

import pytest

from pace_garmin.adaptateur import AdaptateurGarminLectureSeule
from pace_garmin.erreurs import (
    LimiteFrequenceAtteinte,
    MfaRequise,
    ProtectionGarminDetectee,
    TypeErreur,
)
from pace_garmin.modeles import EtatSession


class FauxStockage:
    contenu: bytes | None = None

    def existe(self) -> bool:
        return self.contenu is not None

    def enregistrer(self, contenu: bytes) -> None:
        self.contenu = contenu

    def lire(self) -> bytes:
        assert self.contenu is not None
        return self.contenu

    def supprimer(self) -> None:
        self.contenu = None


class FauxClient:
    def __init__(self, demande_mfa=False):
        self.demande_mfa = demande_mfa
        self.password = "secret-ephemere"

    def login(self, *args, **kwargs):
        return ({"contexte": "anonyme"}, None) if self.demande_mfa else (None, None)

    def resume_login(self, etat, code):
        assert etat == {"contexte": "anonyme"}
        assert code == "123456"

    @property
    def client(self):
        return self

    def dumps(self):
        return '{"session":"anonyme"}'

    def get_activities(self, debut, limite):
        return [{"activityId": 42, "activityName": "Course"}]

    def get_activities_by_date(self, debut, fin):
        return [{"activityId": 43, "activityName": "Course ancienne"}]

    def get_activity(self, identifiant):
        return {"activityId": identifiant, "activityName": "Course"}

    def download_activity(self, identifiant, dl_fmt):
        tampon = io.BytesIO()
        with zipfile.ZipFile(tampon, "w") as archive:
            archive.writestr(f"{identifiant}.fit", b".FIT donnees anonymes")
        return tampon.getvalue()


def test_commencer_connexion_devrait_conserver_session_chiffree_quand_connexion_reussit():
    # ÉTANT DONNÉ
    stockage = FauxStockage()
    adaptateur = AdaptateurGarminLectureSeule(stockage, lambda *args, **kwargs: FauxClient())

    # QUAND
    etat = adaptateur.commencer_connexion("personne@example.invalid", "secret-ephemere")

    # ALORS
    assert etat == EtatSession.CONNECTEE
    assert stockage.contenu is not None
    assert json.loads(stockage.contenu)["session"] == "anonyme"


def test_commencer_connexion_devrait_demander_mfa_quand_garmin_exige_code():
    # ÉTANT DONNÉ
    adaptateur = AdaptateurGarminLectureSeule(
        FauxStockage(), lambda *args, **kwargs: FauxClient(demande_mfa=True)
    )

    # QUAND / ALORS
    with pytest.raises(MfaRequise):
        adaptateur.commencer_connexion("personne@example.invalid", "secret-ephemere")
    assert adaptateur.etat() == EtatSession.MFA_REQUISE


def test_etat_devrait_purger_mfa_quand_delai_expire_spontanement():
    # ÉTANT DONNÉ
    instant = [100.0]
    client = FauxClient(demande_mfa=True)
    adaptateur = AdaptateurGarminLectureSeule(
        FauxStockage(), lambda *args, **kwargs: client, horloge=lambda: instant[0]
    )
    with pytest.raises(MfaRequise):
        adaptateur.commencer_connexion("personne@example.invalid", "secret-ephemere")
    contexte = adaptateur._etat_mfa

    # QUAND
    instant[0] += AdaptateurGarminLectureSeule.DUREE_MFA_SECONDES
    etat = adaptateur.etat()

    # ALORS
    assert etat == EtatSession.ABSENTE
    assert client.password is None
    assert contexte == {}


def test_abandonner_mfa_devrait_effacer_secrets_avant_de_retirer_references():
    # ÉTANT DONNÉ
    instant = [100.0]
    client = FauxClient(demande_mfa=True)
    adaptateur = AdaptateurGarminLectureSeule(
        FauxStockage(), lambda *args, **kwargs: client, horloge=lambda: instant[0]
    )
    with pytest.raises(MfaRequise):
        adaptateur.commencer_connexion("personne@example.invalid", "secret-ephemere")
    contexte = adaptateur._etat_mfa

    # QUAND
    adaptateur.abandonner_mfa()

    # ALORS
    assert client.password is None
    assert contexte == {}
    assert adaptateur.etat() == EtatSession.ABSENTE


def test_activites_devrait_utiliser_plage_quand_backfill_demande():
    # ÉTANT DONNÉ
    stockage = FauxStockage()
    stockage.contenu = b"{}"
    client = FauxClient()
    adaptateur = AdaptateurGarminLectureSeule(stockage, lambda *args, **kwargs: client)

    # QUAND
    activites = adaptateur.activites(10, date(2026, 8, 1), date(2026, 8, 21))

    # ALORS
    assert [activite.identifiant for activite in activites] == ["43"]


def test_convertir_erreur_devrait_arreter_quand_limite_garmin():
    # ÉTANT DONNÉ
    erreur = RuntimeError("HTTP 429")

    # QUAND / ALORS
    with pytest.raises(LimiteFrequenceAtteinte):
        AdaptateurGarminLectureSeule._convertir_erreur(erreur)


@pytest.mark.parametrize("message", ["CAPTCHA required", "Cloudflare challenge"])
def test_convertir_erreur_devrait_etre_definitive_quand_protection_detectee(message):
    # ÉTANT DONNÉ
    erreur = RuntimeError(message)

    # QUAND / ALORS
    with pytest.raises(ProtectionGarminDetectee) as capture:
        AdaptateurGarminLectureSeule._convertir_erreur(erreur)
    assert capture.value.type_erreur == TypeErreur.DEFINITIVE


def test_fit_devrait_extraire_fit_quand_garmin_retourne_archive():
    # ÉTANT DONNÉ
    stockage = FauxStockage()
    stockage.contenu = b"{}"
    adaptateur = AdaptateurGarminLectureSeule(stockage, lambda *args, **kwargs: FauxClient())

    # QUAND
    contenu = adaptateur.fit("42")

    # ALORS
    assert contenu == b".FIT donnees anonymes"
