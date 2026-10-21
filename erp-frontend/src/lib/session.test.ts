import {
    getStoredCompany,
    setAccessToken,
    setStoredCompany,
} from './session';

function token(companyId?: string): string {
    const payload = {
        sub: 'user-1',
        email: 'user@example.com',
        exp: 2_000_000_000,
        ...(companyId ? { companyId } : {}),
    };
    const encoded = btoa(JSON.stringify(payload))
        .replace(/\+/g, '-')
        .replace(/\//g, '_')
        .replace(/=+$/g, '');
    return `header.${encoded}.signature`;
}

describe('session storage synchronization', () => {
    it('clears a stale company when the token has no company claim', () => {
        setStoredCompany({ id: '4d658f40-926d-4cac-9dc7-2e06211786ad', name: 'Old Company' });

        setAccessToken(token());

        expect(getStoredCompany()).toBeNull();
    });

    it('clears a stored company when its ID differs from the token', () => {
        setStoredCompany({ id: '4d658f40-926d-4cac-9dc7-2e06211786ad', name: 'Old Company' });

        setAccessToken(token('de9e67b8-84f5-4984-8149-54820f55bb83'));

        expect(getStoredCompany()).toBeNull();
    });

    it('preserves matching company metadata', () => {
        const company = { id: '4d658f40-926d-4cac-9dc7-2e06211786ad', name: 'Current Company' };
        setStoredCompany(company);

        setAccessToken(token(company.id));

        expect(getStoredCompany()).toEqual(company);
    });
});
