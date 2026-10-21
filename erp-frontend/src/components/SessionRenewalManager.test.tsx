import { act, render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { SessionRenewalManager } from './SessionRenewalManager';
import { failSessionRenewal } from '@/lib/renewalCoordinator';

const authState = {
    token: null as string | null,
    status: 'authenticated' as const,
    replaceToken: vi.fn(),
    logout: vi.fn(),
};
const companyState = {
    selectCompany: vi.fn(),
};

vi.mock('@/context/AuthContext', () => ({
    useAuth: () => authState,
}));
vi.mock('@/context/CompanyContext', () => ({
    useCompany: () => companyState,
}));

function token(expiration: number): string {
    const encoded = btoa(JSON.stringify({
        sub: 'user-1',
        email: 'user@example.com',
        exp: expiration,
    })).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/g, '');
    return `header.${encoded}.signature`;
}

describe('SessionRenewalManager', () => {
    afterEach(() => {
        failSessionRenewal();
        vi.useRealTimers();
    });

    it('opens the renewal modal sixty seconds before expiration', async () => {
        vi.useFakeTimers();
        vi.setSystemTime(new Date('2026-07-16T00:00:00Z'));
        authState.token = token(Math.floor(Date.now() / 1000) + 120);

        render(
            <MemoryRouter initialEntries={['/']}>
                <Routes>
                    <Route element={<SessionRenewalManager />}>
                        <Route path="/" element={<div>Workspace</div>} />
                    </Route>
                </Routes>
            </MemoryRouter>,
        );

        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
        await act(() => vi.advanceTimersByTimeAsync(59_999));
        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
        await act(() => vi.advanceTimersByTimeAsync(1));
        expect(screen.getByRole('dialog')).toBeInTheDocument();
    });
});
