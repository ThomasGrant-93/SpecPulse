import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import ApiEndpointTester from './ApiEndpointTester';
import type { AnySpec, ApiEndpoint } from '@/types/openapi';
import { apiTester } from '@/services/apiTester';
import { HeadersProvider } from '@/features/spec/HeadersProvider';

vi.mock('@/services/apiTester', () => ({
    apiTester: {
        execute: vi.fn(),
    },
}));

const mockExecute = vi.mocked(apiTester.execute);

describe('ApiEndpointTester RBAC', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        window.sessionStorage.clear();
    });

    it('USER can execute API test requests from UI', async () => {
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
        };

        const endpoint: ApiEndpoint = {
            path: '/test',
            method: 'get',
        };

        render(
            <HeadersProvider>
                <ApiEndpointTester
                    endpoint={endpoint}
                    baseUrl={'https://example.com/'}
                    spec={spec}
                    authCredentials={{}}
                    canExecute={true}
                />
            </HeadersProvider>
        );

        const user = userEvent.setup();
        const btn = screen.getByRole('button', { name: /execute/i });
        expect(btn).not.toBeDisabled();
        await user.click(btn);

        await waitFor(() => {
            expect(mockExecute).toHaveBeenCalledTimes(1);
        });
    });

    it('VIEWER cannot execute API test requests from UI', async () => {
        const spec: AnySpec = {
            openapi: '3.0.0',
            info: { title: 't', version: '1' },
            paths: {},
        };

        const endpoint: ApiEndpoint = {
            path: '/test',
            method: 'get',
        };

        render(
            <HeadersProvider>
                <ApiEndpointTester
                    endpoint={endpoint}
                    baseUrl={'https://example.com/'}
                    spec={spec}
                    authCredentials={{}}
                    canExecute={false}
                />
            </HeadersProvider>
        );

        const user = userEvent.setup();
        const btn = screen.getByRole('button', { name: /execute/i });
        expect(btn).toBeDisabled();

        await user.click(btn);

        await waitFor(() => {
            expect(mockExecute).not.toHaveBeenCalled();
        });
    });
});
