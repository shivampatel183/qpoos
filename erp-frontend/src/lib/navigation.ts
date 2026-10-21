// Navigation helpers shared by guards and auth pages.

/**
 * Validates a post-login redirect target coming from router state.
 * Accepts only same-app absolute paths; rejects external URLs and
 * protocol-relative values ("//evil.com") to prevent open redirects.
 */
export function safeInternalPath(value: unknown): string | null {
    if (typeof value !== 'string' || !value.startsWith('/') || value.startsWith('//')) return null;
    return value;
}
