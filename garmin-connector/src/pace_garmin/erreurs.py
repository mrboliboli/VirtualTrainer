from __future__ import annotations

from enum import StrEnum


class TypeErreur(StrEnum):
    TEMPORAIRE = "TEMPORAIRE"
    DEFINITIVE = "DEFINITIVE"
    AUTHENTIFICATION = "AUTHENTIFICATION"
    MFA_REQUISE = "MFA_REQUISE"
    LIMITE_FREQUENCE = "LIMITE_FREQUENCE"
    INDISPONIBLE = "INDISPONIBLE"


class ErreurConnecteur(Exception):
    type_erreur = TypeErreur.DEFINITIVE
    code = "ERREUR_CONNECTEUR"
    statut_http = 502

    def __init__(self, message: str) -> None:
        super().__init__(message)
        self.message = message


class ConnecteurDesactive(ErreurConnecteur):
    type_erreur = TypeErreur.INDISPONIBLE
    code = "CONNECTEUR_DESACTIVE"
    statut_http = 503


class SessionAbsente(ErreurConnecteur):
    type_erreur = TypeErreur.AUTHENTIFICATION
    code = "SESSION_ABSENTE"
    statut_http = 401


class AuthentificationRefusee(ErreurConnecteur):
    type_erreur = TypeErreur.AUTHENTIFICATION
    code = "AUTHENTIFICATION_REFUSEE"
    statut_http = 401


class MfaRequise(ErreurConnecteur):
    type_erreur = TypeErreur.MFA_REQUISE
    code = "MFA_REQUISE"
    statut_http = 202


class ProtectionGarminDetectee(ErreurConnecteur):
    type_erreur = TypeErreur.DEFINITIVE
    code = "PROTECTION_GARMIN_DETECTEE"
    statut_http = 503


class LimiteFrequenceAtteinte(ErreurConnecteur):
    type_erreur = TypeErreur.LIMITE_FREQUENCE
    code = "LIMITE_FREQUENCE"
    statut_http = 429


class CapaciteIndisponible(ErreurConnecteur):
    type_erreur = TypeErreur.INDISPONIBLE
    code = "CAPACITE_INDISPONIBLE"
    statut_http = 501
