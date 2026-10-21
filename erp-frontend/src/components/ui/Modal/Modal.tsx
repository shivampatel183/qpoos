import { useEffect, useRef, type KeyboardEvent, type ReactNode } from 'react';
import { X } from 'lucide-react';
import './Modal.css';

interface ModalProps {
    title: string;
    children: ReactNode;
    footer?: ReactNode;
    maxWidth?: number;
    onClose: () => void;
    dismissible?: boolean;
}

const FOCUSABLE = [
    'button:not([disabled])',
    '[href]',
    'input:not([disabled])',
    'select:not([disabled])',
    'textarea:not([disabled])',
    '[tabindex]:not([tabindex="-1"])',
].join(',');

export function Modal({
    title,
    children,
    footer,
    maxWidth = 560,
    onClose,
    dismissible = true,
}: ModalProps) {
    const panelRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        const previousFocus = document.activeElement as HTMLElement | null;
        const firstFocusable = panelRef.current?.querySelector<HTMLElement>(FOCUSABLE);
        (firstFocusable ?? panelRef.current)?.focus();
        return () => previousFocus?.focus();
    }, []);

    function handleKeyDown(event: KeyboardEvent<HTMLDivElement>) {
        if (event.key === 'Escape') {
            if (dismissible) onClose();
            event.preventDefault();
            return;
        }
        if (event.key !== 'Tab' || !panelRef.current) return;

        const focusable = Array.from(panelRef.current.querySelectorAll<HTMLElement>(FOCUSABLE));
        if (focusable.length === 0) {
            event.preventDefault();
            panelRef.current.focus();
            return;
        }

        const first = focusable[0];
        const last = focusable[focusable.length - 1];
        if (event.shiftKey && document.activeElement === first) {
            event.preventDefault();
            last.focus();
        } else if (!event.shiftKey && document.activeElement === last) {
            event.preventDefault();
            first.focus();
        }
    }

    return (
        <div className="ui-modal__backdrop" onClick={dismissible ? onClose : undefined}>
            <div
                ref={panelRef}
                className="ui-modal__panel"
                style={{ maxWidth }}
                onClick={(event) => event.stopPropagation()}
                onKeyDown={handleKeyDown}
                role="dialog"
                aria-modal="true"
                aria-labelledby="ui-modal-title"
                tabIndex={-1}
            >
                <div className="ui-modal__header">
                    <h2 id="ui-modal-title" className="ui-modal__title">{title}</h2>
                    {dismissible && (
                        <button
                            type="button"
                            className="ui-modal__close"
                            onClick={onClose}
                            aria-label="Close"
                        >
                            <X size={18} />
                        </button>
                    )}
                </div>

                <div className="ui-modal__body">
                    {children}
                </div>

                {footer && (
                    <div className="ui-modal__footer">
                        {footer}
                    </div>
                )}
            </div>
        </div>
    );
}
