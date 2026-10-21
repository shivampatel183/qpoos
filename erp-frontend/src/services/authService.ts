// Auth Service — typed wrappers around the /auth endpoints.
// Components import these functions; they never call apiClient directly.
//
// Usage:
//   import { authService } from '@/services/authService';
//   const { token, user } = await authService.login({ email, password });

import { apiClient, ApiError } from '@/lib/apiClient';
import { API } from '@/lib/apiConfig';
import { decodeAccessToken } from '@/lib/jwt';
import type { AuthUser } from '@/types';

export type { AuthUser };

// ─── Request / Response types ─────────────────────────────────────────────────

export interface LoginRequest {
    email: string;
    password: string;
    rememberMe?: boolean;
}

export interface RegisterRequest {
    email: string;
    password: string;
    mobile: string;
    name?: string;
}

export interface AuthResponse {
    token: string;
    user: AuthUser;
    expiresIn?: number;
}

export interface RefreshResponse {
    accessToken: string;
    expiresInSeconds: number;
}

export interface RegisterResponse {
    token?: string;
    user?: AuthUser;
    message?: string;
}

export interface ForgotPasswordRequest {
    email: string;
}

export interface ResetPasswordRequest {
    token: string;
    password: string;
}

// ─── Service ──────────────────────────────────────────────────────────────────

export const authService = {
    async login(payload: LoginRequest): Promise<AuthResponse> {
        const data = await apiClient.post<{ accessToken: string; expiresInSeconds: number }>(
            API.auth.login,
            { email: payload.email, password: payload.password },
            { skipAuth: true },
        );

        const token = data.accessToken;
        const claims = decodeAccessToken(token);
        if (!claims) throw new Error('Login returned an invalid access token');

        const user: AuthUser = {
            id: claims.sub,
            name: (claims.email ?? payload.email).split('@')[0],
            email: claims.email ?? payload.email,
            role: claims.role,
        };

        return { token, user, expiresIn: data.expiresInSeconds };
    },

    async register(payload: RegisterRequest): Promise<RegisterResponse> {
        const res = await apiClient.post<{ message: string }>(
            API.auth.register,
            { email: payload.email, password: payload.password },
            { skipAuth: true },
        );
        return { message: res.message };
    },

    async logout(): Promise<void> {
        try {
            await apiClient.post<void>(API.auth.logout, undefined, { bypassRenewal: true });
        } catch (err) {
            if (!(err instanceof ApiError)) throw err;
        }
    },

    async refresh(companyId?: string): Promise<RefreshResponse> {
        return apiClient.post<RefreshResponse>(
            API.auth.refreshToken,
            companyId ? { companyId } : undefined,
            { skipAuth: true, bypassRenewal: true },
        );
    },

    async getMe(): Promise<AuthUser> {
        return apiClient.get<AuthUser>(API.auth.me);
    },

    async forgotPassword(payload: ForgotPasswordRequest): Promise<{ message: string }> {
        return apiClient.post(API.auth.forgotPassword, payload, { skipAuth: true });
    },

    async resetPassword(payload: ResetPasswordRequest): Promise<{ message: string }> {
        return apiClient.post(API.auth.resetPassword, payload, { skipAuth: true });
    },
};

export { ApiError };
