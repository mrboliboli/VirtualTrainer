from __future__ import annotations

import io
import logging
import threading
import time
import zipfile
from collections.abc import Callable
from datetime import date
from typing import Any, Protocol

from garminconnect import Garmin

from .chiffrement import StockageSessionChiffree
from .erreurs import (
    AuthentificationRefusee,
    CapaciteIndisponible,
    LimiteFrequenceAtteinte,
    MfaRequise,
    ProtectionGarminDetectee,
    SessionAbsente,
)
from .modeles import Activite, EtatSession

LOGGER = logging.getLogger(__name__)


class ClientGarmin(Protocol):
    client: ClientSessionGarmin

    def login(self, *args: Any, **kwargs: Any) -> Any: ...
    def resume_login(self, etat: dict[str, Any], code: str) -> Any: ...
    def get_activities(self, debut: int, limite: int) -> list[dict[str, Any]]: ...
    def get_activities_by_date(self, debut: str, fin: str) -> list[dict[str, Any]]: ...
    def get_activity(self, identifiant: str) -> dict[str, Any]: ...
    def download_activity(self, identifiant: str, dl_fmt: Any) -> bytes: ...


FabriqueClient = Callable[..., ClientGarmin]


class ClientSessionGarmin(Protocol):
    def dumps(self) -> str: ...


