import { useEffect, useRef, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Building2, LogOut, ChevronRight, Plus } from 'lucide-react';
import { useCompany } from '@/context/CompanyContext';
import { useAuth } from '@/context/AuthContext';
import { companyService } from '@/services/companyService';
import { Spinner } from '@/components/ui/Spinner';
import { useMessagePopup } from '@/components/ui/MessagePopup';
import { CreateCompanyModal } from './CreateCompanyModal';
import { getTokenCompanyId } from '@/lib/jwt';
import { safeInternalPath } from '@/lib/navigation';
import { authService } from '@/services/authService';
import type { Company } from '@/types';
import './CompanyPage.css';

export function CompanyPage() {
    const navigate = useNavigate();
    const location = useLocation();
    const { selectCompany } = useCompany();
    const { user, replaceToken, logout } = useAuth();
    const messagePopup = useMessagePopup();
    const emptyMessageShown = useRef(false);
    const from = safeInternalPath((location.state as { from?: unknown } | null)?.from);

    const [companies, setCompanies] = useState<Company[]>([]);
    const [loading, setLoading] = useState(true);
    const [selectingCompanyId, setSelectingCompanyId] = useState<string | null>(null);
    const [showCreateModal, setShowCreateModal] = useState(false);

    useEffect(() => {
        let cancelled = false;

        companyService.list()
            .then(data => { if (!cancelled) setCompanies(data); })
            .catch(() => {
                if (!cancelled) {
                    messagePopup.error('Failed to load companies. Please try again.');
                }
            })
            .finally(() => { if (!cancelled) setLoading(false); });

        return () => { cancelled = true; };
    }, [messagePopup]);

    useEffect(() => {
        if (!loading && companies.length === 0 && !emptyMessageShown.current) {
            emptyMessageShown.current = true;
            messagePopup.info('No companies linked to your account. Create one to get started.');
        }
    }, [companies.length, loading, messagePopup]);

    async function handleSelect(company: Company) {
        setSelectingCompanyId(company.id);

        try {
            const response = await companyService.switchCompany(company.id);
            if (getTokenCompanyId(response.accessToken) !== company.id) {
                throw new Error('Company switch returned an unexpected company token');
            }
            if (!replaceToken(response.accessToken)) {
                throw new Error('Company switch returned an invalid access token');
            }
            selectCompany(company);
            navigate(from ?? '/', { replace: true });
        } catch {
            messagePopup.error('Failed to select company. Please try again.');
        } finally {
            setSelectingCompanyId(null);
        }
    }

    async function handleLogout() {
        await authService.logout();
        logout();
        navigate('/login', { replace: true });
    }

    function handleCompanyCreated(company: Company) {
        setShowCreateModal(false);
        messagePopup.success('Company created successfully.');
        // Add to the list and immediately select it
        setCompanies(prev => [...prev, company]);
        handleSelect(company);
    }

    return (
        <div className="company-page">
            <div className="company-page__card">

                <div className="company-page__header">
                    <img className="company-logo" src="/qpoos-logo.png" alt="Qpoos" />
                    <h1 className="company-page__title">Select a Company</h1>
                    <p className="company-page__subtitle">
                        Welcome back, <strong>{user?.name}</strong>. Choose which company to work in.
                    </p>
                </div>

                {loading && (
                    <div className="company-page__loading">
                        <Spinner size={24} />
                        <span>Loading companies…</span>
                    </div>
                )}

                {!loading && companies.length > 0 && (
                    <ul className="company-list">
                        {companies.map((company) => (
                            <li key={company.id}>
                                <button
                                    className="company-list__item"
                                    onClick={() => handleSelect(company)}
                                    disabled={selectingCompanyId !== null}
                                >
                                    <div className="company-list__icon">
                                        <Building2 size={20} />
                                    </div>
                                    <span className="company-list__name">{company.name}</span>
                                    {selectingCompanyId === company.id
                                        ? <Spinner size={16} />
                                        : <ChevronRight size={16} className="company-list__arrow" />}
                                </button>
                            </li>
                        ))}
                    </ul>
                )}

                {!loading && (
                    <button
                        className="company-create-btn"
                        onClick={() => setShowCreateModal(true)}
                    >
                        <Plus size={16} />
                        Create a new company
                    </button>
                )}

                <div className="company-page__footer">
                    <button className="company-logout-btn" onClick={handleLogout}>
                        <LogOut size={15} />
                        Sign in with a different account
                    </button>
                </div>
            </div>

            {showCreateModal && (
                <CreateCompanyModal
                    onClose={() => setShowCreateModal(false)}
                    onCreated={handleCompanyCreated}
                />
            )}
        </div>
    );
}
