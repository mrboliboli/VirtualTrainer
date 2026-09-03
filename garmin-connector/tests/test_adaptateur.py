import io
import json
import zipfile
from datetime import date

import pytest

from pace_garmin.adaptateur import AdaptateurGarminLectureSeule
from pace_garmin.erreurs import (
    CapaciteIndisponible,
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


def _archive(**fichiers: bytes) -> bytes:
    tampon = io.BytesIO()
    with zipfile.ZipFile(tampon, "w", compression=zipfile.ZIP_DEFLATED) as archive:
        for nom, contenu in fichiers.items():
            archive.writestr(nom, contenu)
    return tampon.getvalue()


def test_extraire_fit_devrait_refuser_quand_telechargement_depasse_limite(monkeypatch):
    # ÉTANT DONNÉ
    monkeypatch.setattr(AdaptateurGarminLectureSeule, "TAILLE_MAX_TELECHARGEMENT_OCTETS", 8)

    # QUAND / ALORS
    with pytest.raises(CapaciteIndisponible, match="téléchargement"):
        AdaptateurGarminLectureSeule._extraire_fit(b".FIT trop long")


def test_extraire_fit_devrait_refuser_quand_archive_contient_plusieurs_fit():
    # ÉTANT DONNÉ
    contenu = _archive(**{"premier.fit": b".FIT 1", "second.fit": b".FIT 2"})

    # QUAND / ALORS
    with pytest.raises(CapaciteIndisponible, match="un seul fichier FIT"):
        AdaptateurGarminLectureSeule._extraire_fit(contenu)


def test_extraire_fit_devrait_refuser_quand_archive_contient_trop_entrees(monkeypatch):
    # ÉTANT DONNÉ
    monkeypatch.setattr(AdaptateurGarminLectureSeule, "NOMBRE_MAX_ENTREES_ZIP", 1)
    contenu = _archive(**{"course.fit": b".FIT", "note.txt": b"anonyme"})

    # QUAND / ALORS
    with pytest.raises(CapaciteIndisponible, match="trop d'entrées"):
        AdaptateurGarminLectureSeule._extraire_fit(contenu)


def test_extraire_fit_devrait_refuser_quand_taille_decompressee_depasse_limite(monkeypatch):
    # ÉTANT DONNÉ
    monkeypatch.setattr(AdaptateurGarminLectureSeule, "TAILLE_MAX_FIT_DECOMPRESSE_OCTETS", 8)
    contenu = _archive(**{"course.fit": b".FIT donnees trop longues"})

    # QUAND / ALORS
    with pytest.raises(CapaciteIndisponible, match="décompressé dépasse"):
        AdaptateurGarminLectureSeule._extraire_fit(contenu)


def test_extraire_fit_devrait_refuser_quand_taille_compressee_depasse_limite(monkeypatch):
    # ÉTANT DONNÉ
    monkeypatch.setattr(
        AdaptateurGarminLectureSeule,
        "TAILLE_MAX_ENTREE_COMPRESSEE_OCTETS",
        1,
    )
    contenu = _archive(**{"course.fit": b".FIT donnees anonymes"})

    # QUAND / ALORS
    with pytest.raises(CapaciteIndisponible, match="compressé dépasse"):
        AdaptateurGarminLectureSeule._extraire_fit(contenu)


def test_extraire_fit_devrait_refuser_quand_ratio_decompression_est_anormal(monkeypatch):
    # ÉTANT DONNÉ
    monkeypatch.setattr(AdaptateurGarminLectureSeule, "RATIO_MAX_DECOMPRESSION", 2)
    contenu = _archive(**{"course.fit": b".FIT" + b"0" * 1_000})

    # QUAND / ALORS
    with pytest.raises(CapaciteIndisponible, match="taux de compression"):
        AdaptateurGarminLectureSeule._extraire_fit(contenu)


def test_extraire_fit_devrait_lire_par_bloc_quand_archive_valide(monkeypatch):
    # ÉTANT DONNÉ
    monkeypatch.setattr(AdaptateurGarminLectureSeule, "TAILLE_BLOC_LECTURE_OCTETS", 4)
    attendu = b".FIT donnees anonymes en plusieurs blocs"
    contenu = _archive(**{"course.fit": attendu})

    # QUAND
    resultat = AdaptateurGarminLectureSeule._extraire_fit(contenu)

    # ALORS
    assert resultat == attendu
