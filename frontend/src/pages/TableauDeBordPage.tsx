import { useEffect, useState } from 'react';
import { api, ErreurApi } from '../api';
import { Chargement, Erreur, EtatVide } from '../composants/EtatContenu';
import type { Objectif, TableauDeBord } from '../types';
import type { Page } from '../composants/Navigation';

function joursRestants(date: string): number {
  const cible = new Date(`${date}T12:00:00`);
  const maintenant = new Date();
  const aujourdHui = new Date(maintenant.getFullYear(), maintenant.getMonth(), maintenant.getDate(), 12);
  return Math.max(0, Math.ceil((cible.getTime() - aujourdHui.getTime()) / 86_400_000));
}

function prochainObjectif(objectifs: Objectif[]) {
  const aujourdHui = new Date();
  const dateLocale = `${aujourdHui.getFullYear()}-${String(aujourdHui.getMonth() + 1).padStart(2, '0')}-${String(aujourdHui.getDate()).padStart(2, '0')}`;
  const futurs = objectifs.filter((objectif) => objectif.date >= dateLocale);
  return futurs.find((objectif) => objectif.principal) ?? futurs[0];
}

function dureeCourte(secondes: number | null) {
  if (secondes === null) return '—';
  const minutes = Math.round(secondes / 60);
  return minutes >= 60 ? `${Math.floor(minutes / 60)} h ${String(minutes % 60).padStart(2, '0')}` : `${minutes} min`;
}

function allureMoyenne(distance: number | null, duree: number | null) {
  if (!distance || !duree) return '—';
  const secondes = Math.round(duree / (distance / 1000));
  return `${Math.floor(secondes / 60)}:${String(secondes % 60).padStart(2, '0')} /km`;
}

