import { useEffect, useRef, useState } from 'react';
import { Outlet, useNavigate } from 'react-router-dom';
import { LogOut, RefreshCw } from 'lucide-react';
import { Modal } from '@/components/ui/Modal';
import { Spinner } from '@/components/ui/Spinner';
import { useAuth } from '@/context/AuthContext';
import { useCompany } from '@/context/CompanyContext';
import { ApiError, authService } from '@/services/authService';
import { getTokenCompanyId, getTokenExpiration } from '@/lib/jwt';
import {
    completeSessionRenewal,
    failSessionRenewal,
    requestSessionRenewal,
    subscribeToRenewal,
} from '@/lib/renewalCoordinator';
import './SessionRenewalManager.css';

export function SessionRenewalManager() {
    const navigate = useNavigate();
    const { token, status, replaceToken, logout } = useAuth();
    const { selectCompany } = useCompany();
    const [open, setOpen] = useState(false);
    const [renewing, setRenewing] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const warningTimer = useRef<number | null>(null);

    useEffect(() => subscribeToRenewal(setOpen), []);

    useEffect(() => {
        if (warningTimer.current !== null) {
            window.clearTimeout(warningTimer.current);
            warningTimer.current = null;
        }
        if (status !== 'authenticated' || !token) return;

        const expiration = getTokenExpiration(token);
        if (expiration === null) return;
        const warningTime = expiration * 1000 - Date.now() - 60_000;
        if (warningTime <= 0) {
            void requestSessionRenewal().catch(() => undefined);
            return;
        }

        warningTimer.current = window.setTimeout(() => {
            void requestSessionRenewal().catch(() => undefined);
        }, warningTime);

        return () => {
            if (warningTimer.current !== null) {
                window.clearTimeout(warningTimer.current);
                warningTimer.current = null;
            }
        };
    }, [status, token]);

    async function restoreCompanyLessSession(): Promise<string | null> {
        const response = await authService.refresh();
        if (!replaceToken(response.accessToken)) return null;
        selectCompany(null);
        navigate('/company-selection', { replace: true });
        return response.accessToken;
    }

    async function handleRenew() {
        if (renewing) return;
        setRenewing(true);
        setError(null);

        const expectedCompanyId = getTokenCompanyId(token);
        try {
            if (!expectedCompanyId) {
                const companyLessToken = await restoreCompanyLessSession();
                if (companyLessToken) completeSessionRenewal(companyLessToken);
                else failSessionRenewal();
                return;
            }

            const response = await authService.refresh(expectedCompanyId);
            if (getTokenCompanyId(response.accessToken) !== expectedCompanyId) {
                await restoreCompanyLessSession();
                failSessionRenewal(new Error('Renewed token did not preserve the selected company'));
                return;
            }
            if (!replaceToken(response.accessToken)) {
                throw new Error('The server returned an invalid access token');
            }

            completeSessionRenewal(response.accessToken);
        } catch (renewalError) {
            if (renewalError instanceof ApiError && renewalError.status === 401) {
                logout();
                selectCompany(null);
                failSessionRenewal(renewalError);
                navigate('/login', { replace: true });
                return;
            }

            if (renewalError instanceof ApiError && renewalError.status === 403) {
                try {
                    const companyLessToken = await restoreCompanyLessSession();
                    if (companyLessToken) failSessionRenewal(renewalError);
                    else throw new Error('The server returned an invalid access token');
                } catch (recoveryError) {
                    if (recoveryError instanceof ApiError && recoveryError.status === 401) {
                        logout();
                        selectCompany(null);
                        failSessionRenewal(recoveryError);
                        navigate('/login', { replace: true });
                        return;
                    }
                    setError(
                        recoveryError instanceof Error
                            ? recoveryError.message
                            : 'Unable to restore the session. Check your connection and try again.',
                    );
                }
                return;
            }

            setError(
                renewalError instanceof Error
                    ? renewalError.message
                    : 'Unable to renew the session. Check your connection and try again.',
            );
        } finally {
            setRenewing(false);
        }
    }

    async function handleLogout() {
        if (renewing) return;
        setRenewing(true);
        try {
            await authService.logout();
        } finally {
            logout();
            selectCompany(null);
            failSessionRenewal(new Error('User logged out'));
            navigate('/login', { replace: true });
            setRenewing(false);
        }
    }

    return (
        <>
            <Outlet />
            {open && (
                <Modal
                    title="Session expiring"
                    dismissible={false}
                    onClose={() => undefined}
                    maxWidth={440}
                    footer={(
                        <>
                            <button
                                type="button"
                                className="session-renewal__button session-renewal__button--secondary"
                                onClick={handleLogout}
                                disabled={renewing}
                            >
                                <LogOut size={16} />
                                Logout
                            </button>
                            <button
                                type="button"
                                className="session-renewal__button session-renewal__button--primary"
                                onClick={handleRenew}
                                disabled={renewing}
                            >
                                {renewing ? <Spinner size={16} white /> : <RefreshCw size={16} />}
                                Renew session
                            </button>
                        </>
                    )}
                >
                    <p className="session-renewal__message">
                        Your access token is about to expire. Renew to continue working in the current company.
                    </p>
                    {error && <p className="session-renewal__error" role="alert">{error}</p>}
                </Modal>
            )}
        </>
    );
}
