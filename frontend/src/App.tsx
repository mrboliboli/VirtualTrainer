import { useState } from 'react';
import { Navigation, type Page } from './composants/Navigation';
import { ObjectifsPage } from './pages/ObjectifsPage';
import { ProfilPage } from './pages/ProfilPage';
import { SynchronisationPage } from './pages/SynchronisationPage';
import { TableauDeBordPage } from './pages/TableauDeBordPage';
import { EtatVide } from './composants/EtatContenu';

export function App() {
  const [page, setPage] = useState<Page>('accueil');
  const contenu = page === 'accueil' ? <TableauDeBordPage naviguer={setPage} />
    : page === 'profil' ? <ProfilPage />
    : page === 'objectifs' ? <ObjectifsPage />
    : page === 'synchroniser' ? <SynchronisationPage />
    : page === 'sorties' ? <EtatVide titre="Aucune sortie synchronisée" texte="Tes sorties apparaîtront ici après leur récupération depuis Garmin Connect." />
    : <EtatVide titre="Réglages bientôt disponibles" texte="La configuration dépendra du mode de connexion Garmin retenu." />;
  return <div className="application"><a className="evitement" href="#contenu">Aller au contenu</a><main id="contenu" className="contenu">{contenu}</main><Navigation page={page} naviguer={setPage} /></div>;
}