export function TableauDeBordPage({ naviguer }: { naviguer: (page: Page) => void }) {
  const [donnees, setDonnees] = useState<TableauDeBord>();
  const [erreur, setErreur] = useState('');
  const [chargement, setChargement] = useState(true);
  const [action, setAction] = useState<'generation' | 'acceptation' | 'refus'>();
  const [erreurAction, setErreurAction] = useState('');

  const charger = () => {
    setChargement(true); setErreur('');
    Promise.all([api.profilOuAbsent(), api.objectifs(), api.sorties(3), api.prochaineSeance().catch(e => e instanceof ErreurApi && e.statut === 404 ? undefined : Promise.reject(e))])
      .then(([profil, objectifs, activitesRecentes, prochaineSeance]) => setDonnees({ profil, objectifPrincipal: prochainObjectif(objectifs), prochaineSeance, activitesRecentes, alertes: [] }))
      .catch((e: Error) => setErreur(e.message)).finally(() => setChargement(false));
  };
  useEffect(charger, []);
  if (chargement) return <Chargement libelle="Préparation de ton tableau de bord…" />;
  if (erreur) return <Erreur message={erreur} reessayer={charger} />;
  if (!donnees?.profil) return <EtatVide titre="Bienvenue dans Pace" texte="Commençons par quelques informations pour adapter tes entraînements." action={<button className="bouton" onClick={() => naviguer('profil')}>Créer mon profil</button>} />;

  const objectif = donnees.objectifPrincipal;
  const seance = donnees.prochaineSeance;
  const generer = async () => {
    setAction('generation'); setErreurAction('');
    try { const prochaineSeance = await api.genererProchaineSeance(); setDonnees({ ...donnees, prochaineSeance }); }
    catch (e) { setErreurAction((e as Error).message); }
    finally { setAction(undefined); }
  };
  const accepter = async () => {
    if (!seance) return;
    setAction('acceptation'); setErreurAction('');
    try { const prochaineSeance = await api.accepterProchaineSeance(seance.id); setDonnees({ ...donnees, prochaineSeance }); }
    catch (e) { setErreurAction((e as Error).message); }
    finally { setAction(undefined); }
  };
  const refuser = async () => {
    if (!seance) return;
    setAction('refus'); setErreurAction('');
    try {
      await api.refuserProchaineSeance(seance.id);
      const prochaineSeance = await api.prochaineSeance().catch(e => e instanceof ErreurApi && e.statut === 404 ? undefined : Promise.reject(e));
      setDonnees({ ...donnees, prochaineSeance });
    }
    catch (e) { setErreurAction((e as Error).message); }
    finally { setAction(undefined); }
  };
  return <div className="pile">
    <header className="entete-page"><p className="surtitre">Ton entraînement</p><h1>Bonjour {donnees.profil.prenom}</h1><p>{donnees.recommandationRecuperation ?? 'Construisons la suite, une sortie après l’autre.'}</p></header>
    {donnees.alertes.map((alerte) => <div className="alerte" role="status" key={alerte}>{alerte}</div>)}
    {objectif ? <section className="carte carte--objectif"><div><p className="surtitre">Objectif principal</p><h2>{objectif.nom}</h2><p>{new Intl.DateTimeFormat('fr-FR', { dateStyle: 'long' }).format(new Date(`${objectif.date}T12:00:00`))}</p></div><strong className="compte-a-rebours"><span>{joursRestants(objectif.date)}</span> jours</strong></section> : <section className="carte"><h2>Choisis ton cap</h2><p>Ajoute ton prochain objectif pour personnaliser ta préparation.</p><button className="lien-action" onClick={() => naviguer('objectifs')}>Ajouter un objectif</button></section>}
    <section className="carte carte--seance">
      <div className="titre-ligne"><p className="surtitre">Prochaine séance</p>{seance && <span className={`pastille ${['ACCEPTEE', 'PLANIFIEE'].includes(seance.statut) ? 'pastille--succes' : 'pastille--attention'}`}>{['ACCEPTEE', 'PLANIFIEE'].includes(seance.statut) ? 'Planifiée' : 'Proposition'}</span>}</div>
      {seance ? <>
        <h2>{seance.titre}</h2>
        <p className="date-seance">{new Intl.DateTimeFormat('fr-FR', { weekday: 'long', day: 'numeric', month: 'long' }).format(new Date(`${seance.datePrevue}T12:00:00`))}</p>
        <div className="mesures"><span><b>{seance.dureeMinutes ?? '—'}</b> min</span>{seance.distanceKilometres != null && <span><b>{seance.distanceKilometres.toLocaleString('fr-FR')}</b> km</span>}<span><b>{seance.intensite}</b> intensité</span></div>
        {seance.explicationCoach && <p>{seance.explicationCoach}</p>}
        {seance.etapes?.length > 0 && <div className="deroule-seance"><h3>Déroulé de la séance</h3><ol>{seance.etapes.map((etape, index) => <li key={`${index}-${etape}`}>{etape}</li>)}</ol></div>}
        {seance.confiance && <p className="texte-discret">Confiance de la proposition : {seance.confiance.toLocaleLowerCase()}.</p>}
        {erreurAction && <div className="erreur-action" role="alert"><p>{erreurAction}</p><button className="lien-action" onClick={() => setErreurAction('')}>Fermer</button></div>}
        <div className="actions actions--seance">
          {seance.statut === 'PROPOSEE' && <button className="bouton" disabled={!!action} onClick={accepter}>{action === 'acceptation' ? 'Acceptation…' : 'Accepter'}</button>}
          {seance.statut === 'PROPOSEE' && <button className="bouton bouton--secondaire bouton--danger" disabled={!!action} onClick={refuser}>{action === 'refus' ? 'Refus…' : 'Refuser'}</button>}
          <button className="bouton bouton--secondaire" disabled={!!action} onClick={generer}>{action === 'generation' ? 'Génération…' : 'Régénérer'}</button>
        </div>
      </> : <>
        <h2>Pas encore de séance</h2><p>Génère une proposition adaptée à ton objectif et à tes dernières sorties.</p>
        {erreurAction && <div className="erreur-action" role="alert"><p>{erreurAction}</p></div>}
        <button className="bouton bouton--large" disabled={!!action || !objectif} onClick={generer}>{action === 'generation' ? 'Génération en cours…' : 'Générer la prochaine séance'}</button>
      </>}
    </section>
    <section className="carte"><div className="titre-ligne"><h2>Dernières sorties</h2><button className="lien-action" onClick={() => naviguer('sorties')}>Tout voir</button></div>{donnees.activitesRecentes.length === 0 ? <p className="texte-discret">Tes activités Garmin apparaîtront ici après leur synchronisation.</p> : <ul className="liste-sorties-accueil">{donnees.activitesRecentes.slice(0, 3).map((activite) => <li key={activite.id}><div className="sortie-accueil__entete"><strong>{activite.typeEntrainement ?? 'Sortie libre'}</strong><time dateTime={activite.dateHeure ?? undefined}>{activite.dateHeure ? new Intl.DateTimeFormat('fr-FR', { day: 'numeric', month: 'short' }).format(new Date(activite.dateHeure)) : 'Date indisponible'}</time></div><dl className="sortie-accueil__mesures"><div><dt>Distance</dt><dd>{activite.distanceMetres !== null ? `${(activite.distanceMetres / 1000).toLocaleString('fr-FR', { maximumFractionDigits: 1 })} km` : '—'}</dd></div><div><dt>Durée</dt><dd>{dureeCourte(activite.dureeSecondes)}</dd></div><div><dt>Allure</dt><dd>{allureMoyenne(activite.distanceMetres, activite.dureeSecondes)}</dd></div><div><dt>FC moy.</dt><dd>{activite.frequenceCardiaqueMoyenne !== null ? `${activite.frequenceCardiaqueMoyenne} bpm` : '—'}</dd></div></dl></li>)}</ul>}</section>
  </div>;
}
