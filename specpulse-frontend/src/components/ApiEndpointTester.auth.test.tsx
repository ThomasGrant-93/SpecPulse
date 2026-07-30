import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import ApiEndpointTester from './ApiEndpointTester';
import type { AnySpec, ApiEndpoint, AuthCredentialsMap } from '@/types/openapi';
import { apiTester } from '@/services/apiTester';
import { HeadersProvider } from '@/features/spec/HeadersProvider';

vi.mock('@/services/apiTester', () => ({
    apiTester: {
        execute: vi.fn(),
    },
}));

const mockExecute = vi.mocked(apiTester.execute);

describe('ApiEndpointTester auth', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        window.sessionStorage.clear();
    });

    it('applies global OAS3 bearer security to headers', async () => {
        mockExecute.mockResolvedValueOnce({
            status: 200,
            statusText: 'OK',
            headers: {},
            body: {},
            duration: 1,
        });

        const spec: AnySpec = {
            openapi: '3.0.0',
            info: { title: 't', version: '1' },
            paths: {},
            components: {
                securitySchemes: {
                    bearerAuth: { type: 'http', scheme: 'bearer' },
                },
            },
            security: [{ bearerAuth: [] }],
        };

        const endpoint: ApiEndpoint = {
            path: '/test',
            method: 'get',
        };

        const authCredentials: AuthCredentialsMap = {
            bearerAuth: { token: 'abc' },
        };

        render(
            <HeadersProvider>
                <ApiEndpointTester
                    endpoint={endpoint}
                    baseUrl={'https://example.com/'}
                    spec={spec}
                    authCredentials={authCredentials}
                    canExecute={true}
                />
            </HeadersProvider>
        );

        const user = userEvent.setup();
        await user.click(screen.getByRole('button', { name: /execute/i }));

        await waitFor(() => {
            expect(mockExecute).toHaveBeenCalledTimes(1);
        });

        expect(mockExecute).toHaveBeenCalledWith({
            url: 'https://example.com/test',
            method: 'get',
            parameters: {},
            headers: {
                Authorization: 'Bearer abc',
                'Content-Type': 'application/json',
            },
            body: undefined,
        });
    });

    it('operation security: [] disables global OAS3 security requirements', async () => {
        mockExecute.mockResolvedValueOnce({
            status: 200,
            statusText: 'OK',
            headers: {},
            body: {},
            duration: 1,
        });

        const spec: AnySpec = {
            openapi: '3.0.0',
            info: { title: 't', version: '1' },
            paths: {},
            components: {
                securitySchemes: {
                    bearerAuth: { type: 'http', scheme: 'bearer' },
                },
            },
            security: [{ bearerAuth: [] }],
        };

        const endpoint: ApiEndpoint = {
            path: '/test',
            method: 'get',
            security: [],
        };

        const authCredentials: AuthCredentialsMap = {
            bearerAuth: { token: 'abc' },
        };

        render(
            <HeadersProvider>
                <ApiEndpointTester
                    endpoint={endpoint}
                    baseUrl={'https://example.com/'}
                    spec={spec}
                    authCredentials={authCredentials}
                    canExecute={true}
                />
            </HeadersProvider>
        );

        const user = userEvent.setup();
        await user.click(screen.getByRole('button', { name: /execute/i }));

        await waitFor(() => {
            expect(mockExecute).toHaveBeenCalledTimes(1);
        });

        const request = mockExecute.mock.calls[0][0];
        expect(request.parameters).toEqual({});
        expect(request.headers).toEqual({
            'Content-Type': 'application/json',
        });
        expect(request.headers).not.toHaveProperty('Authorization');
    });

    it('applies Swagger 2.0 apiKey in query parameters', async () => {
        mockExecute.mockResolvedValueOnce({
            status: 200,
            statusText: 'OK',
            headers: {},
            body: {},
            duration: 1,
        });

        const spec: AnySpec = {
            swagger: '2.0',
            info: { title: 't', version: '1' },
            paths: {},
            securityDefinitions: {
                api_key: { type: 'apiKey', in: 'query', name: 'api_key' },
            },
            security: [{ api_key: [] }],
        };

        const endpoint: ApiEndpoint = {
            path: '/test',
            method: 'get',
        };

        const authCredentials: AuthCredentialsMap = {
            api_key: { value: 'XYZ' },
        };

        render(
            <HeadersProvider>
                <ApiEndpointTester
                    endpoint={endpoint}
                    baseUrl={'https://example.com/'}
                    spec={spec}
                    authCredentials={authCredentials}
                    canExecute={true}
                />
            </HeadersProvider>
        );

        const user = userEvent.setup();
        await user.click(screen.getByRole('button', { name: /execute/i }));

        await waitFor(() => {
            expect(mockExecute).toHaveBeenCalledTimes(1);
        });

        const request = mockExecute.mock.calls[0][0];
        expect(request.url).toBe('https://example.com/test');
        expect(request.method).toBe('get');
        expect(request.parameters).toEqual({ api_key: 'XYZ' });
        expect(request.headers).toEqual({
            'Content-Type': 'application/json',
        });
        expect(request.headers).not.toHaveProperty('Authorization');
    });
});
