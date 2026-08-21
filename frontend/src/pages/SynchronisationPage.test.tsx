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
});
