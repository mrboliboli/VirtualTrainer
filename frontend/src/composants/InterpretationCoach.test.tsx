import { cleanup, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { InterpretationCoach } from './InterpretationCoach';

describe('InterpretationCoach', () => {
  afterEach(() => { cleanup(); vi.unstubAllGlobals(); });

  it('propose de générer une analyse absente', async () => {
    const analyseEnAttente = { id: 'analyse-1', statut: 'EN_ATTENTE', version: 1, erreur: null, resultat: null, dateCreation: '2026-09-07T18:00:00Z', dateMiseAJour: '2026-09-07T18:00:00Z' };
    const fetch = vi.fn().mockResolvedValueOnce(new Response('', { status: 404 })).mockResolvedValueOnce(new Response(JSON.stringify(analyseEnAttente), { status: 202 }));
    vi.stubGlobal('fetch', fetch);
    render(<InterpretationCoach sortieId="sortie 1" />);
    await userEvent.click(await screen.findByRole('button', { name: 'Générer l’interprétation' }));
    expect(fetch).toHaveBeenNthCalledWith(2, '/api/v1/sorties/sortie%201/analyse', expect.objectContaining({ method: 'POST' }));
    expect(await screen.findByText(/Analyse en attente/)).toBeVisible();
  });

  it('sépare interprétation, faits sources, hypothèses et recommandation', async () => {
    const resultat = { resume: 'Une séance régulière.', interpretations: [{ titre: 'Régularité', texte: 'L’effort est resté stable.', confiance: 'MOYENNE', faitsSources: ['regularite-puissance'] }], pointsPositifs: ['Allure stable'], pointsVigilance: [], recuperation: { recommandation: 'Footing léger.', justification: 'Charge modérée.' }, hypotheses: ['Le relief a pu influencer la fréquence cardiaque.'], donneesManquantes: ['Sommeil'], impactProchaineSeance: 'Pas de modification proposée.', avertissementSante: null };
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify({ id: 'analyse-1', statut: 'REUSSIE', version: 1, erreur: null, resultat, dateCreation: '2026-09-07T18:00:00Z', dateMiseAJour: '2026-09-07T18:01:00Z' }), { status: 200 })));
    render(<InterpretationCoach sortieId="sortie-1" />);
    expect(await screen.findByText('Une séance régulière.')).toBeVisible();
    expect(screen.getByText('Confiance moyenne')).toBeVisible();
    expect(screen.getByText(/regularite-puissance/)).toBeVisible();
    expect(screen.getByRole('heading', { name: 'Hypothèses à confirmer' })).toBeVisible();
    expect(screen.getByRole('heading', { name: 'Récupération recommandée' })).toBeVisible();
  });
});
