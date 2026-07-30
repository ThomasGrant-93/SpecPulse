import { beforeEach, describe, expect, it, vi } from 'vitest';

import { api } from '@/services/api';

import { server } from './server';
import {
    defaultGroups,
    mockErrorHandler,
    mockGroupsEmpty,
    mockGroupsNetworkError,
    mockValidationHandler,
} from './handlers';

describe('MSW API mocks', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('mocks GET /api/v1/groups with default payload', async () => {
        const res = await api.get('/groups');
        expect(res.status).toBe(200);
        expect(res.data).toEqual(defaultGroups);
    });

    it('supports per-test override with an HTTP error', async () => {
        server.use(mockErrorHandler('/groups', 500));

        await expect(api.get('/groups')).rejects.toMatchObject({
            response: expect.objectContaining({ status: 500 }),
        });
    });

    it('supports empty payloads', async () => {
        server.use(mockGroupsEmpty());

        const res = await api.get('/groups');
        expect(res.status).toBe(200);
        expect(res.data).toEqual([]);
    });

    it('supports network failures', async () => {
        server.use(mockGroupsNetworkError());

        await expect(api.get('/groups')).rejects.toMatchObject({
            message: expect.stringContaining('Network Error'),
        });
    });

    it('mocks validation endpoint with custom result', async () => {
        server.use(mockValidationHandler({ valid: false, errors: ['Invalid spec'] }));

        const response = await api.post('/registry/validate', { name: 'x', openApiUrl: 'y' });
        expect(response.data).toEqual({ valid: false, errors: ['Invalid spec'] });
    });
});
