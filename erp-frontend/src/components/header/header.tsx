import { useNavigate } from 'react-router-dom';
import { Building2, ChevronDown, LogOut, UserCircle } from 'lucide-react';
import { useAuth } from '@/context/AuthContext';
import { useCompany } from '@/context/CompanyContext';
import { authService } from '@/services/authService';
import './header.css';

export function Header() {
    const { user, logout } = useAuth();
    const { currentCompany, selectCompany } = useCompany();
    const navigate = useNavigate();

    async function handleLogout() {
        await authService.logout();
        logout();
        selectCompany(null);
        navigate('/login', { replace: true });
    }

    function handleSwitchCompany() {
        selectCompany(null);
        navigate('/company-selection', { replace: true });
    }

    return (
        <header className="top-header">
            <button className="header-company" onClick={handleSwitchCompany} title="Switch company">
                <Building2 size={14} />
                <span>{currentCompany?.name ?? 'Select Company'}</span>
                <ChevronDown size={13} />
            </button>

            <div className="header-right">
                <span className="header-username">
                    <UserCircle size={15} />
                    {user?.name ?? user?.email ?? 'Account'}
                </span>
                <button className="header-logout" onClick={handleLogout} title="Sign out">
                    <LogOut size={15} />
                    Sign out
                </button>
            </div>
        </header>
    );
}
