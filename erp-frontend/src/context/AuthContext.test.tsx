import { act, render, screen, waitFor } from '@testing-library/react';
import { AuthProvider, useAuth } from './AuthContext';
import { getStoredCompany, setAccessToken, setStoredCompany } from '@/lib/session';
import { authService } from '@/services/authService';

vi.mock('@/services/authService', () => ({
    ApiError: class ApiError extends Error {
        readonly status: number;
        constructor(status: number, message: string) {
            super(message);
            this.status = status;
        }
    },
    authService: {
        refresh: vi.fn(),
    },
}));

const COMPANY_ID = '4d658f40-926d-4cac-9dc7-2e06211786ad';

function token({ companyId, expired = false }: { companyId?: string; expired?: boolean } = {}): string {
    const payload = {
        sub: 'user-1',
        email: 'user@example.com',
        exp: Math.floor(Date.now() / 1000) + (expired ? -60 : 900),
        ...(companyId ? { companyId } : {}),
    };
    const encoded = btoa(JSON.stringify(payload))
        .replace(/\+/g, '-')
        .replace(/\//g, '_')
        .replace(/=+$/g, '');
    return `header.${encoded}.signature`;
}

function Probe() {
    const { status, user } = useAuth();
    return <div data-testid="status">{status}{user ? `:${user.email}` : ''}</div>;
}

function renderProvider() {
    return render(
        <AuthProvider>
            <Probe />
        </AuthProvider>,
    );
}

describe('AuthProvider bootstrap', () => {
    beforeEach(() => {
        vi.mocked(authService.refresh).mockReset();
    });

    it('restores an authenticated session from a valid stored token without refreshing', async () => {
        setAccessToken(token({ companyId: COMPANY_ID }));
        setStoredCompany({ id: COMPANY_ID, name: 'Acme' });

        renderProvider();

        await waitFor(() => {
            expect(screen.getByTestId('status')).toHaveTextContent('authenticated:user@example.com');
        });
        expect(authService.refresh).not.toHaveBeenCalled();
        expect(getStoredCompany()).toEqual({ id: COMPANY_ID, name: 'Acme' });
    });

    it('refreshes with the stored company ID when the token is expired', async () => {
        setAccessToken(token({ companyId: COMPANY_ID }));
        setStoredCompany({ id: COMPANY_ID, name: 'Acme' });
        sessionStorage.setItem('auth_token', token({ companyId: COMPANY_ID, expired: true }));
        vi.mocked(authService.refresh).mockResolvedValue({
            accessToken: token({ companyId: COMPANY_ID }),
            expiresInSeconds: 900,
        });

        renderProvider();

        await waitFor(() => {
            expect(screen.getByTestId('status')).toHaveTextContent('authenticated');
        });
        expect(authService.refresh).toHaveBeenCalledWith(COMPANY_ID);
        expect(getStoredCompany()).toEqual({ id: COMPANY_ID, name: 'Acme' });
    });

    it('falls back to a company-less refresh when the company refresh is forbidden', async () => {
        const { ApiError } = await import('@/services/authService');
        sessionStorage.setItem('auth_token', token({ companyId: COMPANY_ID, expired: true }));
        setStoredCompany({ id: COMPANY_ID, name: 'Acme' });
        vi.mocked(authService.refresh)
            .mockRejectedValueOnce(new ApiError(403, 'Company access revoked'))
            .mockResolvedValueOnce({ accessToken: token(), expiresInSeconds: 900 });

        renderProvider();

        await waitFor(() => {
            expect(screen.getByTestId('status')).toHaveTextContent('authenticated');
        });
        expect(authService.refresh).toHaveBeenCalledTimes(2);
        expect(authService.refresh).toHaveBeenNthCalledWith(1, COMPANY_ID);
        expect(authService.refresh).toHaveBeenNthCalledWith(2);
        // Company-less token → stored company must be cleared by setAccessToken.
        expect(getStoredCompany()).toBeNull();
    });

    it('ends unauthenticated when the refresh fails outright', async () => {
        vi.mocked(authService.refresh).mockRejectedValue(new Error('network down'));

        renderProvider();

        await waitFor(() => {
            expect(screen.getByTestId('status')).toHaveTextContent('unauthenticated');
        });
    });

    it('stays in initializing while the bootstrap refresh is pending', async () => {
        let resolveRefresh!: (value: { accessToken: string; expiresInSeconds: number }) => void;
        vi.mocked(authService.refresh).mockReturnValue(
            new Promise(resolve => { resolveRefresh = resolve; }),
        );

        renderProvider();

        expect(screen.getByTestId('status')).toHaveTextContent('initializing');
        await act(async () => {
            resolveRefresh({ accessToken: token(), expiresInSeconds: 900 });
        });
        await waitFor(() => {
            expect(screen.getByTestId('status')).toHaveTextContent('authenticated');
        });
    });
});
