import { cleanup, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { ReglagesIaPage } from './ReglagesIaPage';

const reglages = { active: true, fournisseur: 'OPENAI', urlBase: 'https://api.openai.com/v1', modeleAnalyse: 'modele-analyse', modelePlanification: null, temperature: 0.3, jetonsMaximum: 1200, instructionsPersonnalisees: null, cleConfiguree: true, statutDernierTest: null, dateDernierTest: null };

describe('ReglagesIaPage', () => {
  afterEach(() => { cleanup(); vi.unstubAllGlobals(); });

  it('indique la présence de la clé sans permettre de la saisir', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify(reglages), { status: 200 })));
    render(<ReglagesIaPage />);
    expect(await screen.findByText('Clé configurée')).toBeVisible();
    expect(screen.queryByLabelText(/clé/i)).not.toBeInTheDocument();
  });

  it('enregistre uniquement les paramètres non sensibles', async () => {
    const fetch = vi.fn().mockResolvedValue(new Response(JSON.stringify(reglages), { status: 200 }));
    vi.stubGlobal('fetch', fetch);
    render(<ReglagesIaPage />);
    expect(await screen.findAllByDisplayValue('modele-analyse')).toHaveLength(2);
    await userEvent.click(screen.getByRole('button', { name: 'Enregistrer les réglages' }));
    const options = fetch.mock.calls[1][1] as RequestInit;
    expect(options.method).toBe('PUT');
    expect(options.body).not.toContain('cleConfiguree');
    expect(options.body).not.toContain('statutDernierTest');
  });
});
