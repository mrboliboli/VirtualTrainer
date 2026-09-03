import { useEffect, useState } from 'react';
import { api } from '../api';
import { Chargement, Erreur, EtatVide } from '../composants/EtatContenu';
import type { TableauDeBord } from '../types';
import type { Page } from '../composants/Navigation';

function joursRestants(date: string): number {
  const cible = new Date(`${date}T12:00:00`);
  const maintenant = new Date();
  const aujourdHui = new Date(maintenant.getFullYear(), maintenant.getMonth(), maintenant.getDate(), 12);
  return Math.max(0, Math.ceil((cible.getTime() - aujourdHui.getTime()) / 86_400_000));
}

export function TableauDeBordPage({ naviguer }: { naviguer: (page: Page) => void }) {
  const [donnees, setDonnees] = useState<TableauDeBord>();
  const [erreur, setErreur] = useState('');
  const [chargement, setChargement] = useState(true);

  const charger = () => {
    setChargement(true); setErreur('');
    Promise.all([api.profilOuAbsent(), api.objectifs(), api.sorties(3)])
      .then(([profil, objectifs, activitesRecentes]) => setDonnees({ profil, objectifPrincipal: objectifs.find((objectif) => objectif.principal), activitesRecentes, alertes: [] }))
      .catch((e: Error) => setErreur(e.message)).finally(() => setChargement(false));
  };
  useEffect(charger, []);
  if (chargement) return <Chargement libelle="Préparation de ton tableau de bord…" />;
  if (erreur) return <Erreur message={erreur} reessayer={charger} />;
  if (!donnees?.profil) return <EtatVide titre="Bienvenue dans Pace" texte="Commençons par quelques informations pour adapter tes entraînements." action={<button className="bouton" onClick={() => naviguer('profil')}>Créer mon profil</button>} />;

  const objectif = donnees.objectifPrincipal;
  const seance = donnees.prochaineSeance;
  return <div className="pile">
    <header className="entete-page"><p className="surtitre">Ton entraînement</p><h1>Bonjour {donnees.profil.prenom}</h1><p>{donnees.recommandationRecuperation ?? 'Construisons la suite, une sortie après l’autre.'}</p></header>
    {donnees.alertes.map((alerte) => <div className="alerte" role="status" key={alerte}>{alerte}</div>)}
    {objectif ? <section className="carte carte--objectif"><div><p className="surtitre">Objectif principal</p><h2>{objectif.nom}</h2><p>{new Intl.DateTimeFormat('fr-FR', { dateStyle: 'long' }).format(new Date(`${objectif.date}T12:00:00`))}</p></div><strong className="compte-a-rebours"><span>{joursRestants(objectif.date)}</span> jours</strong></section> : <section className="carte"><h2>Choisis ton cap</h2><p>Ajoute ton prochain objectif pour personnaliser ta préparation.</p><button className="lien-action" onClick={() => naviguer('objectifs')}>Ajouter un objectif</button></section>}
    <section className="carte carte--seance">
      <p className="surtitre">Prochaine séance</p>
      {seance ? <><h2>{seance.titre}</h2><p className="date-seance">{new Intl.DateTimeFormat('fr-FR', { weekday: 'long', day: 'numeric', month: 'long' }).format(new Date(`${seance.datePrevue}T12:00:00`))}</p><div className="mesures"><span><b>{seance.dureeMinutes ?? '—'}</b> min</span><span><b>{seance.intensite}</b> intensité</span></div>{seance.explicationCoach && <p>{seance.explicationCoach}</p>}<button className="bouton bouton--large" onClick={() => naviguer('synchroniser')}>Entraînement effectué</button></> : <><h2>Pas encore de séance</h2><p>Une séance te sera proposée après la définition de ton objectif.</p></>}
    </section>
    <section className="carte"><div className="titre-ligne"><h2>Dernières sorties</h2><button className="lien-action" onClick={() => naviguer('sorties')}>Tout voir</button></div>{donnees.activitesRecentes.length === 0 ? <p className="texte-discret">Tes activités Garmin apparaîtront ici après leur synchronisation.</p> : <ul className="liste-simple">{donnees.activitesRecentes.slice(0, 3).map((activite) => <li key={activite.id}><div><strong>{activite.sport ?? 'Activité Garmin'}</strong><small>{activite.dateHeure ? new Intl.DateTimeFormat('fr-FR', { day: 'numeric', month: 'short' }).format(new Date(activite.dateHeure)) : 'Date indisponible'}</small></div><span>{activite.distanceMetres !== null ? `${(activite.distanceMetres / 1000).toLocaleString('fr-FR', { maximumFractionDigits: 1 })} km` : 'Distance indisponible'}</span></li>)}</ul>}</section>
  </div>;
}
