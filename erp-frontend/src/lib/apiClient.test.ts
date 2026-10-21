import { apiClient, ApiError } from './apiClient';
import { completeSessionRenewal, subscribeToRenewal } from './renewalCoordinator';
import { setAccessToken } from './session';

function token(): string {
    const encoded = btoa(JSON.stringify({
        sub: 'user-1',
        email: 'user@example.com',
        exp: 2_000_000_000,
    })).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/g, '');
    return `header.${encoded}.signature`;
}

describe('API client renewal coordination', () => {
    it('retries a failed request only once after renewal', async () => {
        setAccessToken(token());
        const fetchMock = vi.fn()
            .mockResolvedValueOnce(new Response(null, { status: 401 }))
            .mockResolvedValueOnce(new Response(
                JSON.stringify({ message: 'still unauthorized' }),
                { status: 401, headers: { 'Content-Type': 'application/json' } },
            ));
        vi.stubGlobal('fetch', fetchMock);
        const unsubscribe = subscribeToRenewal(open => {
            if (open) completeSessionRenewal(token());
        });

        await expect(apiClient.get('/protected')).rejects.toBeInstanceOf(ApiError);
        expect(fetchMock).toHaveBeenCalledTimes(2);

        unsubscribe();
        vi.unstubAllGlobals();
    });
});
