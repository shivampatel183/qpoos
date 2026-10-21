import { BASE_URL } from './apiConfig';
import { getAccessToken } from './session';
import { requestSessionRenewal } from './renewalCoordinator';

export class ApiError extends Error {
    readonly status: number;
    readonly data?: unknown;

    constructor(status: number, message: string, data?: unknown) {
        super(message);
        this.name = 'ApiError';
        this.status = status;
        this.data = data;
    }
}

export interface RequestOptions {
    headers?: Record<string, string>;
    skipAuth?: boolean;
    bypassRenewal?: boolean;
    signal?: AbortSignal;
}

function buildHeaders(options?: RequestOptions): HeadersInit {
    const headers: Record<string, string> = {
        'Content-Type': 'application/json',
        ...options?.headers,
    };

    if (!options?.skipAuth) {
        const token = getAccessToken();
        if (token) headers.Authorization = `Bearer ${token}`;
    }

    return headers;
}

async function parseResponse<T>(res: Response): Promise<T> {
    if (res.status === 204) return undefined as T;

    let body: unknown;
    try {
        body = await res.json();
    } catch {
        body = null;
    }

    if (!res.ok) {
        const message =
            (body as { message?: string } | null)?.message ??
            `Request failed with status ${res.status}`;
        throw new ApiError(res.status, message, body);
    }

    return body as T;
}

async function request<T>(
    method: string,
    path: string,
    body?: unknown,
    options?: RequestOptions,
    retryOnUnauthorized = true,
): Promise<T> {
    const res = await fetch(`${BASE_URL}${path}`, {
        method,
        headers: buildHeaders(options),
        credentials: 'include',
        signal: options?.signal,
        body: body !== undefined ? JSON.stringify(body) : undefined,
    });

    if (
        res.status === 401 &&
        retryOnUnauthorized &&
        !options?.skipAuth &&
        !options?.bypassRenewal
    ) {
        await requestSessionRenewal();
        return request<T>(method, path, body, options, false);
    }

    return parseResponse<T>(res);
}

export const apiClient = {
    get<T>(path: string, options?: RequestOptions): Promise<T> {
        return request<T>('GET', path, undefined, options);
    },

    post<T>(path: string, body?: unknown, options?: RequestOptions): Promise<T> {
        return request<T>('POST', path, body, options);
    },

    put<T>(path: string, body?: unknown, options?: RequestOptions): Promise<T> {
        return request<T>('PUT', path, body, options);
    },

    patch<T>(path: string, body?: unknown, options?: RequestOptions): Promise<T> {
        return request<T>('PATCH', path, body, options);
    },

    delete<T = void>(path: string, options?: RequestOptions): Promise<T> {
        return request<T>('DELETE', path, undefined, options);
    },
};
