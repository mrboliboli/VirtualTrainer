import { cleanup, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { TableauDeBordPage } from './TableauDeBordPage';

const proposition = {
  id: 'seance-1', titre: 'Endurance progressive', datePrevue: '2026-09-10', type: 'COURSE',
  dureeMinutes: 45, distanceKilometres: 7.5, intensite: 'MODEREE',
  explicationCoach: 'Une séance contrôlée pour construire ton endurance.',
  etapes: ['10 min faciles', '25 min progressives', '10 min de retour au calme'],
  confiance: 'ELEVEE', statut: 'PROPOSEE',
};

function reponseJson(valeur: unknown, statut = 200) {
  return new Response(JSON.stringify(valeur), { status: statut });
}

function donneesInitiales(seance = proposition) {
  return vi.fn().mockImplementation((entree: RequestInfo | URL) => {
    const url = String(entree);
    if (url.endsWith('/profil')) return Promise.resolve(reponseJson({ prenom: 'Fabien', joursDisponibles: [], terrainsAccessibles: [] }));
    if (url.endsWith('/objectifs')) return Promise.resolve(reponseJson([{ id: 'obj-1', nom: '10 km', date: '2026-12-10', unite: 'KM', type: '10_KM', priorite: 1, statut: 'PREVU', principal: true }]));
    if (url.includes('/sorties?')) return Promise.resolve(reponseJson([]));
    if (url.endsWith('/seances/prochaine')) return Promise.resolve(reponseJson(seance));
    return Promise.reject(new Error(`URL inattendue : ${url}`));
  });
}

describe('TableauDeBordPage — prochaine séance', () => {
  afterEach(() => { cleanup(); vi.unstubAllGlobals(); });

  it('présente une proposition détaillée et permet de l’accepter', async () => {
    const fetch = donneesInitiales();
    fetch.mockImplementationOnce(fetch.getMockImplementation()!);
    const initial = fetch.getMockImplementation()!;
    fetch.mockImplementation((entree: RequestInfo | URL) => {
      const url = String(entree);
      if (url.endsWith('/seance-1/acceptation')) return Promise.resolve(reponseJson({ ...proposition, statut: 'PLANIFIEE' }));
      return initial(entree);
    });
    vi.stubGlobal('fetch', fetch);
    render(<TableauDeBordPage naviguer={vi.fn()} />);

    const carte = (await screen.findByText('Endurance progressive')).closest('section')!;
    expect(within(carte).getByText('Proposition')).toBeVisible();
    expect(within(carte).getByText('25 min progressives')).toBeVisible();
    expect(within(carte).getByText(/confiance de la proposition : elevee/i)).toBeVisible();
    expect(within(carte).getByRole('button', { name: 'Refuser' })).toBeEnabled();
    expect(within(carte).getByRole('button', { name: 'Régénérer' })).toBeEnabled();

    await userEvent.click(within(carte).getByRole('button', { name: 'Accepter' }));
    expect(await within(carte).findByText('Planifiée')).toBeVisible();
    expect(within(carte).queryByRole('button', { name: 'Accepter' })).not.toBeInTheDocument();
  });

  it('conserve la séance et affiche clairement une erreur de refus', async () => {
    const initial = donneesInitiales().getMockImplementation()!;
    const fetch = vi.fn().mockImplementation((entree: RequestInfo | URL) => {
      const url = String(entree);
      if (url.endsWith('/seance-1/refus')) return Promise.resolve(reponseJson({ message: 'Le refus n’a pas pu être enregistré.' }, 409));
      return initial(entree);
    });
    vi.stubGlobal('fetch', fetch);
    render(<TableauDeBordPage naviguer={vi.fn()} />);
    await userEvent.click(await screen.findByRole('button', { name: 'Refuser' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Le refus n’a pas pu être enregistré.');
    expect(screen.getByText('Endurance progressive')).toBeVisible();
  });
});
