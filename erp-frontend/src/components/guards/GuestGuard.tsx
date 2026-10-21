import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '@/context/AuthContext';
import { useCompany } from '@/context/CompanyContext';
import { getTokenCompanyId } from '@/lib/jwt';
import { safeInternalPath } from '@/lib/navigation';
import { Spinner } from '@/components/ui/Spinner';

/** Prevents authenticated users from seeing login/register pages. */
export function GuestGuard() {
    const { isAuthenticated, status, token } = useAuth();
    const { currentCompany } = useCompany();
    const location = useLocation();

    if (status === 'initializing') {
        return (
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: '100vh' }}>
                <Spinner size={24} />
            </div>
        );
    }

    const from = safeInternalPath((location.state as { from?: unknown } | null)?.from);
    const companyId = getTokenCompanyId(token);
    if (isAuthenticated && companyId && currentCompany?.id === companyId) {
        return <Navigate to={from ?? '/'} replace />;
    }
    if (isAuthenticated) {
        return <Navigate to="/company-selection" replace state={from ? { from } : undefined} />;
    }

    return <Outlet />;
}
