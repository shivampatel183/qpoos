import {
    decodeAccessToken,
    getTokenCompanyId,
    getTokenExpiration,
    hasValidCompanyClaim,
    isTokenExpired,
} from './jwt';

function token(payload: Record<string, unknown>): string {
    const encoded = btoa(JSON.stringify(payload))
        .replace(/\+/g, '-')
        .replace(/\//g, '_')
        .replace(/=+$/g, '');
    return `header.${encoded}.signature`;
}

describe('JWT utilities', () => {
    it('decodes company and expiration claims', () => {
        const companyId = '4d658f40-926d-4cac-9dc7-2e06211786ad';
        const accessToken = token({ sub: 'user-1', companyId, exp: 2_000_000_000 });

        expect(decodeAccessToken(accessToken)).toMatchObject({ sub: 'user-1', companyId });
        expect(getTokenCompanyId(accessToken)).toBe(companyId);
        expect(getTokenExpiration(accessToken)).toBe(2_000_000_000);
        expect(hasValidCompanyClaim(accessToken)).toBe(true);
        expect(isTokenExpired(accessToken)).toBe(false);
    });

    it('returns no claims for malformed tokens', () => {
        expect(decodeAccessToken('not-a-jwt')).toBeNull();
        expect(decodeAccessToken('a.invalid-json.c')).toBeNull();
    });

    it('treats missing, blank, and malformed company IDs as no company', () => {
        expect(getTokenCompanyId(token({ sub: 'user-1', exp: 2_000_000_000 }))).toBeNull();
        expect(getTokenCompanyId(token({ sub: 'user-1', companyId: ' ', exp: 2_000_000_000 }))).toBeNull();
        expect(getTokenCompanyId(token({ sub: 'user-1', companyId: 'wrong', exp: 2_000_000_000 }))).toBeNull();
    });
});
