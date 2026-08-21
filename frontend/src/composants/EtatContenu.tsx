import type { ReactNode } from 'react';

export function Chargement({ libelle = 'Chargement en cours…' }: { libelle?: string }) {
  return <div className="etat etat--chargement" role="status"><span className="pulse" aria-hidden="true" />{libelle}</div>;
}

export function Erreur({ message, reessayer }: { message: string; reessayer?: () => void }) {
  return <div className="etat etat--erreur" role="alert"><p>{message}</p>{reessayer && <button className="bouton bouton--secondaire" onClick={reessayer}>Réessayer</button>}</div>;
}

export function EtatVide({ titre, texte, action }: { titre: string; texte: string; action?: ReactNode }) {
  return <div className="etat etat--vide"><h2>{titre}</h2><p>{texte}</p>{action}</div>;
}
