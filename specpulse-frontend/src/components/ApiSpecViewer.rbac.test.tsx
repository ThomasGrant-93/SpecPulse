import { describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import ApiSpecViewer from './ApiSpecViewer';
import type { AnySpec } from '@/types/openapi';
import { apiTester } from '@/services/apiTester';
import { HeadersProvider } from '@/features/spec/HeadersProvider';

vi.mock('@/services/apiTester', () => ({
    apiTester: {
        execute: vi.fn(),
    },
}));

const mockExecute = vi.mocked(apiTester.execute);

describe('ApiSpecViewer RBAC', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        window.sessionStorage.clear();
    });

    it('USER can open docs and execute from documentation UI', async () => {
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
            paths: {
                '/test': {
                    get: {
                        tags: ['default'],
                        summary: 's',
                        parameters: [],
                        responses: {
                            '200': { description: 'ok' },
                        },
                    },
                },
            },
        };

        render(
            <HeadersProvider>
                <ApiSpecViewer
                    spec={spec}
                    baseUrl={'https://example.com'}
                    authCredentials={{}}
                    canExecuteApiTests={true}
                />
            </HeadersProvider>
        );

        const endpointText = screen.getByText('/test');
        const endpointToggle = endpointText.closest('button');
        expect(endpointToggle).not.toBeNull();

        const user = userEvent.setup();
        await user.click(endpointToggle as HTMLButtonElement);

        const executeBtn = await screen.findByRole('button', { name: /execute/i });
        expect(executeBtn).not.toBeDisabled();
        await user.click(executeBtn);

        await waitFor(() => {
            expect(mockExecute).toHaveBeenCalledTimes(1);
        });
    });

    it('VIEWER can open docs but execute button is disabled', async () => {
        const spec: AnySpec = {
            openapi: '3.0.0',
            info: { title: 't', version: '1' },
            paths: {
                '/test': {
                    get: {
                        tags: ['default'],
                        summary: 's',
                        parameters: [],
                        responses: {
                            '200': { description: 'ok' },
                        },
                    },
                },
            },
        };

        render(
            <HeadersProvider>
                <ApiSpecViewer
                    spec={spec}
                    baseUrl={'https://example.com'}
                    authCredentials={{}}
                    canExecuteApiTests={false}
                />
            </HeadersProvider>
        );

        const endpointText = screen.getByText('/test');
        const endpointToggle = endpointText.closest('button');
        expect(endpointToggle).not.toBeNull();

        const user = userEvent.setup();
        await user.click(endpointToggle as HTMLButtonElement);

        const executeBtn = await screen.findByRole('button', { name: /execute/i });
        expect(executeBtn).toBeDisabled();
        await user.click(executeBtn);

        await waitFor(() => {
            expect(mockExecute).not.toHaveBeenCalled();
        });
    });
});
