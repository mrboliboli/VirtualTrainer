import { FormEvent, useEffect, useState } from 'react';
import { api } from '../api';
import { Chargement, Erreur } from '../composants/EtatContenu';
import type { ProfilAthlete } from '../types';

const VIDE: ProfilAthlete = { prenom: '', joursDisponibles: [], terrainsAccessibles: [] };
const JOURS = ['Lundi', 'Mardi', 'Mercredi', 'Jeudi', 'Vendredi', 'Samedi', 'Dimanche'];
const TERRAINS = ['Route', 'Chemin', 'Piste', 'Sentier vallonné', 'Tapis'];

export function ProfilPage() {
  const [profil, setProfil] = useState(VIDE);
  const [chargement, setChargement] = useState(true);
  const [erreur, setErreur] = useState('');
  const [confirmation, setConfirmation] = useState('');
  useEffect(() => { api.profil().then(setProfil).catch((e) => { if (e.statut !== 404) setErreur(e.message); }).finally(() => setChargement(false)); }, []);
  if (chargement) return <Chargement libelle="Chargement de ton profil…" />;

  const nombre = (champ: keyof ProfilAthlete, valeur: string) => setProfil({ ...profil, [champ]: valeur ? Number(valeur) : undefined });
  const basculer = (champ: 'joursDisponibles' | 'terrainsAccessibles', valeur: string) => setProfil({ ...profil, [champ]: profil[champ].includes(valeur) ? profil[champ].filter((x) => x !== valeur) : [...profil[champ], valeur] });
  const soumettre = async (evenement: FormEvent) => { evenement.preventDefault(); setErreur(''); setConfirmation(''); try { setProfil(await api.enregistrerProfil(profil)); setConfirmation('Ton profil est enregistré.'); } catch (e) { setErreur((e as Error).message); } };
  return <div className="pile"><header className="entete-page"><p className="surtitre">Tes repères</p><h1>Profil</h1><p>Seules les informations utiles à ton accompagnement sont demandées.</p></header>{erreur && <Erreur message={erreur} />} {confirmation && <p className="confirmation" role="status">{confirmation}</p>}
    <form className="formulaire" onSubmit={soumettre}>
      <section className="carte grille-champs"><h2>À propos de toi</h2><label>Prénom <input required value={profil.prenom} onChange={(e) => setProfil({ ...profil, prenom: e.target.value })} autoComplete="given-name" /></label><label>Année de naissance <input type="number" min="1900" max={new Date().getFullYear()} value={profil.anneeNaissance ?? ''} onChange={(e) => nombre('anneeNaissance', e.target.value)} /></label><label>Taille <span className="unite">cm</span><input type="number" min="100" max="250" value={profil.tailleCentimetres ?? ''} onChange={(e) => nombre('tailleCentimetres', e.target.value)} /></label><label>Poids <span className="unite">kg</span><input type="number" min="30" max="300" step="0.1" value={profil.poidsKilogrammes ?? ''} onChange={(e) => nombre('poidsKilogrammes', e.target.value)} /></label></section>
      <section className="carte grille-champs"><h2>Repères physiologiques</h2><p className="aide">Laisse un champ vide si tu ne connais pas la valeur. Pace signalera les recommandations concernées.</p><label>Fréquence cardiaque maximale <span className="unite">bpm</span><input type="number" min="100" max="240" value={profil.frequenceCardiaqueMaximale ?? ''} onChange={(e) => nombre('frequenceCardiaqueMaximale', e.target.value)} /></label><label>Fréquence cardiaque au repos <span className="unite">bpm</span><input type="number" min="25" max="150" value={profil.frequenceCardiaqueRepos ?? ''} onChange={(e) => nombre('frequenceCardiaqueRepos', e.target.value)} /></label><label>Seuil cardiaque <span className="unite">bpm</span><input type="number" min="80" max="230" value={profil.seuilCardiaque ?? ''} onChange={(e) => nombre('seuilCardiaque', e.target.value)} /></label><label>Seuil de puissance <span className="unite">W</span><input type="number" min="50" max="1000" value={profil.seuilPuissance ?? ''} onChange={(e) => nombre('seuilPuissance', e.target.value)} /></label></section>
      <section className="carte"><h2>Ton quotidien</h2><fieldset><legend>Jours disponibles</legend><div className="choix-puces">{JOURS.map((jour) => <label key={jour}><input type="checkbox" checked={profil.joursDisponibles.includes(jour)} onChange={() => basculer('joursDisponibles', jour)} /><span>{jour.slice(0, 3)}</span></label>)}</div></fieldset><fieldset><legend>Terrains accessibles</legend><div className="choix-puces">{TERRAINS.map((terrain) => <label key={terrain}><input type="checkbox" checked={profil.terrainsAccessibles.includes(terrain)} onChange={() => basculer('terrainsAccessibles', terrain)} /><span>{terrain}</span></label>)}</div></fieldset><div className="grille-champs"><label>Volume hebdomadaire habituel <span className="unite">km</span><input type="number" min="0" max="500" value={profil.volumeHebdomadaireHabituelKilometres ?? ''} onChange={(e) => nombre('volumeHebdomadaireHabituelKilometres', e.target.value)} /></label><label>Durée maximale par séance <span className="unite">min</span><input type="number" min="10" max="600" value={profil.dureeMaximaleSeanceMinutes ?? ''} onChange={(e) => nombre('dureeMaximaleSeanceMinutes', e.target.value)} /></label></div></section>
      <section className="carte grille-champs"><h2>Contexte</h2><label>Contraintes ou blessures <textarea value={profil.contraintesEtBlessures ?? ''} onChange={(e) => setProfil({ ...profil, contraintesEtBlessures: e.target.value })} /></label><p className="aide">Pace ne formule aucun diagnostic médical. En cas de douleur, consulte un professionnel de santé.</p><label>Préférences d’entraînement <textarea value={profil.preferencesEntrainement ?? ''} onChange={(e) => setProfil({ ...profil, preferencesEntrainement: e.target.value })} /></label><label>Commentaire libre <textarea value={profil.commentaire ?? ''} onChange={(e) => setProfil({ ...profil, commentaire: e.target.value })} /></label></section>
      <button className="bouton bouton--large" type="submit">Enregistrer mon profil</button>
    </form>
  </div>;
}
