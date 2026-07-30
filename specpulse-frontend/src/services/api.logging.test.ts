import { describe, expect, it, vi, beforeEach } from 'vitest';

import { groupsApi } from '@/services/api';
import { server } from '@/test/mocks/server';
import { mockErrorHandler } from '@/test/mocks/handlers';

describe('API logging', () => {
    beforeEach(() => {
        vi.restoreAllMocks();
    });

    it('suppresses verbose API request/response console logs during tests', async () => {
        const warnSpy = vi.spyOn(console, 'warn').mockImplementation(() => undefined);
        const errorSpy = vi.spyOn(console, 'error').mockImplementation(() => undefined);

        server.use(mockErrorHandler('/groups', 500));

        await expect(groupsApi.getAll()).rejects.toBeTruthy();

        expect(warnSpy).not.toHaveBeenCalledWith(expect.stringContaining('[API Request]'));
        expect(errorSpy).not.toHaveBeenCalledWith(expect.stringContaining('[API Response Error]'));
    });
});
