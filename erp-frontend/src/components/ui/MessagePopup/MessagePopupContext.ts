import { createContext } from 'react';

export type MessagePopupType = 'success' | 'error' | 'info';

export interface MessagePopupOptions {
    title?: string;
    message: string;
    type?: MessagePopupType;
    duration?: number;
}

export interface MessagePopupItem {
    id: number;
    type: MessagePopupType;
    title: string | null;
    message: string;
    duration: number;
}

export interface MessagePopupContextValue {
    showMessage: (options: MessagePopupOptions) => number;
    success: (message: string, title?: string) => number;
    error: (message: string, title?: string) => number;
    info: (message: string, title?: string) => number;
    dismissMessage: (id: number) => void;
}

export const DEFAULT_MESSAGE_DURATION = 4200;

export const MessagePopupContext = createContext<MessagePopupContextValue | null>(null);
