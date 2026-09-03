import { render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { SortiesPage } from './SortiesPage';

describe('SortiesPage', () => {
  afterEach(() => vi.unstubAllGlobals());

  it('affiche un état vide utile lorsque le serveur ne renvoie aucune sortie', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('[]', { status: 200 })));
    render(<SortiesPage ouvrir={vi.fn()} />);
    expect(await screen.findByRole('heading', { name: 'Aucune sortie synchronisée' })).toBeVisible();
  });

  it('affiche les données réelles et signale les mesures absentes', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify([
      { id: '1', dateHeure: '2026-08-21T08:30:00Z', sport: 'Course à pied', distanceMetres: 10869, dureeSecondes: 4221, source: 'GARMIN_PERSONNEL' },
      { id: '2', dateHeure: null, sport: null, distanceMetres: null, dureeSecondes: null, source: 'GARMIN_PERSONNEL' },
    ]), { status: 200 })));
    render(<SortiesPage ouvrir={vi.fn()} />);
    expect(await screen.findByRole('heading', { name: 'Course à pied' })).toBeVisible();
    expect(screen.getByText('10,87')).toBeVisible();
    expect(screen.getByText('Date indisponible')).toBeVisible();
    expect(screen.getByText('Durée indisponible')).toBeVisible();
  });

  it('affiche une erreur compréhensible lorsque le serveur échoue', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('', { status: 503 })));
    render(<SortiesPage ouvrir={vi.fn()} />);
    expect(await screen.findByRole('alert')).toHaveTextContent('problème temporaire');
  });
});
