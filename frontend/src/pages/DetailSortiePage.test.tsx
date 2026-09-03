import { cleanup, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { DetailSortiePage } from './DetailSortiePage';

const base = {
  id: 'd8a2b65a-49eb-4ffd-9220-e13cbc35ab07', dateHeure: '2026-08-21T08:30:00Z', sport: 'Course à pied', sousSport: null, source: 'GARMIN_PERSONNEL', erreurDecodage: null,
  distanceMetres: null, dureeEcouleeSecondes: null, dureeActiveSecondes: null, vitesseMoyenneMetresParSeconde: null, vitesseMaximaleMetresParSeconde: null,
  frequenceCardiaqueMoyenne: null, frequenceCardiaqueMaximale: null, cadenceMoyenne: null, cadenceMaximale: null, puissanceMoyenneWatts: null, puissanceMaximaleWatts: null, puissanceNormaliseeWatts: null,
  calories: null, denivelePositifMetres: null, deniveleNegatifMetres: null, effetEntrainementAerobie: null, effetEntrainementAnaerobie: null, chargeEntrainement: null, tours: [], zones: [], serie: [],
};

describe('DetailSortiePage', () => {
  afterEach(() => { cleanup(); vi.unstubAllGlobals(); });

  it('affiche explicitement les mesures absentes sans les inventer', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify({ ...base, etatDecodage: 'DECODEE' }), { status: 200 })));
    render(<DetailSortiePage id={base.id} revenir={vi.fn()} />);
    expect(await screen.findByRole('heading', { name: 'Résumé mesuré' })).toBeVisible();
    expect(screen.getAllByText('Indisponible').length).toBeGreaterThan(5);
    expect(screen.getByText('Aucun tour n’est présent dans le fichier FIT.')).toBeVisible();
  });

  it('présente une erreur de décodage sans afficher de fausses mesures', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify({ ...base, etatDecodage: 'ERREUR', erreurDecodage: 'Le fichier FIT est incomplet.' }), { status: 200 })));
    render(<DetailSortiePage id={base.id} revenir={vi.fn()} />);
    expect(await screen.findByRole('alert')).toHaveTextContent('Le fichier FIT est incomplet.');
    expect(screen.queryByRole('heading', { name: 'Résumé mesuré' })).not.toBeInTheDocument();
  });

  it('annonce le décodage en cours sans afficher prématurément les mesures', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify({ ...base, etatDecodage: 'EN_COURS' }), { status: 200 })));
    render(<DetailSortiePage id={base.id} revenir={vi.fn()} />);
    expect(await screen.findByText(/Décodage du fichier FIT en cours/)).toBeVisible();
    expect(screen.queryByRole('heading', { name: 'Résumé mesuré' })).not.toBeInTheDocument();
  });

  it('n’invente pas une unité de cadence absente du contrat', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify({ ...base, etatDecodage: 'DECODEE', cadenceMoyenne: 172, cadenceMaximale: 184 }), { status: 200 })));
    render(<DetailSortiePage id={base.id} revenir={vi.fn()} />);
    expect(await screen.findByText('172')).toBeVisible();
    expect(screen.queryByText(/pas\/min/)).not.toBeInTheDocument();
  });

  it('distingue les relevés affichés du total lorsque la série est tronquée', async () => {
    const serie = [{ dateHeure: '2026-08-21T08:30:00Z', frequenceCardiaque: 150, puissanceWatts: null, cadence: null, altitudeMetres: null }];
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify({ ...base, etatDecodage: 'DECODEE', serie, totalEchantillons: 12500, serieTronquee: true }), { status: 200 })));
    render(<DetailSortiePage id={base.id} revenir={vi.fn()} />);
    expect(await screen.findByText(/1 relevé affiché sur 12.500 relevés enregistrés/)).toBeVisible();
    expect(screen.getByText(/série affichée est limitée/)).toBeVisible();
  });
});
