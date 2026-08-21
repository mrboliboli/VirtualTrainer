import base64
import os

import pytest
from cryptography.exceptions import InvalidTag

from pace_garmin.chiffrement import StockageSessionChiffree


def test_enregistrer_devrait_restituer_session_quand_cle_correcte(tmp_path):
    # ÉTANT DONNÉ
    stockage = StockageSessionChiffree(tmp_path / "session.enc", os.urandom(32))

    # QUAND
    stockage.enregistrer(b'{"jeton":"contenu sensible"}')

    # ALORS
    assert stockage.lire() == b'{"jeton":"contenu sensible"}'
    assert b"contenu sensible" not in stockage.fichier.read_bytes()
    assert stockage.fichier.stat().st_mode & 0o777 == 0o600


def test_lire_devrait_refuser_session_quand_enveloppe_modifiee(tmp_path):
    # ÉTANT DONNÉ
    stockage = StockageSessionChiffree(tmp_path / "session.enc", os.urandom(32))
    stockage.enregistrer(b"session")
    contenu = bytearray(stockage.fichier.read_bytes())
    contenu[-1] ^= 1
    stockage.fichier.write_bytes(contenu)

    # QUAND / ALORS
    with pytest.raises(InvalidTag):
        stockage.lire()


def test_depuis_base64_devrait_refuser_cle_quand_taille_invalide(tmp_path):
    # ÉTANT DONNÉ
    cle = base64.urlsafe_b64encode(b"trop-courte").decode()

    # QUAND / ALORS
    with pytest.raises(ValueError, match="32 octets"):
        StockageSessionChiffree.depuis_base64(tmp_path / "session.enc", cle)
