from __future__ import annotations

from functools import lru_cache
from pathlib import Path

from pydantic import Field, SecretStr
from pydantic_settings import BaseSettings, SettingsConfigDict


class Configuration(BaseSettings):
    """Configuration validée au démarrage, fournie exclusivement par l'environnement."""

    model_config = SettingsConfigDict(env_prefix="PACE_GARMIN_", extra="ignore")

    cle_service: SecretStr = Field(min_length=32)
    cle_chiffrement_base64: SecretStr
    fichier_session: Path = Path("/donnees/session.enc")
    requetes_par_minute: int = Field(default=6, ge=1, le=30)
    connecteur_actif: bool = False


@lru_cache
def obtenir_configuration() -> Configuration:
    return Configuration()  # type: ignore[call-arg]
