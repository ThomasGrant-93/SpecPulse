import { describe, expect, it, beforeEach, vi } from 'vitest';

import { api } from './api';
import { clearTokens, setTokens } from '@/features/auth/tokenStore';

function base64UrlEncode(input: string): string {
    return Buffer.from(input, 'utf8')
        .toString('base64')
        .replace(/=/g, '')
        .replace(/\+/g, '-')
        .replace(/\//g, '_');
}

function makeJwt(typ: 'access' | 'refresh'): string {
    const header = base64UrlEncode(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
    const payload = base64UrlEncode(JSON.stringify({ typ }));
    return `${header}.${payload}.signature`;
}

describe('API Authorization bearer token typing', () => {
    beforeEach(() => {
        clearTokens();
        vi.restoreAllMocks();
    });

    it('does not attach refresh tokens as Authorization bearer headers', async () => {
        const refreshToken = makeJwt('refresh');
        setTokens({ accessToken: refreshToken });

        let capturedAuthorization: unknown = undefined;

        await api.request({
            url: '/registry/validate',
            method: 'POST',
            data: {},
            adapter: (config) => {
                capturedAuthorization = (config.headers as any)?.Authorization;
                return Promise.resolve({
                    data: {},
                    status: 200,
                    statusText: 'OK',
                    headers: {},
                    config,
                });
            },
        });

        expect(capturedAuthorization).toBeUndefined();
    });

    it('attaches access tokens as Authorization bearer headers', async () => {
        const accessToken = makeJwt('access');
        setTokens({ accessToken });

        let capturedAuthorization: unknown = undefined;

        await api.request({
            url: '/registry/validate',
            method: 'POST',
            data: {},
            adapter: (config) => {
                capturedAuthorization = (config.headers as any)?.Authorization;
                return Promise.resolve({
                    data: {},
                    status: 200,
                    statusText: 'OK',
                    headers: {},
                    config,
                });
            },
        });

        expect(capturedAuthorization).toBe(`Bearer ${accessToken}`);
    });
});
