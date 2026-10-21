import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { RouterProvider } from 'react-router-dom';
import './theme.css';
import { router } from './router';
import { AuthProvider } from './context/AuthContext';
import { CompanyProvider } from './context/CompanyContext';
import { ErrorBoundary } from './components/ErrorBoundary';
import { MessagePopupProvider } from './components/ui/MessagePopup';

createRoot(document.getElementById('root')!).render(
    <StrictMode>
        <ErrorBoundary>
            <AuthProvider>
                <CompanyProvider>
                    <MessagePopupProvider>
                        <RouterProvider router={router} />
                    </MessagePopupProvider>
                </CompanyProvider>
            </AuthProvider>
        </ErrorBoundary>
    </StrictMode>,
);
