import { useCallback, useEffect, useRef, useState } from 'react';
import { api, ErreurApi } from '../api';
import type { AnalyseSortie, ConfianceCalcul } from '../types';

function Confiance({ niveau }: { niveau: ConfianceCalcul }) {
  const libelle = { ELEVEE: 'élevée', MOYENNE: 'moyenne', FAIBLE: 'faible' }[niveau];
  return <span className={`pastille confiance--${niveau.toLowerCase()}`}>Confiance {libelle}</span>;
}

export function InterpretationCoach({ sortieId }: { sortieId: string }) {
  const [analyse, setAnalyse] = useState<AnalyseSortie>();
  const [absente, setAbsente] = useState(false);
  const [chargement, setChargement] = useState(true);
  const [action, setAction] = useState(false);
  const [erreur, setErreur] = useState('');
  const minuterie = useRef<number>(undefined);
  const charger = useCallback(async () => {
    try {
      const resultat = await api.analyseSortie(sortieId);
      setAnalyse(resultat); setAbsente(false); setErreur('');
      if (resultat.statut === 'EN_ATTENTE' || resultat.statut === 'EN_COURS') minuterie.current = window.setTimeout(charger, 2000);
    } catch (cause) {
      if (cause instanceof ErreurApi && cause.statut === 404) setAbsente(true);
      else setErreur((cause as Error).message);
    } finally { setChargement(false); }
  }, [sortieId]);
  useEffect(() => { charger(); return () => window.clearTimeout(minuterie.current); }, [charger]);

  const lancer = async (regeneration = false) => {
    setAction(true); setErreur(''); setAbsente(false);
    try {
      const resultat = regeneration ? await api.regenererAnalyseSortie(sortieId) : await api.lancerAnalyseSortie(sortieId);
      setAnalyse(resultat);
      if (resultat.statut === 'EN_ATTENTE' || resultat.statut === 'EN_COURS') minuterie.current = window.setTimeout(charger, 1200);
    } catch (cause) { setErreur((cause as Error).message); }
    finally { setAction(false); }
  };

  const statutEnCours = analyse?.statut === 'EN_ATTENTE' || analyse?.statut === 'EN_COURS';
  const erreurAnalyse = analyse?.statut === 'ERREUR_TEMPORAIRE' || analyse?.statut === 'ERREUR_DEFINITIVE';
  const resultat = analyse?.statut === 'REUSSIE' ? analyse.resultat : null;
  return <section className="carte carte--coach" aria-labelledby="titre-coach"><p className="surtitre">Interprétation générée par IA</p><div className="titre-ligne"><div><h2 id="titre-coach">Interprétation du coach</h2><p className="texte-discret">Cette lecture s’appuie sur les faits calculés ci-dessus. Elle ne constitue ni une mesure Garmin ni un diagnostic médical.</p></div>{analyse?.version && <span className="pastille">Version {analyse.version}</span>}</div>
    {chargement && <p role="status"><span className="pulse" aria-hidden="true" />Recherche d’une analyse…</p>}
    {!chargement && absente && <div className="etat-inline"><p>Aucune interprétation n’a encore été générée pour cette sortie.</p><button className="bouton" disabled={action} onClick={() => lancer()}>{action ? 'Lancement…' : 'Générer l’interprétation'}</button></div>}
    {statutEnCours && <p role="status"><span className="pulse" aria-hidden="true" />{analyse?.statut === 'EN_ATTENTE' ? 'Analyse en attente…' : 'Interprétation en cours de génération…'}</p>}
    {(erreur || erreurAnalyse) && <div className="avertissement" role="alert"><strong>L’interprétation n’a pas pu être générée.</strong><p>{erreur || analyse?.erreur || (analyse?.statut === 'ERREUR_TEMPORAIRE' ? 'Le fournisseur rencontre un problème temporaire.' : 'Vérifie la configuration IA dans les réglages.')}</p><button className="bouton bouton--secondaire" disabled={action} onClick={() => lancer()}>{action ? 'Relance…' : 'Réessayer'}</button></div>}
    {resultat && <div className="pile pile--compacte"><p className="resume-coach">{resultat.resume}</p>
      {resultat.interpretations.map((interpretation, index) => <article className="interpretation" key={`${interpretation.titre}-${index}`}><div className="titre-ligne"><h3>{interpretation.titre}</h3><Confiance niveau={interpretation.confiance} /></div><p>{interpretation.texte}</p>{interpretation.faitsSources.length > 0 && <p className="faits-sources"><strong>Faits utilisés :</strong> {interpretation.faitsSources.join(' · ')}</p>}</article>)}
      <div className="grille-coach"><div><h3>Points positifs</h3>{resultat.pointsPositifs.length ? <ul>{resultat.pointsPositifs.map(point => <li key={point}>{point}</li>)}</ul> : <p className="texte-discret">Aucun point identifié.</p>}</div><div><h3>Points de vigilance</h3>{resultat.pointsVigilance.length ? <ul>{resultat.pointsVigilance.map(point => <li key={point}>{point}</li>)}</ul> : <p className="texte-discret">Aucun point identifié.</p>}</div></div>
      <div className="recommandation"><h3>Récupération recommandée</h3><p>{resultat.recuperation.recommandation}</p><p className="texte-discret">{resultat.recuperation.justification}</p></div>
      <div><h3>Impact sur la prochaine séance</h3><p>{resultat.impactProchaineSeance}</p></div>
      {resultat.hypotheses.length > 0 && <div className="hypotheses"><h3>Hypothèses à confirmer</h3><ul>{resultat.hypotheses.map(hypothese => <li key={hypothese}>{hypothese}</li>)}</ul></div>}
      {resultat.donneesManquantes.length > 0 && <div><h3>Données manquantes</h3><ul>{resultat.donneesManquantes.map(donnee => <li key={donnee}>{donnee}</li>)}</ul></div>}
      {resultat.avertissementSante && <p className="avertissement"><strong>Vigilance santé :</strong> {resultat.avertissementSante}</p>}
      <button className="bouton bouton--secondaire" disabled={action} onClick={() => lancer(true)}>{action ? 'Régénération…' : 'Régénérer l’interprétation'}</button>
    </div>}
  </section>;
}
