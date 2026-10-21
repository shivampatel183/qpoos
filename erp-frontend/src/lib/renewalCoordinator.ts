type RenewalListener = (open: boolean) => void;

let renewalPromise: Promise<string> | null = null;
let resolveRenewal: ((token: string) => void) | null = null;
let rejectRenewal: ((reason?: unknown) => void) | null = null;
const listeners = new Set<RenewalListener>();

function notify(open: boolean): void {
    listeners.forEach(listener => listener(open));
}

export function requestSessionRenewal(): Promise<string> {
    if (!renewalPromise) {
        renewalPromise = new Promise<string>((resolve, reject) => {
            resolveRenewal = resolve;
            rejectRenewal = reject;
        }).finally(() => {
            renewalPromise = null;
            resolveRenewal = null;
            rejectRenewal = null;
            notify(false);
        });
        notify(true);
    }
    return renewalPromise;
}

export function completeSessionRenewal(token: string): void {
    resolveRenewal?.(token);
}

export function failSessionRenewal(reason?: unknown): void {
    rejectRenewal?.(reason ?? new Error('Session renewal was cancelled'));
}

export function subscribeToRenewal(listener: RenewalListener): () => void {
    listeners.add(listener);
    return () => listeners.delete(listener);
}
