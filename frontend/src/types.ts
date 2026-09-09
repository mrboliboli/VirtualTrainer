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
  etapes: string[];
  confiance?: ConfianceCalcul;
  statut: 'PROPOSEE' | 'ACCEPTEE' | 'PLANIFIEE' | 'EFFECTUEE' | 'REFUSEE' | 'REPORTEE' | 'ANNULEE' | 'REMPLACEE';
  version?: number;
  versionPrecedenteId?: string;
}

export interface RealisationSeance {
  seanceId: string;
  titre: string;
  datePrevue: string;
  dureePrevueMinutes: number | null;
  statut: 'REALISEE';
  activiteId: string;
  dateActivite: string | null;
  sport: string | null;
  distanceMetres: number | null;
  dureeSecondes: number | null;
  methodeRapprochement: string;
  rapprocheLe: string;
}

export interface ActiviteRecente {
  id: string;
  dateHeure: string | null;
  sport: string | null;
  distanceMetres: number | null;
  dureeSecondes: number | null;
  frequenceCardiaqueMoyenne: number | null;
  typeEntrainement: string | null;
  source: string;
}

export type ConfianceCalcul = 'ELEVEE' | 'MOYENNE' | 'FAIBLE';
export interface EvolutionFactuelle {
  premiereMoitie: number;
  secondeMoitie: number;
  evolutionPourcent: number | null;
  confiance: ConfianceCalcul;
}

export interface DetailSortie {
  id: string;
  dateHeure: string | null;
  sport: string | null;
  source: string;
  sousSport: string | null;
  etatDecodage: 'A_DECODER' | 'EN_COURS' | 'DECODEE' | 'ERREUR';
  erreurDecodage: string | null;
  distanceMetres: number | null;
  dureeEcouleeSecondes: number | null;
  dureeActiveSecondes: number | null;
  vitesseMoyenneMetresParSeconde: number | null;
  vitesseMaximaleMetresParSeconde: number | null;
  frequenceCardiaqueMoyenne: number | null;
  frequenceCardiaqueMaximale: number | null;
  cadenceMoyenne: number | null;
  cadenceMaximale: number | null;
  puissanceMoyenneWatts: number | null;
  puissanceMaximaleWatts: number | null;
  puissanceNormaliseeWatts: number | null;
  calories: number | null;
  denivelePositifMetres: number | null;
  deniveleNegatifMetres: number | null;
  effetEntrainementAerobie: number | null;
  effetEntrainementAnaerobie: number | null;
  chargeEntrainement: number | null;
  ressenti: { rpeSurDix: number | null; scoreGarminSurCent: number | null; source: string | null };
  compteRenduFactuel: {
    allureMoyenneSecondesParKilometre: number | null;
    frequenceCardiaque: EvolutionFactuelle | null;
    puissance: EvolutionFactuelle | null;
    cadence: EvolutionFactuelle | null;
    regularitePuissanceCoefficientVariationPourcent: number | null;
    confianceRegularitePuissance: ConfianceCalcul | null;
    repartitionZones: Array<{ type: string; index: number; dureeSecondes: number | null; pourcentage: number | null }>;
    donneesAbsentes: string[];
  } | null;
  totalEchantillons?: number;
  serieTronquee?: boolean;
  tours: Array<{ index: number; dateHeure: string | null; distanceMetres: number | null; dureeEcouleeSecondes: number | null; dureeActiveSecondes: number | null; frequenceCardiaqueMoyenne: number | null; frequenceCardiaqueMaximale: number | null; puissanceMoyenneWatts: number | null }>;
  zones: Array<{ type: string; index: number; borneBasse: number | null; borneHaute: number | null; dureeSecondes: number | null }>;
  serie: Array<{ dateHeure: string | null; frequenceCardiaque: number | null; puissanceWatts: number | null; cadence: number | null; altitudeMetres: number | null }>;
}

export interface TableauDeBord {
  profil?: ProfilAthlete;
  objectifPrincipal?: Objectif;
  prochaineSeance?: SeancePlanifiee;
  derniereRealisation?: RealisationSeance;
  recommandationRecuperation?: string;
  activitesRecentes: ActiviteRecente[];
  progressionHebdomadaire?: { realiseKilometres: number; prevuKilometres?: number };
  alertes: string[];
}

export type StatutConnexionGarmin = 'DECONNECTE' | 'CONNEXION_EN_COURS' | 'MFA_REQUIS' | 'CONNECTE' | 'EXPIREE' | 'ERREUR';
export interface ConnexionGarmin { statut: StatutConnexionGarmin; mode: 'PERSONNEL'; compte?: { nomAffiche: string }; derniereSynchronisation?: string; derniereActivite?: string; defiMfaId?: string; }
export interface CandidateGarmin { idExterne: string; dateHeure: string; sport: string; distanceMetres?: number; dureeSecondes?: number; confiance: 'ELEVEE' | 'MOYENNE' | 'FAIBLE'; }
export interface SynchronisationGarmin { id: string; statut: 'EN_COURS' | 'TERMINEE' | 'ERREUR_TEMPORAIRE' | 'ERREUR_DEFINITIVE'; messageUtilisateur?: string; candidates?: CandidateGarmin[]; dateDebutRecherche?: string; rattrapageTermine?: boolean; }

export interface ReglagesIa {
  fournisseur: 'OPENAI';
  urlBase: string;
  modeleAnalyse: string;
  modelePlanification: string | null;
  temperature: number;
  jetonsMaximum: number;
  instructionsPersonnalisees: string | null;
  active: boolean;
  cleConfiguree: boolean;
  statutDernierTest: 'REUSSI' | 'ECHEC' | null;
  dateDernierTest: string | null;
  generationSeanceApresImport: boolean;
}

export type StatutAnalyse = 'EN_ATTENTE' | 'EN_COURS' | 'REUSSIE' | 'ERREUR_TEMPORAIRE' | 'ERREUR_DEFINITIVE';
export interface InterpretationCoach {
  titre: string;
  texte: string;
  confiance: ConfianceCalcul;
  faitsSources: string[];
}
export interface AnalyseSortie {
  id: string;
  statut: StatutAnalyse;
  version: number;
  erreur: string | null;
  resultat: ResultatAnalyse | null;
  dateCreation: string;
  dateMiseAJour: string;
}
export interface ResultatAnalyse {
  resume: string;
  interpretations: InterpretationCoach[];
  pointsPositifs: string[];
  pointsVigilance: string[];
  recuperation: { recommandation: string; justification: string };
  hypotheses: string[];
  donneesManquantes: string[];
  impactProchaineSeance: string;
  avertissementSante: string | null;
}
