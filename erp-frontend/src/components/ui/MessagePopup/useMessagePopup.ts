import { useContext } from 'react';
import {
    MessagePopupContext,
    type MessagePopupContextValue,
} from './MessagePopupContext';

export function useMessagePopup(): MessagePopupContextValue {
    const context = useContext(MessagePopupContext);
    if (!context) {
        throw new Error('useMessagePopup must be used inside <MessagePopupProvider>');
    }
    return context;
}
