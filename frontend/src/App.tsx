import { useState } from 'react';
import { Navigation, type Page } from './composants/Navigation';
import { ObjectifsPage } from './pages/ObjectifsPage';
import { ProfilPage } from './pages/ProfilPage';
import { SynchronisationPage } from './pages/SynchronisationPage';
import { TableauDeBordPage } from './pages/TableauDeBordPage';
import { EtatVide } from './composants/EtatContenu';
import { SortiesPage } from './pages/SortiesPage';
import { DetailSortiePage } from './pages/DetailSortiePage';
import { ReglagesIaPage } from './pages/ReglagesIaPage';

export function App() {
  const [page, setPage] = useState<Page>('accueil');
  const [sortieSelectionnee, setSortieSelectionnee] = useState<string>();
  const contenu = page === 'accueil' ? <TableauDeBordPage naviguer={setPage} />
    : page === 'profil' ? <ProfilPage />
    : page === 'objectifs' ? <ObjectifsPage />
    : page === 'synchroniser' ? <SynchronisationPage />
    : page === 'sorties' ? sortieSelectionnee ? <DetailSortiePage id={sortieSelectionnee} revenir={() => setSortieSelectionnee(undefined)} /> : <SortiesPage ouvrir={setSortieSelectionnee} />
    : page === 'reglages' ? <ReglagesIaPage />
    : <EtatVide titre="Page indisponible" texte="Cette page n’est pas encore disponible." />;
  return <div className="application"><a className="evitement" href="#contenu">Aller au contenu</a><main id="contenu" className="contenu">{contenu}</main><Navigation page={page} naviguer={setPage} /></div>;
}
