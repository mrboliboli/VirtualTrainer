import { useEffect, useRef, useState } from 'react';
import { api } from '../api';
import { Chargement, Erreur } from '../composants/EtatContenu';
import type { DetailSortie } from '../types';

function valeur(nombre: number | null, unite: string, chiffres = 0) {
  return nombre === null ? 'Indisponible' : `${nombre.toLocaleString('fr-FR', { maximumFractionDigits: chiffres })}\u00a0${unite}`;
}
function duree(secondes: number | null) {
  if (secondes === null) return 'Indisponible';
  const heures = Math.floor(secondes / 3600); const minutes = Math.floor((secondes % 3600) / 60); const reste = Math.round(secondes % 60);
  return heures ? `${heures} h ${String(minutes).padStart(2, '0')} min ${String(reste).padStart(2, '0')} s` : `${minutes} min ${String(reste).padStart(2, '0')} s`;
}
function allureSecondes(secondes: number | null) {
  if (secondes === null || secondes <= 0) return 'Indisponible';
  const arrondi = Math.round(secondes); return `${Math.floor(arrondi / 60)}:${String(arrondi % 60).padStart(2, '0')} min/km`;
}
function allure(vitesse: number | null) { return vitesse === null || vitesse <= 0 ? 'Indisponible' : allureSecondes(1000 / vitesse); }
function nombreReleves(nombre: number) { return `${nombre.toLocaleString('fr-FR')} relevé${nombre > 1 ? 's' : ''}`; }
function Mesure({ libelle, contenu }: { libelle: string; contenu: string }) {
  return <div className={contenu === 'Indisponible' ? 'mesure-factuelle mesure-factuelle--absente' : 'mesure-factuelle'}><dt>{libelle}</dt><dd>{contenu}</dd></div>;
}
function Confiance({ niveau }: { niveau: 'ELEVEE' | 'MOYENNE' | 'FAIBLE' | null }) {
  if (!niveau) return null;
  const libelle = { ELEVEE: 'élevée', MOYENNE: 'moyenne', FAIBLE: 'faible' }[niveau];
  return <span className={`pastille confiance--${niveau.toLowerCase()}`}>Confiance {libelle}</span>;
}
function Evolution({ libelle, unite, evolution }: { libelle: string; unite: string; evolution: NonNullable<DetailSortie['compteRenduFactuel']>['frequenceCardiaque'] }) {
  if (!evolution) return null;
  const variation = evolution.evolutionPourcent === null ? 'Variation indisponible' : `${evolution.evolutionPourcent >= 0 ? '+' : ''}${evolution.evolutionPourcent.toLocaleString('fr-FR', { maximumFractionDigits: 1 })} %`;
  return <article className="calcul-factuel"><div className="titre-ligne"><h3>{libelle}</h3><Confiance niveau={evolution.confiance} /></div><p><strong>{variation}</strong> entre les deux moitiés</p><p className="texte-discret">Première moitié : {valeur(evolution.premiereMoitie, unite, 1)} · seconde moitié : {valeur(evolution.secondeMoitie, unite, 1)}</p></article>;
}

