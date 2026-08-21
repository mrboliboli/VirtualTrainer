export type Page = 'accueil' | 'sorties' | 'synchroniser' | 'objectifs' | 'reglages' | 'profil';

const ENTREES: Array<{ page: Page; libelle: string; symbole: string }> = [
  { page: 'accueil', libelle: 'Accueil', symbole: '⌂' },
  { page: 'sorties', libelle: 'Sorties', symbole: '↗' },
  { page: 'synchroniser', libelle: 'Synchroniser', symbole: '↻' },
  { page: 'objectifs', libelle: 'Objectifs', symbole: '◎' },
  { page: 'reglages', libelle: 'Réglages', symbole: '⚙' },
];

export function Navigation({ page, naviguer }: { page: Page; naviguer: (page: Page) => void }) {
  return (
    <nav className="navigation" aria-label="Navigation principale">
      {ENTREES.map((entree) => (
        <button
          type="button"
          key={entree.page}
          className={entree.page === 'synchroniser' ? 'navigation__element navigation__element--central' : 'navigation__element'}
          aria-current={page === entree.page ? 'page' : undefined}
          aria-label={entree.libelle}
          onClick={() => naviguer(entree.page)}
        >
          <span aria-hidden="true">{entree.symbole}</span>
          <small>{entree.libelle}</small>
        </button>
      ))}
    </nav>
  );
}
