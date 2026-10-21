import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react';
import { getTokenCompanyId } from '@/lib/jwt';
import {
    clearStoredCompany,
    getAccessToken,
    getStoredCompany,
    setStoredCompany,
    subscribeToTokenChanges,
} from '@/lib/session';
import type { Company } from '@/types';

export type { Company };

interface CompanyState {
    currentCompany: Company | null;
    selectCompany: (company: Company | null) => void;
}

const CompanyContext = createContext<CompanyState | null>(null);

/**
 * Derives the active company from the token's company claim plus stored
 * metadata. Pure read — storage consistency (clearing mismatched companies)
 * is enforced by setAccessToken/clearSession in lib/session, so the stored
 * company must survive here while auth is still bootstrapping.
 */
function companyFromToken(token: string | null): Company | null {
    const companyId = getTokenCompanyId(token);
    const storedCompany = getStoredCompany();
    if (!companyId || !storedCompany || storedCompany.id !== companyId) {
        return null;
    }
    return storedCompany;
}

export function CompanyProvider({ children }: { children: ReactNode }) {
    const [currentCompany, setCurrentCompany] = useState<Company | null>(
        () => companyFromToken(getAccessToken()),
    );

    useEffect(() => subscribeToTokenChanges(newToken => {
        // Re-derive from storage so a bootstrap refresh that preserves the
        // company claim restores the selection instead of dropping it.
        setCurrentCompany(current => {
            const derived = companyFromToken(newToken);
            return current && derived && current.id === derived.id ? current : derived;
        });
    }), []);

    const selectCompany = useCallback((company: Company | null) => {
        if (!company) {
            clearStoredCompany();
            setCurrentCompany(null);
            return;
        }

        if (getTokenCompanyId(getAccessToken()) !== company.id) {
            clearStoredCompany();
            setCurrentCompany(null);
            return;
        }

        setStoredCompany(company);
        setCurrentCompany(company);
    }, []);

    return (
        <CompanyContext.Provider value={{ currentCompany, selectCompany }}>
            {children}
        </CompanyContext.Provider>
    );
}

export function useCompany(): CompanyState {
    const ctx = useContext(CompanyContext);
    if (!ctx) throw new Error('useCompany must be used inside <CompanyProvider>');
    return ctx;
}
