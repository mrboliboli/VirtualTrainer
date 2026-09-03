import { useEffect, useRef, useState } from 'react';
import { api } from '../api';
import { Chargement, Erreur } from '../composants/EtatContenu';
import type { DetailSortie } from '../types';

function valeur(nombre: number | null, unite: string, chiffres = 0) {
  return nombre === null ? 'Indisponible' : `${nombre.toLocaleString('fr-FR', { maximumFractionDigits: chiffres })}\u00a0${unite}`;
}

function duree(secondes: number | null) {
  if (secondes === null) return 'Indisponible';
  const heures = Math.floor(secondes / 3600);
  const minutes = Math.floor((secondes % 3600) / 60);
  const reste = Math.round(secondes % 60);
  return heures ? `${heures} h ${String(minutes).padStart(2, '0')} min ${String(reste).padStart(2, '0')} s` : `${minutes} min ${String(reste).padStart(2, '0')} s`;
}

function allure(vitesse: number | null) {
  if (vitesse === null || vitesse <= 0) return 'Indisponible';
  const secondes = Math.round(1000 / vitesse);
  return `${Math.floor(secondes / 60)}:${String(secondes % 60).padStart(2, '0')} min/km`;
}

function nombreReleves(nombre: number) {
  return `${nombre.toLocaleString('fr-FR')} relevé${nombre > 1 ? 's' : ''}`;
}

function Mesure({ libelle, contenu }: { libelle: string; contenu: string }) {
  const absente = contenu === 'Indisponible';
  return <div className={absente ? 'mesure-factuelle mesure-factuelle--absente' : 'mesure-factuelle'}><dt>{libelle}</dt><dd>{contenu}</dd></div>;
}