class AdaptateurGarminLectureSeule:
    """Liste blanche des seules opérations Garmin autorisées par Pace."""

    def __init__(
        self,
        stockage: StockageSessionChiffree,
        fabrique: FabriqueClient = Garmin,
        horloge: Callable[[], float] = time.monotonic,
    ) -> None:
        self._stockage = stockage
        self._fabrique = fabrique
        self._horloge = horloge
        self._client_mfa: ClientGarmin | None = None
        self._etat_mfa: dict[str, Any] | None = None
        self._expiration_mfa: float | None = None
        self._verrou = threading.Lock()

    def etat(self) -> EtatSession:
        with self._verrou:
            if self._mfa_expiree():
                self._purger_mfa()
            if self._client_mfa is not None:
                return EtatSession.MFA_REQUISE
            return EtatSession.CONNECTEE if self._stockage.existe() else EtatSession.ABSENTE

    def commencer_connexion(self, courriel: str, mot_de_passe: str) -> EtatSession:
        with self._verrou:
            client = self._fabrique(
                courriel,
                mot_de_passe,
                return_on_mfa=True,
                retry_attempts=1,
            )
            try:
                resultat = client.login()
                etat_mfa = self._extraire_etat_mfa(resultat)
                if etat_mfa is not None:
                    self._client_mfa = client
                    self._etat_mfa = etat_mfa
                    self._expiration_mfa = self._horloge() + self.DUREE_MFA_SECONDES
                    raise MfaRequise("Un code de double authentification est requis")
                self._enregistrer_session(client)
                self._effacer_mot_de_passe(client)
                return EtatSession.CONNECTEE
            except MfaRequise:
                raise
            except Exception as erreur:
                self._convertir_erreur(erreur)
                raise AssertionError("Erreur Garmin non convertie") from erreur

    def terminer_mfa(self, code: str) -> EtatSession:
        with self._verrou:
            if (
                self._client_mfa is None
                or self._etat_mfa is None
                or self._expiration_mfa is None
                or self._mfa_expiree()
            ):
                self._purger_mfa()
                raise SessionAbsente("Aucune connexion MFA n'est en attente")
            client = self._client_mfa
            etat_mfa = self._etat_mfa
            try:
                client.resume_login(etat_mfa, code)
                self._enregistrer_session(client)
                self._purger_mfa()
                return EtatSession.CONNECTEE
            except Exception as erreur:
                self._purger_mfa()
                self._convertir_erreur(erreur)
                raise AssertionError("Erreur Garmin non convertie") from erreur

    def activites(self, limite: int, debut: date | None, fin: date | None) -> list[Activite]:
        with self._verrou:
            client = self._restaurer_client()
            try:
                donnees = (
                    client.get_activities_by_date(debut.isoformat(), fin.isoformat())
                    if debut is not None and fin is not None
                    else client.get_activities(0, limite)
                )
                return [self._vers_activite(element) for element in donnees[:limite]]
            except Exception as erreur:
                self._convertir_erreur(erreur)
                raise AssertionError("Erreur Garmin non convertie") from erreur

    def detail(self, identifiant: str) -> Activite:
        with self._verrou:
            client = self._restaurer_client()
            try:
                return self._vers_activite(client.get_activity(identifiant))
            except Exception as erreur:
                self._convertir_erreur(erreur)
                raise AssertionError("Erreur Garmin non convertie") from erreur

    def fit(self, identifiant: str) -> bytes:
        with self._verrou:
            client = self._restaurer_client()
            format_original = getattr(
                getattr(Garmin, "ActivityDownloadFormat", None),
                "ORIGINAL",
                None,
            )
            if format_original is None or not hasattr(client, "download_activity"):
                raise CapaciteIndisponible("Le téléchargement FIT n'est pas pris en charge")
            try:
                contenu = client.download_activity(identifiant, dl_fmt=format_original)
                return self._extraire_fit(contenu)
            except CapaciteIndisponible:
                raise
            except zipfile.BadZipFile as erreur:
                raise CapaciteIndisponible(
                    "Garmin n'a pas retourné un fichier FIT exploitable"
                ) from erreur
            except Exception as erreur:
                self._convertir_erreur(erreur)
                raise AssertionError("Erreur Garmin non convertie") from erreur

    def supprimer_session_locale(self) -> None:
        with self._verrou:
            self._purger_mfa()
            self._stockage.supprimer()

    def abandonner_mfa(self) -> None:
        with self._verrou:
            self._purger_mfa()

    def _mfa_expiree(self) -> bool:
        return self._expiration_mfa is not None and self._horloge() >= self._expiration_mfa

    def _purger_mfa(self) -> None:
        if self._client_mfa is not None:
            self._effacer_mot_de_passe(self._client_mfa)
        if self._etat_mfa is not None:
            self._etat_mfa.clear()
        self._client_mfa = None
        self._etat_mfa = None
        self._expiration_mfa = None

    def _enregistrer_session(self, client: ClientGarmin) -> None:
        jetons = client.client.dumps()
        self._stockage.enregistrer(jetons.encode("utf-8"))

    def _restaurer_client(self) -> ClientGarmin:
        if not self._stockage.existe():
            raise SessionAbsente("Aucune session Garmin locale n'est disponible")
        client = self._fabrique(retry_attempts=1)
        client.login(tokenstore=self._stockage.lire().decode("utf-8"))
        return client

    @staticmethod
    def _extraire_etat_mfa(resultat: Any) -> dict[str, Any] | None:
        if not isinstance(resultat, tuple) or not resultat:
            return None
        etat = resultat[0]
        return etat if isinstance(etat, dict) else None

    @staticmethod
    def _effacer_mot_de_passe(client: ClientGarmin) -> None:
        if hasattr(client, "password"):
            client.password = None

    @staticmethod
    def _vers_activite(donnees: dict[str, Any]) -> Activite:
        identifiant = donnees.get("activityId") or donnees.get("activityUUID")
        if identifiant is None:
            raise CapaciteIndisponible("Une activité Garmin ne contient aucun identifiant")
        return Activite(identifiant=str(identifiant), donnees=donnees)

    @classmethod
    def _extraire_fit(cls, contenu: bytes) -> bytes:
        if len(contenu) > cls.TAILLE_MAX_TELECHARGEMENT_OCTETS:
            raise CapaciteIndisponible("Le téléchargement Garmin dépasse la taille autorisée")
        if contenu.startswith(b"PK"):
            with zipfile.ZipFile(io.BytesIO(contenu)) as archive:
                entrees = archive.infolist()
                if len(entrees) > cls.NOMBRE_MAX_ENTREES_ZIP:
                    raise CapaciteIndisponible("L'archive Garmin contient trop d'entrées")
                entrees_fit = [
                    entree
                    for entree in entrees
                    if not entree.is_dir() and entree.filename.lower().endswith(".fit")
                ]
                if not entrees_fit:
                    raise CapaciteIndisponible("L'archive Garmin ne contient aucun fichier FIT")
                if len(entrees_fit) != 1:
                    raise CapaciteIndisponible("L'archive Garmin doit contenir un seul fichier FIT")
                entree = entrees_fit[0]
                cls._verifier_entree_fit(entree)
                return cls._lire_entree_fit(archive, entree)
        if contenu[:1] == b"." or b".FIT" in contenu[:16].upper():
            return contenu
        raise CapaciteIndisponible("Le contenu Garmin n'est ni un FIT ni une archive FIT")

    @classmethod
    def _verifier_entree_fit(cls, entree: zipfile.ZipInfo) -> None:
        if entree.flag_bits & 0x1:
            raise CapaciteIndisponible("L'archive Garmin contient un fichier FIT chiffré")
        if entree.compress_size > cls.TAILLE_MAX_ENTREE_COMPRESSEE_OCTETS:
            raise CapaciteIndisponible("Le fichier FIT compressé dépasse la taille autorisée")
        if entree.file_size > cls.TAILLE_MAX_FIT_DECOMPRESSE_OCTETS:
            raise CapaciteIndisponible("Le fichier FIT décompressé dépasse la taille autorisée")
        ratio = entree.file_size / max(entree.compress_size, 1)
        if ratio > cls.RATIO_MAX_DECOMPRESSION:
            raise CapaciteIndisponible("Le taux de compression de l'archive Garmin est anormal")

    @classmethod
    def _lire_entree_fit(
        cls,
        archive: zipfile.ZipFile,
        entree: zipfile.ZipInfo,
    ) -> bytes:
        resultat = io.BytesIO()
        taille = 0
        with archive.open(entree, "r") as flux:
            while bloc := flux.read(cls.TAILLE_BLOC_LECTURE_OCTETS):
                taille += len(bloc)
                if taille > cls.TAILLE_MAX_FIT_DECOMPRESSE_OCTETS:
                    raise CapaciteIndisponible(
                        "Le fichier FIT décompressé dépasse la taille autorisée"
                    )
                resultat.write(bloc)
        return resultat.getvalue()

    @staticmethod
    def _convertir_erreur(erreur: Exception) -> None:
        texte = str(erreur).lower()
        if "429" in texte or "rate limit" in texte:
            raise LimiteFrequenceAtteinte("Garmin limite temporairement les requêtes") from erreur
        if any(marqueur in texte for marqueur in ("cloudflare", "captcha", "403", "forbidden")):
            raise ProtectionGarminDetectee(
                "Garmin demande une vérification humaine ; aucun contournement n'est tenté"
            ) from erreur
        if "401" in texte or "unauthorized" in texte or "authentication" in texte:
            raise AuthentificationRefusee("Garmin a refusé l'authentification") from erreur
        LOGGER.warning("Échec d'une opération Garmin", exc_info=erreur)
        raise CapaciteIndisponible("L'opération Garmin est actuellement indisponible") from erreur

    DUREE_MFA_SECONDES = 300
    TAILLE_MAX_TELECHARGEMENT_OCTETS = 32 * 1024 * 1024
    NOMBRE_MAX_ENTREES_ZIP = 16
    TAILLE_MAX_ENTREE_COMPRESSEE_OCTETS = 32 * 1024 * 1024
    TAILLE_MAX_FIT_DECOMPRESSE_OCTETS = 128 * 1024 * 1024
    RATIO_MAX_DECOMPRESSION = 200
    TAILLE_BLOC_LECTURE_OCTETS = 64 * 1024
