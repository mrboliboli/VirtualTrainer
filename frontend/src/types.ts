export type EtatRequete = 'chargement' | 'pret' | 'vide' | 'erreur';

export interface ProfilAthlete {
  id?: string;
  prenom: string;
  anneeNaissance?: number;
  tailleCentimetres?: number;
  poidsKilogrammes?: number;
  frequenceCardiaqueMaximale?: number;
  frequenceCardiaqueRepos?: number;
  seuilCardiaque?: number;
  seuilPuissance?: number;
  volumeHebdomadaireHabituelKilometres?: number;
  joursDisponibles: string[];
  dureeMaximaleSeanceMinutes?: number;
  terrainsAccessibles: string[];
  contraintesEtBlessures?: string;
  preferencesEntrainement?: string;
  commentaire?: string;
}

export type StatutObjectif = 'PREVU' | 'ACTIF' | 'ATTEINT' | 'ABANDONNE';

export interface Objectif {
  id: string;
  nom: string;
  date: string;
  distance?: number;
  unite: 'KM' | 'MILES';
  type: 'RUNNING' | 'TRAIL' | 'ROUTE' | '5_KM' | '10_KM' | 'SEMI_MARATHON' | 'MARATHON' | 'AUTRE';
  url?: string;
  priorite: number;
  objectifTempsSecondes?: number;
  deniveleCibleMetres?: number;
  notes?: string;
  statut: StatutObjectif;
  principal: boolean;
}

export interface SeancePlanifiee {
  id: string;
  titre: string;
  datePrevue: string;
  type: string;
  dureeMinutes?: number;
  distanceKilometres?: number;
  intensite: string;
  explicationCoach?: string;
  statut: 'PROPOSEE' | 'PLANIFIEE' | 'EFFECTUEE' | 'REPORTEE' | 'ANNULEE' | 'REMPLACEE';
}

export interface ActiviteRecente {
  id: string;
  dateHeure: string;
  sport: string;
  distanceMetres?: number;
  dureeSecondes?: number;
  rapportDisponible: boolean;
}

export interface TableauDeBord {
  profil?: ProfilAthlete;
  objectifPrincipal?: Objectif;
  prochaineSeance?: SeancePlanifiee;
  recommandationRecuperation?: string;
  activitesRecentes: ActiviteRecente[];
  progressionHebdomadaire?: { realiseKilometres: number; prevuKilometres?: number };
  alertes: string[];
}

export type StatutConnexionGarmin = 'DECONNECTE' | 'CONNEXION_EN_COURS' | 'MFA_REQUIS' | 'CONNECTE' | 'EXPIREE' | 'ERREUR';
export interface ConnexionGarmin { statut: StatutConnexionGarmin; mode: 'PERSONNEL'; compte?: { nomAffiche: string }; derniereSynchronisation?: string; derniereActivite?: string; defiMfaId?: string; }
export interface CandidateGarmin { idExterne: string; dateHeure: string; sport: string; distanceMetres?: number; dureeSecondes?: number; confiance: 'ELEVEE' | 'MOYENNE' | 'FAIBLE'; }
export interface SynchronisationGarmin { id: string; statut: 'EN_COURS' | 'TERMINEE' | 'ERREUR_TEMPORAIRE' | 'ERREUR_DEFINITIVE'; messageUtilisateur?: string; candidates?: CandidateGarmin[]; }
