import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '@/context/AuthContext';
import { Spinner } from '@/components/ui/Spinner';

/**
 * Requires an authenticated session. Redirects to /login if not signed in.
 * Blocks on the initial bootstrap (spinner) so a page refresh never redirects
 * before the session restore attempt has finished.
 */
export function AuthGuard() {
    const { isAuthenticated, status } = useAuth();
    const location = useLocation();

    if (status === 'initializing') {
        return (
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: '100vh' }}>
                <Spinner size={24} />
            </div>
        );
    }
    if (!isAuthenticated) {
        return (
            <Navigate
                to="/login"
                replace
                state={{ from: location.pathname + location.search }}
            />
        );
    }
    return <Outlet />;
}
