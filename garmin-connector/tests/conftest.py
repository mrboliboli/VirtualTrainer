"""Configuration hermétique des tests du connecteur Garmin."""

from __future__ import annotations

import os

# Ces valeurs sont exclusivement destinées aux doubles de test. Les définir avant
# l'import de pace_garmin.api permet d'exécuter simplement `.venv/bin/pytest` sans
# dépendre d'un fichier .env local ni d'un secret réel.
os.environ.setdefault("PACE_GARMIN_CLE_SERVICE", "cle-de-service-reservee-aux-tests-0001")
os.environ.setdefault(
    "PACE_GARMIN_CLE_CHIFFREMENT_BASE64",
    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
)
os.environ.setdefault("PACE_GARMIN_CONNECTEUR_ACTIF", "true")
