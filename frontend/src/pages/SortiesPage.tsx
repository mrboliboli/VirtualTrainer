import { useEffect, useState } from 'react';
import { api } from '../api';
import { Chargement, Erreur, EtatVide } from '../composants/EtatContenu';
import type { ActiviteRecente } from '../types';

function formaterDuree(secondes: number | null) {
  if (secondes === null) return 'Durée indisponible';
  const heures = Math.floor(secondes / 3600);
  const minutes = Math.floor((secondes % 3600) / 60);
  const reste = secondes % 60;
  return heures > 0 ? `${heures} h ${String(minutes).padStart(2, '0')} min` : `${minutes} min ${String(reste).padStart(2, '0')} s`;
}

export function SortiesPage({ ouvrir }: { ouvrir: (id: string) => void }) {
  const [sorties, setSorties] = useState<ActiviteRecente[]>([]);
  const [chargement, setChargement] = useState(true);
  const [erreur, setErreur] = useState('');
  const charger = () => {
    setChargement(true); setErreur('');
    api.sorties().then(setSorties).catch((e: Error) => setErreur(e.message)).finally(() => setChargement(false));
  };
  useEffect(charger, []);

  if (chargement) return <Chargement libelle="Chargement de tes sorties…" />;
  if (erreur) return <Erreur message={erreur} reessayer={charger} />;
  if (sorties.length === 0) return <EtatVide titre="Aucune sortie synchronisée" texte="Tes activités apparaîtront ici après leur récupération depuis Garmin Connect." />;

  return <div className="pile"><header className="entete-page"><p className="surtitre">Historique</p><h1>Sorties</h1><p>Retrouve les activités récupérées automatiquement depuis Garmin.</p></header><div className="liste-cartes">{sorties.map((sortie) => <article className="carte carte--sortie" key={sortie.id}><div className="titre-ligne"><div><p className="surtitre">{sortie.source === 'GARMIN_PERSONNEL' ? 'Garmin Connect' : sortie.source}</p><h2>{sortie.sport ?? 'Activité'}</h2></div><time dateTime={sortie.dateHeure ?? undefined}>{sortie.dateHeure ? new Intl.DateTimeFormat('fr-FR', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(sortie.dateHeure)) : 'Date indisponible'}</time></div><div className="mesures"><span><b>{sortie.distanceMetres === null ? '—' : (sortie.distanceMetres / 1000).toLocaleString('fr-FR', { maximumFractionDigits: 2 })}</b> km</span><span><b>{formaterDuree(sortie.dureeSecondes)}</b></span></div><button className="lien-action" onClick={() => ouvrir(sortie.id)}>Voir les mesures</button></article>)}</div></div>;
}
