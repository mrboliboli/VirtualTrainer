from __future__ import annotations

import base64
import os
from dataclasses import dataclass
from pathlib import Path

from cryptography.hazmat.primitives.ciphers.aead import AESGCM


@dataclass(frozen=True)
class StockageSessionChiffree:
    fichier: Path
    cle: bytes

    CONTEXTE = b"pace-garmin-session-v1"

    @classmethod
    def depuis_base64(cls, fichier: Path, cle_base64: str) -> StockageSessionChiffree:
        cle = base64.urlsafe_b64decode(cle_base64)
        if len(cle) != 32:
            raise ValueError("La clé de chiffrement doit contenir exactement 32 octets")
        return cls(fichier=fichier, cle=cle)

    def existe(self) -> bool:
        return self.fichier.is_file()

    def enregistrer(self, contenu: bytes) -> None:
        nonce = os.urandom(12)
        chiffre = AESGCM(self.cle).encrypt(nonce, contenu, self.CONTEXTE)
        self.fichier.parent.mkdir(mode=0o700, parents=True, exist_ok=True)
        temporaire = self.fichier.with_suffix(".tmp")
        temporaire.write_bytes(nonce + chiffre)
        temporaire.chmod(0o600)
        temporaire.replace(self.fichier)

    def lire(self) -> bytes:
        enveloppe = self.fichier.read_bytes()
        if len(enveloppe) < 29:
            raise ValueError("La session chiffrée est invalide")
        return AESGCM(self.cle).decrypt(enveloppe[:12], enveloppe[12:], self.CONTEXTE)

    def supprimer(self) -> None:
        self.fichier.unlink(missing_ok=True)
