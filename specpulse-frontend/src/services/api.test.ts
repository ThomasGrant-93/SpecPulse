import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import axios from 'axios';
import {auditApi, diffsApi, pullApi, registryApi, versionsApi} from './api';

// Mock axios module
vi.mock('axios', () => {
    const mockAxiosInstance = {
        get: vi.fn(),
        post: vi.fn(),
        put: vi.fn(),
        delete: vi.fn(),
        interceptors: {
            request: {use: vi.fn(), eject: vi.fn()},
            response: {use: vi.fn(), eject: vi.fn()},
        },
    };
    return {
        default: {
            ...mockAxiosInstance,
            create: () => mockAxiosInstance,
        },
        __esModule: true,
    };
});

const mockedAxios = vi.mocked(axios);

describe('API Service', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    describe('Registry API', () => {
        it('should get all services', async () => {
            const mockServices = [
                {
                    id: 1,
                    name: 'Service 1',
                    openApiUrl: 'https://api1.com/openapi.json',
                    enabled: true,
                },
                {
                    id: 2,
                    name: 'Service 2',
                    openApiUrl: 'https://api2.com/openapi.json',
                    enabled: false,
                },
            ];

            mockedAxios.get.mockResolvedValueOnce({data: mockServices});

            const response = await registryApi.getAll();

            expect(mockedAxios.get).toHaveBeenCalledWith('/registry');
            expect(response.data).toEqual(mockServices);
        });

        it('should get enabled services', async () => {
            const mockServices = [
                {
                    id: 1,
                    name: 'Service 1',
                    openApiUrl: 'https://api1.com/openapi.json',
                    enabled: true,
                },
            ];

            mockedAxios.get.mockResolvedValueOnce({data: mockServices});

            const response = await registryApi.getEnabled();

            expect(mockedAxios.get).toHaveBeenCalledWith('/registry/enabled');
            expect(response.data).toEqual(mockServices);
        });

        it('should get service by id', async () => {
            const mockService = {
                id: 1,
                name: 'Service 1',
                openApiUrl: 'https://api1.com/openapi.json',
                enabled: true,
            };

            mockedAxios.get.mockResolvedValueOnce({data: mockService});

            const response = await registryApi.getById(1);

            expect(mockedAxios.get).toHaveBeenCalledWith('/registry/1');
            expect(response.data).toEqual(mockService);
        });

        it('should create service', async () => {
            const mockService = {
                id: 1,
                name: 'New Service',
                openApiUrl: 'https://api.new.com/openapi.json',
                enabled: true,
            };

            mockedAxios.post.mockResolvedValueOnce({data: mockService});

            const response = await registryApi.create({
                name: 'New Service',
                openApiUrl: 'https://api.new.com/openapi.json',
            });

            expect(mockedAxios.post).toHaveBeenCalledWith('/registry', {
                name: 'New Service',
                openApiUrl: 'https://api.new.com/openapi.json',
            });
            expect(response.data).toEqual(mockService);
        });

        it('should update service', async () => {
            const mockService = {
                id: 1,
                name: 'Updated Service',
                openApiUrl: 'https://api.updated.com/openapi.json',
                enabled: false,
            };

            mockedAxios.put.mockResolvedValueOnce({data: mockService});

            const response = await registryApi.update(1, {
                name: 'Updated Service',
                enabled: false,
            });

            expect(mockedAxios.put).toHaveBeenCalledWith('/registry/1', {
                name: 'Updated Service',
                enabled: false,
            });
            expect(response.data).toEqual(mockService);
        });

        it('should delete service', async () => {
            mockedAxios.delete.mockResolvedValueOnce({data: {}});

            await registryApi.delete(1);

            expect(mockedAxios.delete).toHaveBeenCalledWith('/registry/1');
        });
    });

    describe('Versions API', () => {
        it('should get versions by service id', async () => {
            const mockVersions = [
                {id: 1, serviceId: 1, versionHash: 'abc123', pulledAt: '2024-01-01T00:00:00Z'},
                {id: 2, serviceId: 1, versionHash: 'def456', pulledAt: '2024-01-02T00:00:00Z'},
            ];

            mockedAxios.get.mockResolvedValueOnce({data: mockVersions});

            const response = await versionsApi.getByService(1);

            expect(mockedAxios.get).toHaveBeenCalledWith('/versions/service/1');
            expect(response.data).toEqual(mockVersions);
        });

        it('should get latest version', async () => {
            const mockVersion = {
                id: 2,
                serviceId: 1,
                versionHash: 'def456',
                pulledAt: '2024-01-02T00:00:00Z',
            };

            mockedAxios.get.mockResolvedValueOnce({data: mockVersion});

            const response = await versionsApi.getLatest(1);

            expect(mockedAxios.get).toHaveBeenCalledWith('/versions/service/1/latest');
            expect(response.data).toEqual(mockVersion);
        });

        it('should get version by id', async () => {
            const mockVersion = {
                id: 1,
                serviceId: 1,
                versionHash: 'abc123',
                pulledAt: '2024-01-01T00:00:00Z',
            };

            mockedAxios.get.mockResolvedValueOnce({data: mockVersion});

            const response = await versionsApi.getById(1);

            expect(mockedAxios.get).toHaveBeenCalledWith('/versions/1');
            expect(response.data).toEqual(mockVersion);
        });
    });

    describe('Diffs API', () => {
        it('should get diffs by service id', async () => {
            const mockDiffs = [
                {
                    id: 1,
                    serviceId: 1,
                    fromVersionId: 1,
                    toVersionId: 2,
                    diffContent: 'diff content',
                    hasBreakingChanges: false,
                    breakingChangesCount: 0,
                    createdAt: '2024-01-02T00:00:00Z',
                },
            ];

            mockedAxios.get.mockResolvedValueOnce({data: mockDiffs});

            const response = await diffsApi.getByService(1);

            expect(mockedAxios.get).toHaveBeenCalledWith('/diffs/service/1');
            expect(response.data).toEqual(mockDiffs);
        });

        it('should get diff by id', async () => {
            const mockDiff = {
                id: 1,
                serviceId: 1,
                fromVersionId: 1,
                toVersionId: 2,
                diffContent: 'diff content',
                hasBreakingChanges: true,
                breakingChangesCount: 2,
                createdAt: '2024-01-02T00:00:00Z',
            };

            mockedAxios.get.mockResolvedValueOnce({data: mockDiff});

            const response = await diffsApi.getById(1);

            expect(mockedAxios.get).toHaveBeenCalledWith('/diffs/1');
            expect(response.data).toEqual(mockDiff);
        });

        it('should compare specs', async () => {
            const mockDiff = {
                hasBreakingChanges: false,
                breakingChangesCount: 0,
                diffContent: 'diff content',
            };

            mockedAxios.post.mockResolvedValueOnce({data: mockDiff});

            const response = await diffsApi.compare('old spec', 'new spec');

            expect(mockedAxios.post).toHaveBeenCalledWith('/diffs/compare', {
                oldSpec: 'old spec',
                newSpec: 'new spec',
            });
            expect(response.data).toEqual(mockDiff);
        });
    });

    describe('Audit API', () => {
        it('should get audit logs by service id', async () => {
            const mockLogs = [
                {
                    id: 1,
                    serviceId: 1,
                    eventType: 'SERVICE_CREATED',
                    createdAt: '2024-01-01T00:00:00Z',
                },
            ];

            mockedAxios.get.mockResolvedValueOnce({data: mockLogs});

            const response = await auditApi.getByService(1);

            expect(mockedAxios.get).toHaveBeenCalledWith('/audit/service/1');
            expect(response.data).toEqual(mockLogs);
        });

        it('should get recent audit logs with default limit', async () => {
            const mockLogs = [
                {id: 1, eventType: 'SERVICE_CREATED', createdAt: '2024-01-01T00:00:00Z'},
            ];

            mockedAxios.get.mockResolvedValueOnce({data: mockLogs});

            const response = await auditApi.getRecent();

            expect(mockedAxios.get).toHaveBeenCalledWith('/audit/recent?limit=50');
            expect(response.data).toEqual(mockLogs);
        });

        it('should get recent audit logs with custom limit', async () => {
            const mockLogs = [
                {id: 1, eventType: 'SERVICE_CREATED', createdAt: '2024-01-01T00:00:00Z'},
            ];

            mockedAxios.get.mockResolvedValueOnce({data: mockLogs});

            const response = await auditApi.getRecent(10);

            expect(mockedAxios.get).toHaveBeenCalledWith('/audit/recent?limit=10');
            expect(response.data).toEqual(mockLogs);
        });
    });

    describe('Pull API', () => {
        it('should trigger pull for service', async () => {
            const mockResult = {
                success: true,
                hasChanges: true,
                newVersionId: 2,
                previousVersionId: 1,
                versionHash: 'abc123',
            };

            mockedAxios.post.mockResolvedValueOnce({data: mockResult});

            const response = await pullApi.triggerService(1);

            expect(mockedAxios.post).toHaveBeenCalledWith('/pull/service/1');
            expect(response.data).toEqual(mockResult);
        });

        it('should trigger pull for all services', async () => {
            const mockResult = {
                success: true,
                pulledCount: 5,
                results: [],
            };

            mockedAxios.post.mockResolvedValueOnce({data: mockResult});

            const response = await pullApi.triggerAll();

            expect(mockedAxios.post).toHaveBeenCalledWith('/pull/all');
            expect(response.data).toEqual(mockResult);
        });
    });
});
