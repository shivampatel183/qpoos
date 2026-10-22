import {
    useCallback,
    useMemo,
    useState,
    type ReactNode,
} from 'react';
import { AlertCircle, Check, Info, X } from 'lucide-react';
import {
    DEFAULT_MESSAGE_DURATION,
    MessagePopupContext,
    type MessagePopupContextValue,
    type MessagePopupItem,
    type MessagePopupOptions,
    type MessagePopupType,
} from './MessagePopupContext';
import './MessagePopup.css';

const TYPE_META: Record<MessagePopupType, {
    icon: ReactNode;
}> = {
    success: {
        icon: <Check size={14} strokeWidth={3} />,
    },
    error: {
        icon: <AlertCircle size={14} strokeWidth={2.5} />,
    },
    info: {
        icon: <Info size={14} strokeWidth={2.5} />,
    },
};

export function MessagePopupProvider({ children }: { children: ReactNode }) {
    const [messages, setMessages] = useState<MessagePopupItem[]>([]);

    const dismissMessage = useCallback((id: number) => {
        setMessages((current) => current.filter((message) => message.id !== id));
    }, []);

    const showMessage = useCallback((options: MessagePopupOptions) => {
        const type = options.type ?? 'info';
        const id = Date.now() + Math.floor(Math.random() * 1000);
        const item: MessagePopupItem = {
            id,
            type,
            title: options.title ?? null,
            message: options.message,
            duration: options.duration ?? DEFAULT_MESSAGE_DURATION,
        };

        setMessages((current) => [item, ...current].slice(0, 4));

        if (item.duration > 0) {
            window.setTimeout(() => dismissMessage(id), item.duration);
        }

        return id;
    }, [dismissMessage]);

    const value = useMemo<MessagePopupContextValue>(() => ({
        showMessage,
        success: (message, title) => showMessage({ type: 'success', message, title }),
        error: (message, title) => showMessage({ type: 'error', message, title }),
        info: (message, title) => showMessage({ type: 'info', message, title }),
        dismissMessage,
    }), [dismissMessage, showMessage]);

    return (
        <MessagePopupContext.Provider value={value}>
            {children}

            <div className="message-popup__stack" aria-live="polite" aria-relevant="additions">
                {messages.map((message) => (
                    <div
                        key={message.id}
                        className={`message-popup message-popup--${message.type}`}
                        role={message.type === 'error' ? 'alert' : 'status'}
                    >
                        <span className="message-popup__icon" aria-hidden="true">
                            {TYPE_META[message.type].icon}
                        </span>

                        <div className="message-popup__content">
                            {message.title && (
                                <p className="message-popup__title">{message.title}</p>
                            )}
                            <p className="message-popup__text">{message.message}</p>
                        </div>

                        <button
                            type="button"
                            className="message-popup__close"
                            onClick={() => dismissMessage(message.id)}
                            aria-label="Dismiss message"
                        >
                            <X size={15} />
                        </button>
                    </div>
                ))}
            </div>
        </MessagePopupContext.Provider>
    );
}
