import type { AuthUser, Company } from '@/types';
import { decodeAccessToken, getTokenCompanyId } from './jwt';

const TOKEN_KEY = 'auth_token';
const USER_KEY = 'auth_user';
const COMPANY_KEY = 'selected_company';

type TokenListener = (token: string | null) => void;
const tokenListeners = new Set<TokenListener>();

function readJson<T>(key: string): T | null {
    try {
        const value = sessionStorage.getItem(key);
        return value ? JSON.parse(value) as T : null;
    } catch {
        sessionStorage.removeItem(key);
        return null;
    }
}

function emitTokenChange(token: string | null): void {
    tokenListeners.forEach(listener => listener(token));
}

export function getAccessToken(): string | null {
    return sessionStorage.getItem(TOKEN_KEY);
}

export function setAccessToken(token: string): boolean {
    const claims = decodeAccessToken(token);
    if (!claims) {
        clearSession();
        return false;
    }

    sessionStorage.setItem(TOKEN_KEY, token);
    const companyId = getTokenCompanyId(token);
    const storedCompany = getStoredCompany();
    if (!companyId || (storedCompany && storedCompany.id !== companyId)) {
        sessionStorage.removeItem(COMPANY_KEY);
    }
    emitTokenChange(token);
    return true;
}

export function clearAccessToken(): void {
    sessionStorage.removeItem(TOKEN_KEY);
    emitTokenChange(null);
}

export function getStoredUser(): AuthUser | null {
    return readJson<AuthUser>(USER_KEY);
}

export function setStoredUser(user: AuthUser): void {
    sessionStorage.setItem(USER_KEY, JSON.stringify(user));
}

export function getStoredCompany(): Company | null {
    return readJson<Company>(COMPANY_KEY);
}

export function setStoredCompany(company: Company): void {
    sessionStorage.setItem(COMPANY_KEY, JSON.stringify(company));
}

export function clearStoredCompany(): void {
    sessionStorage.removeItem(COMPANY_KEY);
}

export function clearSession(): void {
    sessionStorage.removeItem(TOKEN_KEY);
    sessionStorage.removeItem(USER_KEY);
    sessionStorage.removeItem(COMPANY_KEY);
    emitTokenChange(null);
}

export function subscribeToTokenChanges(listener: TokenListener): () => void {
    tokenListeners.add(listener);
    return () => tokenListeners.delete(listener);
}
