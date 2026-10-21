import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { CompanyGuard } from './CompanyGuard';

const authState = {
    token: null as string | null,
    status: 'authenticated' as const,
    logout: vi.fn(),
};
const companyState = {
    currentCompany: null as { id: string; name: string } | null,
    selectCompany: vi.fn(),
};

vi.mock('@/context/AuthContext', () => ({
    useAuth: () => authState,
}));
vi.mock('@/context/CompanyContext', () => ({
    useCompany: () => companyState,
}));

function token(companyId?: string): string {
    const payload = {
        sub: 'user-1',
        exp: Math.floor(Date.now() / 1000) + 900,
        ...(companyId ? { companyId } : {}),
    };
    const encoded = btoa(JSON.stringify(payload))
        .replace(/\+/g, '-')
        .replace(/\//g, '_')
        .replace(/=+$/g, '');
    return `header.${encoded}.signature`;
}

function renderGuard() {
    return render(
        <MemoryRouter initialEntries={['/protected']}>
            <Routes>
                <Route element={<CompanyGuard />}>
                    <Route path="/protected" element={<div>Protected content</div>} />
                </Route>
                <Route path="/company-selection" element={<div>Company selection</div>} />
                <Route path="/login" element={<div>Login</div>} />
            </Routes>
        </MemoryRouter>,
    );
}

describe('CompanyGuard', () => {
    beforeEach(() => {
        authState.logout.mockClear();
        companyState.selectCompany.mockClear();
        companyState.currentCompany = null;
    });

    it('redirects when the token has no company claim', () => {
        authState.token = token();
        renderGuard();
        expect(screen.getByText('Company selection')).toBeInTheDocument();
    });

    it('clears stale company state and redirects when IDs disagree', async () => {
        authState.token = token('4d658f40-926d-4cac-9dc7-2e06211786ad');
        companyState.currentCompany = {
            id: 'de9e67b8-84f5-4984-8149-54820f55bb83',
            name: 'Wrong Company',
        };
        renderGuard();

        expect(screen.getByText('Company selection')).toBeInTheDocument();
        await waitFor(() => expect(companyState.selectCompany).toHaveBeenCalledWith(null));
    });

    it('renders protected content only when company IDs match', () => {
        const companyId = '4d658f40-926d-4cac-9dc7-2e06211786ad';
        authState.token = token(companyId);
        companyState.currentCompany = { id: companyId, name: 'Current Company' };
        renderGuard();
        expect(screen.getByText('Protected content')).toBeInTheDocument();
    });
});
