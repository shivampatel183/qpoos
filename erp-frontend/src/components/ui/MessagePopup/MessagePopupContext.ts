import { createContext } from 'react';

export type MessagePopupType = 'success' | 'error' | 'info';

export interface MessagePopupOptions {
    title?: string;
    message: string;
    type?: MessagePopupType;
    duration?: number;
}

export interface MessagePopupItem extends Required<MessagePopupOptions> {
    id: number;
}

export interface MessagePopupContextValue {
    showMessage: (options: MessagePopupOptions) => number;
    success: (message: string, title?: string) => number;
    error: (message: string, title?: string) => number;
    info: (message: string, title?: string) => number;
    dismissMessage: (id: number) => void;
}

export const DEFAULT_MESSAGE_DURATION = 4200;

export const MESSAGE_TYPE_TITLES: Record<MessagePopupType, string> = {
    success: 'Success',
    error: 'Error',
    info: 'Info',
};

export const MessagePopupContext = createContext<MessagePopupContextValue | null>(null);
