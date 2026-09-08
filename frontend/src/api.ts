import type { ActiviteRecente, AnalyseSortie, ConnexionGarmin, DetailSortie, Objectif, ProfilAthlete, ReglagesIa, SeancePlanifiee, SynchronisationGarmin } from './types';

const BASE_API = import.meta.env.VITE_API_URL ?? '/api/v1';

export class ErreurApi extends Error {
  constructor(
    message: string,
    readonly statut?: number,
  ) {
    super(message);
    this.name = 'ErreurApi';
  }
}

async function requete<T>(chemin: string, options?: RequestInit): Promise<T> {
  let reponse: Response;
  try {
    reponse = await fetch(`${BASE_API}${chemin}`, {
      ...options,
      headers: { 'Content-Type': 'application/json', 'X-Requete-Pace': 'interface-web', ...options?.headers },
    });
  } catch {
    throw new ErreurApi("L’application ne parvient pas à joindre le serveur. Vérifie qu’il est démarré.");
  }
  if (!reponse.ok) {
    const corps = await reponse.json().catch(() => undefined) as { message?: string } | undefined;
    throw new ErreurApi(corps?.message || messagePourStatut(reponse.status), reponse.status);
  }
  if (reponse.status === 204) return undefined as T;
  return reponse.json() as Promise<T>;
}

function messagePourStatut(statut: number): string {
  if (statut === 400) return 'Certaines informations sont incorrectes. Vérifie les champs indiqués.';
  if (statut === 404) return 'Les informations demandées sont introuvables.';
  if (statut >= 500) return 'Le serveur rencontre un problème temporaire. Réessaie dans un instant.';
  return "L’action n’a pas pu aboutir. Réessaie dans un instant.";
}

export const api = {
  profil: () => requete<ProfilAthlete>('/profil'),
  profilOuAbsent: async () => {
    try { return await requete<ProfilAthlete>('/profil'); }
    catch (cause) { if (cause instanceof ErreurApi && cause.statut === 404) return undefined; throw cause; }
  },
  enregistrerProfil: (profil: ProfilAthlete) =>
    requete<ProfilAthlete>('/profil', { method: 'PUT', body: JSON.stringify(profil) }),
  objectifs: () => requete<Objectif[]>('/objectifs'),
  creerObjectif: (objectif: Omit<Objectif, 'id'>) =>
    requete<Objectif>('/objectifs', { method: 'POST', body: JSON.stringify(objectif) }),
  archiverObjectif: (id: string) => requete<void>(`/objectifs/${encodeURIComponent(id)}/archivage`, { method: 'POST' }),
  supprimerObjectif: (id: string) => requete<void>(`/objectifs/${encodeURIComponent(id)}`, { method: 'DELETE' }),
  sorties: (limite = 20) => requete<ActiviteRecente[]>(`/sorties?limite=${Math.min(100, Math.max(1, limite))}`),
  detailSortie: (id: string) => requete<DetailSortie>(`/sorties/${encodeURIComponent(id)}`),
  connexionGarmin: () => requete<ConnexionGarmin>('/garmin/connexion'),
  connecterGarmin: (identifiant: string, motDePasse: string) => requete<ConnexionGarmin>('/garmin/connexion', { method: 'POST', body: JSON.stringify({ identifiant, motDePasse, consentementRisques: true }) }),
  confirmerMfaGarmin: (defiMfaId: string, code: string) => requete<ConnexionGarmin>('/garmin/connexion/mfa', { method: 'POST', body: JSON.stringify({ defiMfaId, code }) }),
  deconnecterGarmin: () => requete<void>('/garmin/connexion', { method: 'DELETE' }),
  lancerSynchronisationGarmin: () => requete<SynchronisationGarmin>('/garmin/synchronisations', { method: 'POST' }),
  synchronisationGarmin: (id: string) => requete<SynchronisationGarmin>(`/garmin/synchronisations/${encodeURIComponent(id)}`),
  confirmerCandidateGarmin: (synchronisationId: string, idExterne: string) => requete<void>(`/garmin/synchronisations/${encodeURIComponent(synchronisationId)}/confirmation`, { method: 'POST', body: JSON.stringify({ idExterne }) }),
  reglagesIa: () => requete<ReglagesIa>('/reglages/ia'),
  enregistrerReglagesIa: (reglages: Omit<ReglagesIa, 'cleConfiguree' | 'statutDernierTest' | 'dateDernierTest'>) => requete<ReglagesIa>('/reglages/ia', { method: 'PUT', body: JSON.stringify(reglages) }),
  testerReglagesIa: () => requete<ReglagesIa>('/reglages/ia/test', { method: 'POST' }),
  analyseSortie: (id: string) => requete<AnalyseSortie>(`/sorties/${encodeURIComponent(id)}/analyse`),
  lancerAnalyseSortie: (id: string) => requete<AnalyseSortie>(`/sorties/${encodeURIComponent(id)}/analyse`, { method: 'POST' }),
  regenererAnalyseSortie: (id: string) => requete<AnalyseSortie>(`/sorties/${encodeURIComponent(id)}/analyse/regeneration`, { method: 'POST' }),
  prochaineSeance: () => requete<SeancePlanifiee>('/seances/prochaine'),
  genererProchaineSeance: () => requete<SeancePlanifiee>('/seances/prochaine/generation', { method: 'POST' }),
};
