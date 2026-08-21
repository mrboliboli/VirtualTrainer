import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { Navigation } from './Navigation';

describe('Navigation', () => {
  it('appelle la navigation lorsque la synchronisation est choisie', async () => {
    const naviguer = vi.fn();
    render(<Navigation page="accueil" naviguer={naviguer} />);
    await userEvent.click(screen.getByRole('button', { name: 'Synchroniser' }));
    expect(naviguer).toHaveBeenCalledWith('synchroniser');
  });
});
