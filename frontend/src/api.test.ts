import { afterEach, describe, expect, it, vi } from 'vitest';
import { api, ErreurApi } from './api';

describe('client API', () => {
  afterEach(() => vi.unstubAllGlobals());
  it('traduit une indisponibilité du serveur en message compréhensible', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('network')));
    await expect(api.profil()).rejects.toEqual(expect.objectContaining<Partial<ErreurApi>>({ message: expect.stringContaining('serveur') }));
  });
  it('envoie le profil au format JSON', async () => {
    const fetch = vi.fn().mockResolvedValue(new Response(JSON.stringify({ prenom: 'Ana', joursDisponibles: [], terrainsAccessibles: [] }), { status: 200 }));
    vi.stubGlobal('fetch', fetch);
    await api.enregistrerProfil({ prenom: 'Ana', joursDisponibles: [], terrainsAccessibles: [] });
    expect(fetch).toHaveBeenCalledWith('/api/v1/profil', expect.objectContaining({ method: 'PUT', body: expect.stringContaining('Ana') }));
  });

  it('retourne une absence de profil sans masquer les autres erreurs', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('', { status: 404 })));
    await expect(api.profilOuAbsent()).resolves.toBeUndefined();
  });

  it('utilise le contrat canonique pour créer et archiver un objectif', async () => {
    const fetch = vi.fn()
      .mockResolvedValueOnce(new Response(JSON.stringify({ id: 'objectif-1' }), { status: 201 }))
      .mockResolvedValueOnce(new Response(null, { status: 204 }));
    vi.stubGlobal('fetch', fetch);
    await api.creerObjectif({ nom: '10 km', date: '2026-10-10', unite: 'KM', type: '10_KM', url: 'https://example.test', priorite: 2, statut: 'PREVU', principal: true });
    await api.archiverObjectif('objectif-1');
    expect(fetch).toHaveBeenNthCalledWith(1, '/api/v1/objectifs', expect.objectContaining({ method: 'POST', body: expect.stringContaining('"url":"https://example.test"') }));
    expect(fetch).toHaveBeenNthCalledWith(2, '/api/v1/objectifs/objectif-1/archivage', expect.objectContaining({ method: 'POST' }));
  });

  it('envoie le consentement explicite lors de la connexion personnelle', async () => {
    const fetch = vi.fn().mockResolvedValue(new Response(JSON.stringify({ statut: 'MFA_REQUIS', mode: 'PERSONNEL', defiMfaId: 'defi-1' }), { status: 200 }));
    vi.stubGlobal('fetch', fetch);
    await api.connecterGarmin('ana@example.test', 'secret-temporaire');
    expect(fetch).toHaveBeenCalledWith('/api/v1/garmin/connexion', expect.objectContaining({ method: 'POST', body: JSON.stringify({ identifiant: 'ana@example.test', motDePasse: 'secret-temporaire', consentementRisques: true }) }));
  });

  it('confirme une candidate via le serveur Java', async () => {
    const fetch = vi.fn().mockResolvedValue(new Response(null, { status: 204 }));
    vi.stubGlobal('fetch', fetch);
    await api.confirmerCandidateGarmin('sync 1', 'activité/2');
    expect(fetch).toHaveBeenCalledWith('/api/v1/garmin/synchronisations/sync%201/confirmation', expect.objectContaining({ method: 'POST', body: JSON.stringify({ idExterne: 'activité/2' }) }));
  });

  it('borne la limite des sorties et utilise la route publique réelle', async () => {
    const fetch = vi.fn().mockResolvedValue(new Response('[]', { status: 200 }));
    vi.stubGlobal('fetch', fetch);
    await api.sorties(200);
    expect(fetch).toHaveBeenCalledWith('/api/v1/sorties?limite=100', expect.any(Object));
  });

  it('encode l’identifiant pour charger le détail factuel d’une sortie', async () => {
    const fetch = vi.fn().mockResolvedValue(new Response(JSON.stringify({ id: 'sortie 1', etatDecodage: 'EN_COURS', tours: [], zones: [], serie: [] }), { status: 200 }));
    vi.stubGlobal('fetch', fetch);
    await api.detailSortie('sortie 1');
    expect(fetch).toHaveBeenCalledWith('/api/v1/sorties/sortie%201', expect.any(Object));
  });
});
