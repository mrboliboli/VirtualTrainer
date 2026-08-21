from __future__ import annotations

from datetime import date
from enum import StrEnum
from typing import Any

from pydantic import BaseModel, ConfigDict, Field, SecretStr


class EtatSession(StrEnum):
    ABSENTE = "ABSENTE"
    MFA_REQUISE = "MFA_REQUISE"
    CONNECTEE = "CONNECTEE"


class Sante(BaseModel):
    statut: str = "OK"
    connecteur_actif: bool


class DemandeConnexion(BaseModel):
    model_config = ConfigDict(str_strip_whitespace=True)
    courriel: str = Field(min_length=3, max_length=254)
    mot_de_passe: SecretStr = Field(min_length=1, max_length=512)


class DemandeMfa(BaseModel):
    code: SecretStr = Field(min_length=4, max_length=12)


class ReponseSession(BaseModel):
    etat: EtatSession
    message: str


class Activite(BaseModel):
    identifiant: str
    donnees: dict[str, Any]


class ListeActivites(BaseModel):
    activites: list[Activite]
    limite: int
    debut: date | None = None
    fin: date | None = None


class ErreurApi(BaseModel):
    code: str
    type: str
    message: str
    correlation_id: str
