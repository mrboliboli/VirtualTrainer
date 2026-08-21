import { FormEvent, useEffect, useState } from 'react';
import { api } from '../api';
import { Chargement, Erreur, EtatVide } from '../composants/EtatContenu';
import type { Objectif } from '../types';

export function ObjectifsPage() {
  const [objectifs, setObjectifs] = useState<Objectif[]>([]);
  const [chargement, setChargement] = useState(true);
  const [erreur, setErreur] = useState('');
  const [formulaire, setFormulaire] = useState(false);
  const charger = () => { setChargement(true); api.objectifs().then(setObjectifs).catch((e: Error) => setErreur(e.message)).finally(() => setChargement(false)); };
  useEffect(charger, []);
  const creer = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault(); setErreur('');
    const donnees = new FormData(e.currentTarget);
    try {
      await api.creerObjectif({ nom: String(donnees.get('nom')), date: String(donnees.get('date')), distance: donnees.get('distance') ? Number(donnees.get('distance')) : undefined, unite: 'KM', type: String(donnees.get('type')) as Objectif['type'], url: String(donnees.get('url') || '') || undefined, priorite: Number(donnees.get('priorite')), statut: 'PREVU', principal: donnees.get('principal') === 'on' });
      setFormulaire(false); charger();
    } catch (cause) { setErreur((cause as Error).message); }
  };
  return <div className="pile"><header className="entete-page titre-ligne"><div><p className="surtitre">Ta ligne d’horizon</p><h1>Objectifs</h1></div><button className="bouton bouton--compact" onClick={() => setFormulaire(!formulaire)}>{formulaire ? 'Fermer' : 'Ajouter'}</button></header>
    {erreur && <Erreur message={erreur} reessayer={charger} />}
    {formulaire && <form className="carte formulaire grille-champs" onSubmit={creer}><h2>Nouvel objectif</h2><label>Nom <input name="nom" required maxLength={120} /></label><label>Date <input name="date" type="date" required /></label><label>Distance <span className="unite">km</span><input name="distance" type="number" min="0.1" step="0.1" /></label><label>Type <select name="type" defaultValue="RUNNING"><option value="RUNNING">Course à pied</option><option value="TRAIL">Trail</option><option value="ROUTE">Route</option><option value="5_KM">5 km</option><option value="10_KM">10 km</option><option value="SEMI_MARATHON">Semi-marathon</option><option value="MARATHON">Marathon</option><option value="AUTRE">Autre</option></select></label><label>Lien officiel <input name="url" type="url" inputMode="url" /></label><label>Priorité <select name="priorite" defaultValue="2"><option value="1">Faible</option><option value="2">Normale</option><option value="3">Élevée</option></select></label><label className="case"><input name="principal" type="checkbox" /> En faire mon objectif principal</label><button className="bouton" type="submit">Créer l’objectif</button></form>}
    {chargement ? <Chargement /> : objectifs.length === 0 ? <EtatVide titre="Aucun objectif pour le moment" texte="Ajoute une course ou un défi pour donner une direction à ta préparation." action={<button className="bouton" onClick={() => setFormulaire(true)}>Ajouter mon premier objectif</button>} /> : <div className="liste-cartes">{objectifs.map((objectif) => <article className="carte" key={objectif.id}><div className="titre-ligne"><div><p className="surtitre">{objectif.principal ? 'Objectif principal' : 'Objectif'}</p><h2>{objectif.nom}</h2></div><span className="pastille">{objectif.statut.toLocaleLowerCase('fr-FR')}</span></div><p>{new Intl.DateTimeFormat('fr-FR', { dateStyle: 'long' }).format(new Date(`${objectif.date}T12:00:00`))}{objectif.distance ? ` · ${objectif.distance} km` : ''}</p>{objectif.url && <a href={objectif.url} target="_blank" rel="noreferrer">Voir le site officiel <span className="visuellement-cache">de {objectif.nom}</span></a>}<div className="actions"><button className="lien-action" onClick={async () => { await api.archiverObjectif(objectif.id); charger(); }}>Archiver</button><button className="lien-action lien-action--danger" onClick={async () => { if (window.confirm(`Supprimer définitivement l’objectif « ${objectif.nom} » ?`)) { await api.supprimerObjectif(objectif.id); charger(); } }}>Supprimer</button></div></article>)}</div>}
  </div>;
}
