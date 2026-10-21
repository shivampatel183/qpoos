import { fireEvent, render, screen } from '@testing-library/react';
import { Modal } from './Modal';

describe('Modal', () => {
    it('cannot be dismissed when dismissible is false', () => {
        const onClose = vi.fn();
        render(
            <Modal title="Renew session" dismissible={false} onClose={onClose}>
                <button type="button">Renew</button>
            </Modal>,
        );

        expect(screen.getByRole('dialog')).toHaveAttribute('aria-modal', 'true');
        expect(screen.queryByRole('button', { name: 'Close' })).not.toBeInTheDocument();
        fireEvent.click(screen.getByRole('dialog').parentElement!);
        fireEvent.keyDown(screen.getByRole('dialog'), { key: 'Escape' });
        expect(onClose).not.toHaveBeenCalled();
    });
});
