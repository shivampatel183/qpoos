import { act, render, screen } from '@testing-library/react';
import { CompanyProvider, useCompany } from './CompanyContext';
import { clearSession, setAccessToken, setStoredCompany } from '@/lib/session';

const COMPANY_ID = '4d658f40-926d-4cac-9dc7-2e06211786ad';
const OTHER_COMPANY_ID = 'de9e67b8-84f5-4984-8149-54820f55bb83';

function token(companyId?: string): string {
    const payload = {
        sub: 'user-1',
        email: 'user@example.com',
        exp: Math.floor(Date.now() / 1000) + 900,
        ...(companyId ? { companyId } : {}),
    };
    const encoded = btoa(JSON.stringify(payload))
        .replace(/\+/g, '-')
        .replace(/\//g, '_')
        .replace(/=+$/g, '');
    return `header.${encoded}.signature`;
}

function Probe() {
    const { currentCompany } = useCompany();
    return <div data-testid="company">{currentCompany?.name ?? 'none'}</div>;
}

function renderProvider() {
    return render(
        <CompanyProvider>
            <Probe />
        </CompanyProvider>,
    );
}

describe('CompanyProvider', () => {
    it('hydrates from storage when token and stored company agree', () => {
        setAccessToken(token(COMPANY_ID));
        setStoredCompany({ id: COMPANY_ID, name: 'Acme' });

        renderProvider();

        expect(screen.getByTestId('company')).toHaveTextContent('Acme');
    });

    it('does not destroy the stored company while there is no token yet', () => {
        // Simulates a page refresh with an expired access token: the company
        // must survive until the bootstrap refresh decides the session's fate.
        setStoredCompany({ id: COMPANY_ID, name: 'Acme' });

        renderProvider();

        expect(screen.getByTestId('company')).toHaveTextContent('none');

        // Bootstrap refresh lands with the same company claim → restored.
        act(() => {
            setAccessToken(token(COMPANY_ID));
        });
        expect(screen.getByTestId('company')).toHaveTextContent('Acme');
    });

    it('drops the company when a new token carries a different company claim', () => {
        setAccessToken(token(COMPANY_ID));
        setStoredCompany({ id: COMPANY_ID, name: 'Acme' });
        renderProvider();

        act(() => {
            setAccessToken(token(OTHER_COMPANY_ID));
        });

        expect(screen.getByTestId('company')).toHaveTextContent('none');
    });

    it('clears the company on logout', () => {
        setAccessToken(token(COMPANY_ID));
        setStoredCompany({ id: COMPANY_ID, name: 'Acme' });
        renderProvider();

        act(() => {
            clearSession();
        });

        expect(screen.getByTestId('company')).toHaveTextContent('none');
    });
});
