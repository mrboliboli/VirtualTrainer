import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { SynchronisationPage } from './SynchronisationPage';

describe('SynchronisationPage', () => {
  afterEach(() => vi.unstubAllGlobals());

  it('exige un consentement avant la connexion personnelle', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify({ statut: 'DECONNECTE', mode: 'PERSONNEL' }), { status: 200 })));
    render(<SynchronisationPage />);
    const bouton = await screen.findByRole('button', { name: 'Accepter et se connecter' });
    expect(bouton).toBeDisabled();
    await userEvent.click(screen.getByRole('checkbox'));
    expect(bouton).toBeEnabled();
    expect(screen.getByText(/suspension de ton compte Garmin/i)).toBeVisible();
  });

  it('présente la vérification en deux étapes lorsqu’elle est requise', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify({ statut: 'MFA_REQUIS', mode: 'PERSONNEL', defiMfaId: 'defi-1' }), { status: 200 })));
    render(<SynchronisationPage />);
    expect(await screen.findByRole('heading', { name: 'Saisis le code reçu' })).toBeVisible();
    expect(screen.getByLabelText('Code de vérification')).toHaveAttribute('autocomplete', 'one-time-code');
  });

  it('affiche une erreur terminale renvoyée directement par le POST 202', async () => {
    const fetch = vi.fn()
      .mockResolvedValueOnce(new Response(JSON.stringify({ statut: 'CONNECTE', mode: 'PERSONNEL' }), { status: 200 }))
      .mockResolvedValueOnce(new Response(JSON.stringify({
        id: 'ff2ccfe1-4efb-40e8-848f-b68e6eb48335',
        statut: 'ERREUR_DEFINITIVE',
        messageUtilisateur: 'La date de l’activité Garmin est invalide.',
        candidates: [],
      }), { status: 202 }));
    vi.stubGlobal('fetch', fetch);
    render(<SynchronisationPage />);
    await userEvent.click(await screen.findByRole('button', { name: 'Rechercher de nouvelles activités' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('La date de l’activité Garmin est invalide.');
    expect(fetch).toHaveBeenCalledTimes(2);
  });

  it('récupère l’identifiant du POST, interroge le suivi puis affiche les candidates', async () => {
    const id = '434d32eb-733b-493e-8f6c-253b4487057b';
    const fetch = vi.fn()
      .mockResolvedValueOnce(new Response(JSON.stringify({ statut: 'CONNECTE', mode: 'PERSONNEL' }), { status: 200 }))
      .mockResolvedValueOnce(new Response(JSON.stringify({ id, statut: 'EN_COURS', candidates: [] }), { status: 202 }))
      .mockResolvedValueOnce(new Response(JSON.stringify({ id, statut: 'TERMINEE', candidates: [{ idExterne: 'course-42', dateHeure: '2026-08-21T08:30:00Z', sport: 'Course à pied', distanceMetres: 10869, dureeSecondes: 4221, confiance: 'ELEVEE' }] }), { status: 200 }));
    vi.stubGlobal('fetch', fetch);
    render(<SynchronisationPage />);
    await userEvent.click(await screen.findByRole('button', { name: 'Rechercher de nouvelles activités' }));
    expect(await screen.findByText('Course à pied')).toBeVisible();
    expect(screen.getByText('Confiance élevée')).toBeVisible();
    expect(fetch).toHaveBeenNthCalledWith(3, `/api/v1/garmin/synchronisations/${id}`, expect.any(Object));
  });
});
