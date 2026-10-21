import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react';
import { decodeAccessToken, getTokenCompanyId, isTokenExpired } from '@/lib/jwt';
import {
    clearSession,
    getAccessToken,
    getStoredCompany,
    getStoredUser,
    setAccessToken,
    setStoredUser,
} from '@/lib/session';
import { ApiError, authService } from '@/services/authService';
import type { AuthUser } from '@/types';

export type { AuthUser };
export type AuthStatus = 'initializing' | 'authenticated' | 'unauthenticated';

interface AuthState {
    user: AuthUser | null;
    token: string | null;
    status: AuthStatus;
    isAuthenticated: boolean;
    replaceToken: (token: string, user?: AuthUser) => boolean;
    logout: () => void;
}

const AuthContext = createContext<AuthState | null>(null);
let bootstrapRefresh: ReturnType<typeof authService.refresh> | null = null;

function userFromToken(token: string, fallback?: AuthUser | null): AuthUser | null {
    const claims = decodeAccessToken(token);
    if (!claims) return null;
    const email = claims.email ?? fallback?.email;
    if (!email) return null;
    return {
        id: claims.sub,
        name: fallback?.name ?? email.split('@')[0],
        email,
        role: claims.role ?? fallback?.role,
        mobile: fallback?.mobile,
        avatarUrl: fallback?.avatarUrl,
    };
}

function refreshForBootstrap(companyId: string | null) {
    if (!bootstrapRefresh) {
        bootstrapRefresh = (async () => {
            if (companyId) {
                try {
                    return await authService.refresh(companyId);
                } catch (error) {
                    // The company may have been deleted or revoked — fall back
                    // to a company-less session instead of dropping the user.
                    if (!(error instanceof ApiError && error.status === 403)) throw error;
                }
            }
            return authService.refresh();
        })().finally(() => {
            bootstrapRefresh = null;
        });
    }
    return bootstrapRefresh;
}

export function AuthProvider({ children }: { children: ReactNode }) {
    const [token, setToken] = useState<string | null>(null);
    const [user, setUser] = useState<AuthUser | null>(null);
    const [status, setStatus] = useState<AuthStatus>('initializing');

    const replaceToken = useCallback((newToken: string, newUser?: AuthUser): boolean => {
        const resolvedUser = userFromToken(newToken, newUser ?? getStoredUser());
        if (!resolvedUser || !setAccessToken(newToken)) {
            clearSession();
            setToken(null);
            setUser(null);
            setStatus('unauthenticated');
            return false;
        }

        setStoredUser(resolvedUser);
        setToken(newToken);
        setUser(resolvedUser);
        setStatus('authenticated');
        return true;
    }, []);

    const logout = useCallback(() => {
        clearSession();
        setToken(null);
        setUser(null);
        setStatus('unauthenticated');
    }, []);

    useEffect(() => {
        let active = true;
        const storedToken = getAccessToken();
        if (storedToken && !isTokenExpired(storedToken)) {
            replaceToken(storedToken);
            return () => {
                active = false;
            };
        }

        // The access token is missing or expired. Try to restore the session
        // from the refresh cookie, preserving the previously selected company.
        // The stored company/user stay untouched until the outcome is known —
        // clearing them here is what caused company loss on page refresh.
        const companyIdHint = getTokenCompanyId(storedToken) ?? getStoredCompany()?.id ?? null;
        refreshForBootstrap(companyIdHint)
            .then(response => {
                if (active) replaceToken(response.accessToken);
            })
            .catch(() => {
                if (active) logout();
            });

        return () => {
            active = false;
        };
    }, [logout, replaceToken]);

    return (
        <AuthContext.Provider
            value={{
                user,
                token,
                status,
                isAuthenticated: status === 'authenticated' && token !== null,
                replaceToken,
                logout,
            }}
        >
            {children}
        </AuthContext.Provider>
    );
}

export function useAuth(): AuthState {
    const ctx = useContext(AuthContext);
    if (!ctx) throw new Error('useAuth must be used inside <AuthProvider>');
    return ctx;
}
