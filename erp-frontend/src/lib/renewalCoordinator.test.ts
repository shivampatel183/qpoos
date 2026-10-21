import {
    completeSessionRenewal,
    requestSessionRenewal,
    subscribeToRenewal,
} from './renewalCoordinator';

describe('renewal coordinator', () => {
    it('shares one renewal promise across concurrent requests', async () => {
        const listener = vi.fn();
        const unsubscribe = subscribeToRenewal(listener);

        const first = requestSessionRenewal();
        const second = requestSessionRenewal();
        expect(first).toBe(second);
        expect(listener).toHaveBeenCalledTimes(1);
        expect(listener).toHaveBeenCalledWith(true);

        completeSessionRenewal('new-token');
        await expect(first).resolves.toBe('new-token');
        await expect(second).resolves.toBe('new-token');
        expect(listener).toHaveBeenLastCalledWith(false);
        unsubscribe();
    });
});
