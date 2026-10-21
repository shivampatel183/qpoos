export interface AccessTokenClaims {
    sub: string;
    email?: string;
    role?: string;
    companyId?: string;
    iat?: number;
    exp: number;
}

const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

function decodeBase64Url(value: string): string {
    const base64 = value.replace(/-/g, '+').replace(/_/g, '/');
    const padded = base64.padEnd(Math.ceil(base64.length / 4) * 4, '=');
    const bytes = Uint8Array.from(atob(padded), character => character.charCodeAt(0));
    return new TextDecoder().decode(bytes);
}

export function decodeAccessToken(token: string): AccessTokenClaims | null {
    try {
        const parts = token.split('.');
        if (parts.length !== 3 || !parts[1]) return null;

        const claims = JSON.parse(decodeBase64Url(parts[1])) as Partial<AccessTokenClaims>;
        if (typeof claims.sub !== 'string' || !claims.sub.trim()) return null;
        if (typeof claims.exp !== 'number' || !Number.isFinite(claims.exp)) return null;

        return claims as AccessTokenClaims;
    } catch {
        return null;
    }
}

export function getTokenCompanyId(token: string | null): string | null {
    if (!token) return null;
    const companyId = decodeAccessToken(token)?.companyId;
    if (typeof companyId !== 'string') return null;
    const normalized = companyId.trim();
    return UUID_PATTERN.test(normalized) ? normalized : null;
}

export function getTokenExpiration(token: string | null): number | null {
    if (!token) return null;
    return decodeAccessToken(token)?.exp ?? null;
}

export function isTokenExpired(token: string | null): boolean {
    const expiration = getTokenExpiration(token);
    return expiration === null || expiration * 1000 <= Date.now();
}

export function hasValidCompanyClaim(token: string | null): boolean {
    return getTokenCompanyId(token) !== null;
}
