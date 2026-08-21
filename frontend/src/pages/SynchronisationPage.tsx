import { FormEvent, useEffect, useRef, useState } from 'react';
import { api } from '../api';
import { Chargement, Erreur } from '../composants/EtatContenu';
import type { CandidateGarmin, ConnexionGarmin, SynchronisationGarmin } from '../types';

function duree(secondes?: number) {
  if (secondes === undefined) return 'Durée indisponible';
  const heures = Math.floor(secondes / 3600);
  const minutes = Math.floor((secondes % 3600) / 60);
  return heures ? `${heures} h ${String(minutes).padStart(2, '0')} min` : `${minutes} min`;
}

function libelleConfiance(confiance: CandidateGarmin['confiance']) {
  return confiance === 'ELEVEE' ? 'Confiance élevée' : confiance === 'MOYENNE' ? 'Confiance moyenne' : 'Confiance faible';
}

export function SynchronisationPage() {
  const [connexion, setConnexion] = useState<ConnexionGarmin>();
  const [chargement, setChargement] = useState(true);
  const [actionEnCours, setActionEnCours] = useState(false);
  const [erreur, setErreur] = useState('');
  const [consentement, setConsentement] = useState(false);
  const [defiMfaId, setDefiMfaId] = useState('');
  const [synchronisation, setSynchronisation] = useState<SynchronisationGarmin>();
  const [confirmation, setConfirmation] = useState('');
  const minuterie = useRef<number>(undefined);

  const chargerConnexion = () => {
    setChargement(true); setErreur('');
    api.connexionGarmin().then(setConnexion).catch((e: Error) => setErreur(e.message)).finally(() => setChargement(false));
  };
  useEffect(() => { chargerConnexion(); return () => window.clearTimeout(minuterie.current); }, []);

  const connecter = async (evenement: FormEvent<HTMLFormElement>) => {
    evenement.preventDefault(); setActionEnCours(true); setErreur('');
    const donnees = new FormData(evenement.currentTarget);
    try {
      const resultat = await api.connecterGarmin(String(donnees.get('identifiant')), String(donnees.get('motDePasse')));
      setConnexion(resultat); setDefiMfaId(resultat.defiMfaId ?? '');
    } catch (e) { setErreur((e as Error).message); }
    finally { setActionEnCours(false); }
  };

  const confirmerMfa = async (evenement: FormEvent<HTMLFormElement>) => {
    evenement.preventDefault(); setActionEnCours(true); setErreur('');
    const code = String(new FormData(evenement.currentTarget).get('code'));
    try { setConnexion(await api.confirmerMfaGarmin(defiMfaId, code)); }
    catch (e) { setErreur((e as Error).message); }
    finally { setActionEnCours(false); }
  };

  const suivre = async (id: string) => {
    try {
      const resultat = await api.synchronisationGarmin(id); setSynchronisation(resultat);
      if (resultat.statut === 'EN_COURS') minuterie.current = window.setTimeout(() => suivre(id), 1500);
      if (resultat.statut.startsWith('ERREUR')) setErreur(resultat.messageUtilisateur ?? 'La recherche Garmin n’a pas abouti. Réessaie dans un instant.');
    } catch (e) { setErreur((e as Error).message); }
  };

  const synchroniser = async () => {
    setActionEnCours(true); setErreur(''); setConfirmation('');
    try { const resultat = await api.lancerSynchronisationGarmin(); setSynchronisation(resultat); if (resultat.statut === 'EN_COURS') await suivre(resultat.id); }
    catch (e) { setErreur((e as Error).message); }
    finally { setActionEnCours(false); }
  };

  const confirmerCandidate = async (candidate: CandidateGarmin) => {
    if (!synchronisation) return;
    setActionEnCours(true); setErreur('');
    try { await api.confirmerCandidateGarmin(synchronisation.id, candidate.idExterne); setConfirmation('Activité confirmée. Pace récupère maintenant ses détails.'); }
    catch (e) { setErreur((e as Error).message); }
    finally { setActionEnCours(false); }
  };

  if (chargement) return <Chargement libelle="Vérification de la connexion Garmin…" />;
  if (!connexion && erreur) return <Erreur message={erreur} reessayer={chargerConnexion} />;
  const mfaRequis = connexion?.statut === 'MFA_REQUIS';
  const connecte = connexion?.statut === 'CONNECTE';

  return <div className="pile"><header className="entete-page"><p className="surtitre">Garmin Connect</p><h1>Retrouver mon entraînement</h1><p>Pace récupère automatiquement tes activités. Aucun fichier à importer.</p></header>
    {erreur && <Erreur message={erreur} />}{confirmation && <p className="confirmation" role="status">{confirmation}</p>}
    {!connecte && !mfaRequis && <><aside className="avertissement" aria-labelledby="titre-avertissement"><h2 id="titre-avertissement">Connexion personnelle expérimentale</h2><p>Ce mode n’est pas une intégration Garmin officielle. Il peut cesser de fonctionner et son utilisation peut entraîner une limitation ou une suspension de ton compte Garmin.</p><p>Ton mot de passe sert uniquement à ouvrir la session et n’est pas conservé par Pace.</p></aside><form className="carte formulaire" onSubmit={connecter}><h2>Se connecter à Garmin</h2><label>Adresse électronique Garmin <input name="identifiant" type="email" autoComplete="username" required /></label><label>Mot de passe Garmin <input name="motDePasse" type="password" autoComplete="current-password" required /></label><label className="case case--consentement"><input type="checkbox" checked={consentement} onChange={(e) => setConsentement(e.target.checked)} required /><span>Je comprends le caractère non officiel, sa fragilité et le risque de suspension de compte.</span></label><button className="bouton bouton--large" type="submit" disabled={!consentement || actionEnCours}>{actionEnCours ? 'Connexion en cours…' : 'Accepter et se connecter'}</button></form></>}
    {mfaRequis && <form className="carte formulaire" onSubmit={confirmerMfa}><p className="surtitre">Vérification en deux étapes</p><h2>Saisis le code reçu</h2><p>Garmin demande une vérification supplémentaire. Le code n’est utilisé que pour terminer cette connexion.</p><label>Code de vérification <input name="code" inputMode="numeric" autoComplete="one-time-code" pattern="[0-9 ]+" required autoFocus /></label><button className="bouton bouton--large" disabled={actionEnCours}>{actionEnCours ? 'Vérification…' : 'Vérifier le code'}</button></form>}
    {connecte && <><section className="carte"><div className="titre-ligne"><div><p className="surtitre">Connexion active</p><h2>{connexion.compte?.nomAffiche ?? 'Compte Garmin connecté'}</h2></div><span className="pastille pastille--succes">Connecté</span></div>{connexion.derniereSynchronisation && <p className="texte-discret">Dernière synchronisation : {new Intl.DateTimeFormat('fr-FR', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(connexion.derniereSynchronisation))}</p>}<button className="bouton bouton--large" onClick={synchroniser} disabled={actionEnCours || synchronisation?.statut === 'EN_COURS'}>{synchronisation?.statut === 'EN_COURS' ? 'Recherche en cours…' : 'Rechercher ma dernière activité'}</button></section>
      {synchronisation?.statut === 'EN_COURS' && <Chargement libelle="Recherche de ta dernière activité Garmin…" />}
      {synchronisation?.statut === 'TERMINEE' && (synchronisation.candidates?.length ?? 0) === 0 && <section className="etat etat--vide"><h2>Aucune activité récente trouvée</h2><p>Ta course n’est peut-être pas encore arrivée dans Garmin Connect. Vérifie la synchronisation de ta montre, puis réessaie.</p><button className="bouton bouton--secondaire" onClick={synchroniser}>Réessayer</button></section>}
      {(synchronisation?.candidates?.length ?? 0) > 0 && <section aria-labelledby="titre-candidates"><h2 id="titre-candidates">Est-ce bien ton activité&nbsp;?</h2><div className="liste-cartes">{synchronisation!.candidates!.map((candidate) => <article className="carte carte--candidate" key={candidate.idExterne}><div className="titre-ligne"><div><strong>{candidate.sport}</strong><p>{new Intl.DateTimeFormat('fr-FR', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(candidate.dateHeure))}</p></div><span className={`pastille confiance--${candidate.confiance.toLowerCase()}`}>{libelleConfiance(candidate.confiance)}</span></div><div className="mesures"><span><b>{candidate.distanceMetres === undefined ? '—' : (candidate.distanceMetres / 1000).toLocaleString('fr-FR', { maximumFractionDigits: 2 })}</b> km</span><span><b>{duree(candidate.dureeSecondes)}</b></span></div><button className="bouton bouton--large" onClick={() => confirmerCandidate(candidate)} disabled={actionEnCours}>Confirmer cette activité</button></article>)}</div></section>}
      <button className="lien-action lien-action--danger" onClick={async () => { if (window.confirm('Déconnecter Garmin de Pace ? Les activités déjà récupérées seront conservées.')) { await api.deconnecterGarmin(); setConnexion({ statut: 'DECONNECTE', mode: 'PERSONNEL' }); setSynchronisation(undefined); } }}>Déconnecter Garmin</button></>}
  </div>;
}