export function DetailSortiePage({ id, revenir }: { id: string; revenir: () => void }) {
  const [sortie, setSortie] = useState<DetailSortie>(); const [chargement, setChargement] = useState(true); const [erreur, setErreur] = useState(''); const minuterie = useRef<number>(undefined);
  const charger = () => { setErreur(''); api.detailSortie(id).then((resultat) => { setSortie(resultat); setChargement(false); if (resultat.etatDecodage === 'A_DECODER' || resultat.etatDecodage === 'EN_COURS') minuterie.current = window.setTimeout(charger, 2000); }).catch((e: Error) => { setErreur(e.message); setChargement(false); }); };
  useEffect(() => { charger(); return () => window.clearTimeout(minuterie.current); }, [id]);
  if (chargement) return <Chargement libelle="Chargement des mesures de la sortie…" />;
  if (erreur && !sortie) return <Erreur message={erreur} reessayer={charger} />;
  if (!sortie) return null;
  const enCours = sortie.etatDecodage === 'A_DECODER' || sortie.etatDecodage === 'EN_COURS';
  const resumeSerie = sortie.serie.length === 0 ? 'Aucun relevé temporel n’est présent dans le fichier FIT.' : sortie.serieTronquee && sortie.totalEchantillons !== undefined ? `${nombreReleves(sortie.serie.length)} affiché${sortie.serie.length > 1 ? 's' : ''} sur ${nombreReleves(sortie.totalEchantillons)} enregistré${sortie.totalEchantillons > 1 ? 's' : ''}.` : sortie.totalEchantillons !== undefined ? `${nombreReleves(sortie.totalEchantillons)} enregistré${sortie.totalEchantillons > 1 ? 's' : ''}.` : `${nombreReleves(sortie.serie.length)} inclus dans cette réponse.`;

  return <div className="pile"><button className="lien-action" onClick={revenir}>← Retour aux sorties</button><header className="entete-page"><p className="surtitre">Faits mesurés</p><h1>{sortie.sport ?? 'Sortie'}</h1><p>{sortie.dateHeure ? new Intl.DateTimeFormat('fr-FR', { dateStyle: 'long', timeStyle: 'short' }).format(new Date(sortie.dateHeure)) : 'Date indisponible'}{sortie.sousSport ? ` · ${sortie.sousSport}` : ''}</p></header>
    {enCours && <Chargement libelle={sortie.etatDecodage === 'A_DECODER' ? 'Décodage en attente…' : 'Décodage du fichier FIT en cours…'} />}
    {sortie.etatDecodage === 'ERREUR' && <Erreur message={sortie.erreurDecodage ?? 'Le fichier FIT n’a pas pu être décodé. Les données originales restent conservées.'} reessayer={charger} />}
    {sortie.etatDecodage === 'DECODEE' && <>
      <section className="carte"><h2>Résumé mesuré</h2><dl className="grille-mesures"><Mesure libelle="Distance" contenu={valeur(sortie.distanceMetres === null ? null : sortie.distanceMetres / 1000, 'km', 2)} /><Mesure libelle="Durée écoulée" contenu={duree(sortie.dureeEcouleeSecondes)} /><Mesure libelle="Durée active" contenu={duree(sortie.dureeActiveSecondes)} /><Mesure libelle="Allure moyenne" contenu={allure(sortie.vitesseMoyenneMetresParSeconde)} /><Mesure libelle="Vitesse maximale" contenu={valeur(sortie.vitesseMaximaleMetresParSeconde === null ? null : sortie.vitesseMaximaleMetresParSeconde * 3.6, 'km/h', 1)} /><Mesure libelle="Calories" contenu={valeur(sortie.calories, 'kcal')} /></dl></section>
      <section className="carte"><p className="surtitre">Donnée Garmin</p><h2>Ressenti enregistré</h2>{sortie.ressenti.source === 'GARMIN' ? <dl className="grille-mesures"><Mesure libelle="Effort perçu (RPE)" contenu={valeur(sortie.ressenti.rpeSurDix, '/ 10', 1)} /><Mesure libelle="Score de ressenti Garmin" contenu={valeur(sortie.ressenti.scoreGarminSurCent, '/ 100')} /></dl> : <p className="texte-discret">Aucun ressenti n’a été fourni par Garmin. Le compte rendu reste disponible sans saisie supplémentaire.</p>}</section>
      <section className="carte"><h2>Fréquence cardiaque, cadence et puissance</h2><dl className="grille-mesures"><Mesure libelle="Fréquence cardiaque moyenne" contenu={valeur(sortie.frequenceCardiaqueMoyenne, 'bpm')} /><Mesure libelle="Fréquence cardiaque maximale" contenu={valeur(sortie.frequenceCardiaqueMaximale, 'bpm')} /><Mesure libelle="Cadence moyenne" contenu={valeur(sortie.cadenceMoyenne, '').trim()} /><Mesure libelle="Cadence maximale" contenu={valeur(sortie.cadenceMaximale, '').trim()} /><Mesure libelle="Puissance moyenne" contenu={valeur(sortie.puissanceMoyenneWatts, 'W')} /><Mesure libelle="Puissance maximale" contenu={valeur(sortie.puissanceMaximaleWatts, 'W')} /><Mesure libelle="Puissance normalisée" contenu={valeur(sortie.puissanceNormaliseeWatts, 'W')} /></dl></section>
      <section className="carte"><h2>Relief et effet d’entraînement Garmin</h2><dl className="grille-mesures"><Mesure libelle="Dénivelé positif" contenu={valeur(sortie.denivelePositifMetres, 'm')} /><Mesure libelle="Dénivelé négatif" contenu={valeur(sortie.deniveleNegatifMetres, 'm')} /><Mesure libelle="Effet aérobie" contenu={valeur(sortie.effetEntrainementAerobie, '', 1).trim()} /><Mesure libelle="Effet anaérobie" contenu={valeur(sortie.effetEntrainementAnaerobie, '', 1).trim()} /><Mesure libelle="Charge d’entraînement" contenu={valeur(sortie.chargeEntrainement, '', 1).trim()} /></dl></section>
      <section className="carte"><h2>Tours</h2>{sortie.tours.length === 0 ? <p className="texte-discret">Aucun tour n’est présent dans le fichier FIT.</p> : <div className="tableau-defilant"><table><thead><tr><th>Tour</th><th>Distance</th><th>Durée</th><th>FC moyenne</th><th>Puissance</th></tr></thead><tbody>{sortie.tours.map((tour) => <tr key={tour.index}><th>{tour.index}</th><td>{valeur(tour.distanceMetres === null ? null : tour.distanceMetres / 1000, 'km', 2)}</td><td>{duree(tour.dureeActiveSecondes)}</td><td>{valeur(tour.frequenceCardiaqueMoyenne, 'bpm')}</td><td>{valeur(tour.puissanceMoyenneWatts, 'W')}</td></tr>)}</tbody></table></div>}</section>
      <section className="carte"><h2>Zones enregistrées</h2>{sortie.zones.length === 0 ? <p className="texte-discret">Aucune zone n’est présente dans le fichier FIT.</p> : <ul className="liste-simple">{sortie.zones.map((zone) => <li key={`${zone.type}-${zone.index}`}><strong>{zone.type} · zone {zone.index}</strong><span>{duree(zone.dureeSecondes)}</span></li>)}</ul>}</section>
      {sortie.compteRenduFactuel && <section className="carte"><p className="surtitre">Valeurs calculées localement</p><h2>Compte rendu factuel</h2><p className="texte-discret">Ces constats sont calculés par Pace à partir des mesures disponibles. Ils ne constituent pas une interprétation de coaching.</p><dl className="grille-mesures"><Mesure libelle="Allure calculée" contenu={allureSecondes(sortie.compteRenduFactuel.allureMoyenneSecondesParKilometre)} /><Mesure libelle="Dispersion de puissance" contenu={valeur(sortie.compteRenduFactuel.regularitePuissanceCoefficientVariationPourcent, '%', 1)} /></dl><Confiance niveau={sortie.compteRenduFactuel.confianceRegularitePuissance} /><div className="pile pile--compacte"><Evolution libelle="Fréquence cardiaque" unite="bpm" evolution={sortie.compteRenduFactuel.frequenceCardiaque} /><Evolution libelle="Puissance" unite="W" evolution={sortie.compteRenduFactuel.puissance} /><Evolution libelle="Cadence" unite="" evolution={sortie.compteRenduFactuel.cadence} /></div>{sortie.compteRenduFactuel.repartitionZones.length > 0 && <div><h3>Répartition des zones</h3><ul className="liste-simple">{sortie.compteRenduFactuel.repartitionZones.map(zone => <li key={`${zone.type}-${zone.index}`}><strong>{zone.type} · zone {zone.index}</strong><span>{zone.pourcentage === null ? 'Indisponible' : `${zone.pourcentage.toLocaleString('fr-FR', { maximumFractionDigits: 1 })} %`}</span></li>)}</ul></div>}{sortie.compteRenduFactuel.donneesAbsentes.length > 0 && <div className="alerte"><strong>Limites des données</strong><ul>{sortie.compteRenduFactuel.donneesAbsentes.map(message => <li key={message}>{message}</li>)}</ul></div>}</section>}
      <section className="carte"><h2>Série temporelle</h2><p>{resumeSerie}</p>{sortie.serieTronquee && <p className="alerte">La série affichée est limitée pour préserver les performances de l’application.</p>}<p className="texte-discret">Aucun graphique n’est généré tant qu’une restitution accessible et fidèle de ces relevés n’est disponible.</p></section>
    </>}
  </div>;
}
