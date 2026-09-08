import { FormEvent, useEffect, useState } from 'react';
import { api } from '../api';
import { Chargement, Erreur } from '../composants/EtatContenu';
import type { ReglagesIa } from '../types';

export function ReglagesIaPage() {
  const [reglages, setReglages] = useState<ReglagesIa>();
  const [chargement, setChargement] = useState(true);
  const [action, setAction] = useState<'enregistrement' | 'test'>();
  const [erreur, setErreur] = useState('');
  const [confirmation, setConfirmation] = useState('');
  const charger = () => { setChargement(true); setErreur(''); api.reglagesIa().then(setReglages).catch((e: Error) => setErreur(e.message)).finally(() => setChargement(false)); };
  useEffect(charger, []);
  if (chargement) return <Chargement libelle="Chargement des réglages IA…" />;
  if (!reglages) return <Erreur message={erreur} reessayer={charger} />;

  const enregistrer = async (evenement: FormEvent) => {
    evenement.preventDefault(); setAction('enregistrement'); setErreur(''); setConfirmation('');
    try { const { cleConfiguree: _cle, statutDernierTest: _statut, dateDernierTest: _date, ...modifiables } = reglages; setReglages(await api.enregistrerReglagesIa(modifiables)); setConfirmation('Les réglages IA sont enregistrés.'); }
    catch (e) { setErreur((e as Error).message); }
    finally { setAction(undefined); }
  };
  const tester = async () => {
    setAction('test'); setErreur(''); setConfirmation('');
    try { setReglages(await api.testerReglagesIa()); setConfirmation('La connexion au fournisseur IA fonctionne.'); }
    catch (e) { setErreur((e as Error).message); }
    finally { setAction(undefined); }
  };
  const dateTest = reglages.dateDernierTest ? new Intl.DateTimeFormat('fr-FR', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(reglages.dateDernierTest)) : null;

  return <div className="pile reglages-ia"><header className="entete-page"><p className="surtitre">Configuration locale</p><h1>Réglages IA</h1><p>Choisis comment le coach interprète tes sorties. La clé reste dans l’environnement du serveur et n’est jamais affichée ici.</p></header>
    {erreur && <Erreur message={erreur} />}{confirmation && <p className="confirmation" role="status">{confirmation}</p>}
    <section className="carte"><div className="titre-ligne"><div><h2>Clé d’accès</h2><p className="texte-discret">À configurer avec la variable <code>PACE_AI_API_KEY</code> sur le serveur.</p></div><span className={`pastille ${reglages.cleConfiguree ? 'pastille--succes' : 'pastille--attention'}`}>{reglages.cleConfiguree ? 'Clé configurée' : 'Clé absente'}</span></div></section>
    <form className="formulaire" onSubmit={enregistrer}>
      <section className="carte grille-champs"><h2>Fournisseur et modèles</h2><label>Fournisseur<select value={reglages.fournisseur} disabled><option value="OPENAI">OpenAI</option></select></label><label>URL de l’API<input required type="url" value={reglages.urlBase} onChange={e => setReglages({ ...reglages, urlBase: e.target.value })} /></label><label>Modèle d’analyse<input value={reglages.modeleAnalyse} disabled /><span className="aide">Verrouillé par le serveur pour maîtriser le coût.</span></label><label>Modèle de planification<input value={reglages.modelePlanification ?? reglages.modeleAnalyse} disabled /></label></section>
      <section className="carte grille-champs"><h2>Comportement du coach</h2><label>Température <span className="unite">0 à 2</span><input type="number" min="0" max="2" step="0.1" value={reglages.temperature} onChange={e => setReglages({ ...reglages, temperature: Number(e.target.value) })} /></label><label>Longueur maximale <span className="unite">jetons</span><input type="number" min="100" max="10000" step="100" value={reglages.jetonsMaximum} onChange={e => setReglages({ ...reglages, jetonsMaximum: Number(e.target.value) })} /></label><label className="champ-large">Instructions personnalisées<textarea maxLength={4000} value={reglages.instructionsPersonnalisees ?? ''} onChange={e => setReglages({ ...reglages, instructionsPersonnalisees: e.target.value || null })} /><span className="aide">Ces instructions orientent le ton du coach, sans remplacer les règles de sécurité.</span></label><label className="case champ-large"><input type="checkbox" checked={reglages.active} onChange={e => setReglages({ ...reglages, active: e.target.checked })} /> Activer les analyses IA</label><label className="case champ-large"><input type="checkbox" checked={reglages.generationSeanceApresImport} onChange={e => setReglages({ ...reglages, generationSeanceApresImport: e.target.checked })} /> Générer automatiquement la prochaine séance après l’import d’une course<span className="aide">Désactivé par défaut. Sans cette option, la génération reste manuelle depuis l’accueil.</span></label></section>
      <button className="bouton bouton--large" disabled={Boolean(action)}>{action === 'enregistrement' ? 'Enregistrement…' : 'Enregistrer les réglages'}</button>
    </form>
    <section className="carte"><h2>Tester la configuration</h2><p className="texte-discret">Ce test minimal ne transmet aucune donnée sportive.</p>{reglages.statutDernierTest && <p className={reglages.statutDernierTest === 'REUSSI' ? 'confirmation' : 'avertissement'}><strong>{reglages.statutDernierTest === 'REUSSI' ? 'Dernier test réussi' : 'Dernier test en erreur'}</strong>{dateTest ? ` · ${dateTest}` : ''}</p>}<button type="button" className="bouton bouton--secondaire" disabled={Boolean(action) || !reglages.cleConfiguree} onClick={tester}>{action === 'test' ? 'Test en cours…' : 'Tester la connexion'}</button></section>
  </div>;
}