export function DetailSortiePage({ id, revenir }: { id: string; revenir: () => void }) {
  const [sortie, setSortie] = useState<DetailSortie>();
  const [chargement, setChargement] = useState(true);
  const [erreur, setErreur] = useState('');
  const minuterie = useRef<number>(undefined);
  const charger = () => {
    setErreur('');
    api.detailSortie(id).then((resultat) => {
      setSortie(resultat); setChargement(false);
      if (resultat.etatDecodage === 'A_DECODER' || resultat.etatDecodage === 'EN_COURS') minuterie.current = window.setTimeout(charger, 2000);
    }).catch((e: Error) => { setErreur(e.message); setChargement(false); });
  };
  useEffect(() => { charger(); return () => window.clearTimeout(minuterie.current); }, [id]);

  if (chargement) return <Chargement libelle="Chargement des mesures de la sortie…" />;
  if (erreur && !sortie) return <Erreur message={erreur} reessayer={charger} />;
  if (!sortie) return null;
  const enCours = sortie.etatDecodage === 'A_DECODER' || sortie.etatDecodage === 'EN_COURS';
  const resumeSerie = sortie.serie.length === 0
    ? 'Aucun relevé temporel n’est présent dans le fichier FIT.'
    : sortie.serieTronquee && sortie.totalEchantillons !== undefined
      ? `${nombreReleves(sortie.serie.length)} affiché${sortie.serie.length > 1 ? 's' : ''} sur ${nombreReleves(sortie.totalEchantillons)} enregistré${sortie.totalEchantillons > 1 ? 's' : ''}.`
      : sortie.totalEchantillons !== undefined
        ? `${nombreReleves(sortie.totalEchantillons)} enregistré${sortie.totalEchantillons > 1 ? 's' : ''}.`
        : `${nombreReleves(sortie.serie.length)} inclus dans cette réponse.`;

  return <div className="pile"><button className="lien-action" onClick={revenir}>← Retour aux sorties</button><header className="entete-page"><p className="surtitre">Faits mesurés</p><h1>{sortie.sport ?? 'Sortie'}</h1><p>{sortie.dateHeure ? new Intl.DateTimeFormat('fr-FR', { dateStyle: 'long', timeStyle: 'short' }).format(new Date(sortie.dateHeure)) : 'Date indisponible'}{sortie.sousSport ? ` · ${sortie.sousSport}` : ''}</p></header>
    {enCours && <Chargement libelle={sortie.etatDecodage === 'A_DECODER' ? 'Décodage en attente…' : 'Décodage du fichier FIT en cours…'} />}
    {sortie.etatDecodage === 'ERREUR' && <Erreur message={sortie.erreurDecodage ?? 'Le fichier FIT n’a pas pu être décodé. Les données originales restent conservées.'} reessayer={charger} />}
    {sortie.etatDecodage === 'DECODEE' && <><section className="carte"><h2>Résumé mesuré</h2><dl className="grille-mesures"><Mesure libelle="Distance" contenu={valeur(sortie.distanceMetres === null ? null : sortie.distanceMetres / 1000, 'km', 2)} /><Mesure libelle="Durée écoulée" contenu={duree(sortie.dureeEcouleeSecondes)} /><Mesure libelle="Durée active" contenu={duree(sortie.dureeActiveSecondes)} /><Mesure libelle="Allure moyenne" contenu={allure(sortie.vitesseMoyenneMetresParSeconde)} /><Mesure libelle="Vitesse maximale" contenu={valeur(sortie.vitesseMaximaleMetresParSeconde === null ? null : sortie.vitesseMaximaleMetresParSeconde * 3.6, 'km/h', 1)} /><Mesure libelle="Calories" contenu={valeur(sortie.calories, 'kcal')} /></dl></section>
      <section className="carte"><h2>Fréquence cardiaque, cadence et puissance</h2><dl className="grille-mesures"><Mesure libelle="Fréquence cardiaque moyenne" contenu={valeur(sortie.frequenceCardiaqueMoyenne, 'bpm')} /><Mesure libelle="Fréquence cardiaque maximale" contenu={valeur(sortie.frequenceCardiaqueMaximale, 'bpm')} /><Mesure libelle="Cadence moyenne" contenu={valeur(sortie.cadenceMoyenne, '').trim()} /><Mesure libelle="Cadence maximale" contenu={valeur(sortie.cadenceMaximale, '').trim()} /><Mesure libelle="Puissance moyenne" contenu={valeur(sortie.puissanceMoyenneWatts, 'W')} /><Mesure libelle="Puissance maximale" contenu={valeur(sortie.puissanceMaximaleWatts, 'W')} /><Mesure libelle="Puissance normalisée" contenu={valeur(sortie.puissanceNormaliseeWatts, 'W')} /></dl></section>
      <section className="carte"><h2>Relief et effet d’entraînement Garmin</h2><dl className="grille-mesures"><Mesure libelle="Dénivelé positif" contenu={valeur(sortie.denivelePositifMetres, 'm')} /><Mesure libelle="Dénivelé négatif" contenu={valeur(sortie.deniveleNegatifMetres, 'm')} /><Mesure libelle="Effet aérobie" contenu={valeur(sortie.effetEntrainementAerobie, '', 1).trim()} /><Mesure libelle="Effet anaérobie" contenu={valeur(sortie.effetEntrainementAnaerobie, '', 1).trim()} /><Mesure libelle="Charge d’entraînement" contenu={valeur(sortie.chargeEntrainement, '', 1).trim()} /></dl></section>
      <section className="carte"><h2>Tours</h2>{sortie.tours.length === 0 ? <p className="texte-discret">Aucun tour n’est présent dans le fichier FIT.</p> : <div className="tableau-defilant"><table><thead><tr><th>Tour</th><th>Distance</th><th>Durée</th><th>FC moyenne</th><th>Puissance</th></tr></thead><tbody>{sortie.tours.map((tour) => <tr key={tour.index}><th>{tour.index}</th><td>{valeur(tour.distanceMetres === null ? null : tour.distanceMetres / 1000, 'km', 2)}</td><td>{duree(tour.dureeActiveSecondes)}</td><td>{valeur(tour.frequenceCardiaqueMoyenne, 'bpm')}</td><td>{valeur(tour.puissanceMoyenneWatts, 'W')}</td></tr>)}</tbody></table></div>}</section>
      <section className="carte"><h2>Zones enregistrées</h2>{sortie.zones.length === 0 ? <p className="texte-discret">Aucune zone n’est présente dans le fichier FIT.</p> : <ul className="liste-simple">{sortie.zones.map((zone) => <li key={`${zone.type}-${zone.index}`}><strong>{zone.type} · zone {zone.index}</strong><span>{duree(zone.dureeSecondes)}</span></li>)}</ul>}</section>
      <section className="carte"><h2>Série temporelle</h2><p>{resumeSerie}</p>{sortie.serieTronquee && <p className="alerte">La série affichée est limitée pour préserver les performances de l’application.</p>}<p className="texte-discret">Aucun graphique n’est généré tant qu’une restitution accessible et fidèle de ces relevés n’est pas disponible.</p></section></>}
  </div>;
}
