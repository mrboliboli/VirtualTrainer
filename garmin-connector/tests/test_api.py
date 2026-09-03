from fastapi.testclient import TestClient

from pace_garmin import api
from pace_garmin.modeles import EtatSession


class FauxAdaptateur:
    def __init__(self) -> None:
        self.connexion_recue: tuple[str, str] | None = None
        self.code_mfa_recu: str | None = None

    def commencer_connexion(self, courriel: str, mot_de_passe: str) -> EtatSession:
        self.connexion_recue = (courriel, mot_de_passe)
        return EtatSession.CONNECTEE

    def terminer_mfa(self, code: str) -> EtatSession:
        self.code_mfa_recu = code
        return EtatSession.CONNECTEE

    def abandonner_mfa(self) -> None:
        pass


def test_connexion_devrait_transmettre_corps_json_a_adaptateur(monkeypatch) -> None:
    # ÉTANT DONNÉ
    faux_adaptateur = FauxAdaptateur()
    monkeypatch.setattr(api, "adaptateur", faux_adaptateur)
    entetes = {"X-Cle-Service": api.configuration.cle_service.get_secret_value()}

    # QUAND
    with TestClient(api.app) as client:
        reponse = client.post(
            "/interne/v1/session/connexion",
            headers=entetes,
            json={
                "courriel": "personne@example.invalid",
                "mot_de_passe": "secret-ephemere",
            },
        )

    # ALORS
    assert reponse.status_code == 200
    assert reponse.json()["etat"] == "CONNECTEE"
    assert faux_adaptateur.connexion_recue == (
        "personne@example.invalid",
        "secret-ephemere",
    )


def test_mfa_devrait_transmettre_corps_json_a_adaptateur(monkeypatch) -> None:
    # ÉTANT DONNÉ
    faux_adaptateur = FauxAdaptateur()
    monkeypatch.setattr(api, "adaptateur", faux_adaptateur)
    entetes = {"X-Cle-Service": api.configuration.cle_service.get_secret_value()}

    # QUAND
    with TestClient(api.app) as client:
        reponse = client.post(
            "/interne/v1/session/mfa",
            headers=entetes,
            json={"code": "123456"},
        )

    # ALORS
    assert reponse.status_code == 200
    assert reponse.json()["etat"] == "CONNECTEE"
    assert faux_adaptateur.code_mfa_recu == "123456"


def test_openapi_devrait_declarer_connexion_et_mfa_comme_corps_json() -> None:
    # ÉTANT DONNÉ / QUAND
    schema = api.app.openapi()

    # ALORS
    for chemin in ("/interne/v1/session/connexion", "/interne/v1/session/mfa"):
        operation = schema["paths"][chemin]["post"]
        assert "requestBody" in operation
        assert "application/json" in operation["requestBody"]["content"]
        assert all(parametre["name"] != "demande" for parametre in operation.get("parameters", []))


def test_connexion_devrait_expliquer_validation_sans_exposer_valeurs() -> None:
    # ÉTANT DONNÉ
    entetes = {"X-Cle-Service": api.configuration.cle_service.get_secret_value()}
    secret = "secret-qui-ne-doit-pas-apparaitre"
    courriel = "adresse-qui-ne-doit-pas-apparaitre"

    # QUAND
    with TestClient(api.app) as client:
        reponse = client.post(
            "/interne/v1/session/connexion",
            headers=entetes,
            json={"email": courriel, "password": secret},
        )

    # ALORS
    corps = reponse.json()
    assert reponse.status_code == 422
    assert corps["code"] == "REQUETE_INVALIDE"
    assert corps["type"] == "VALIDATION"
    assert "body.courriel (missing)" in corps["message"]
    assert "body.mot_de_passe (missing)" in corps["message"]
    assert secret not in reponse.text
    assert courriel not in reponse.text
    assert "input" not in reponse.text


def test_connexion_devrait_masquer_valeur_quand_type_est_invalide() -> None:
    # ÉTANT DONNÉ
    entetes = {"X-Cle-Service": api.configuration.cle_service.get_secret_value()}
    valeur_sensible = "valeur-sensible"

    # QUAND
    with TestClient(api.app) as client:
        reponse = client.post(
            "/interne/v1/session/connexion",
            headers=entetes,
            json={"courriel": valeur_sensible, "mot_de_passe": {}},
        )

    # ALORS
    assert reponse.status_code == 422
    assert "body.mot_de_passe (string_type)" in reponse.json()["message"]
    assert valeur_sensible not in reponse.text
