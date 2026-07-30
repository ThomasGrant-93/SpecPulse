import { http, HttpResponse } from 'msw';

import type { ServiceGroup } from '@/types';

export const defaultGroups: ServiceGroup[] = [
    {
        id: 1,
        name: 'Default Group',
        description: 'Test group',
        sortOrder: 0,
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
        parentGroupId: null as any,
        childGroups: [],
        serviceCount: 0,
        services: [],
    },
];

export function mockGroupsEmpty() {
    return http.get('*/api/v1/groups', () => {
        return HttpResponse.json([], { status: 200 });
    });
}

export function mockGroupsNetworkError() {
    return http.get('*/api/v1/groups', () => {
        return HttpResponse.error();
    });
}

export function mockErrorHandler(path: string, status: number) {
    // Default to GET semantics; override via server.use(http.post(...)) if needed.
    return http.get(`*/api/v1${path}`, () => {
        return HttpResponse.json({ error: 'Mock error' }, { status });
    });
}

export function mockValidationHandler(result: { valid: boolean; errors: string[] }) {
    return http.post('*/api/v1/registry/validate', async () => {
        return HttpResponse.json(result, { status: 200 });
    });
}

export function mockValidationHandlerDelayed(
    result: { valid: boolean; errors: string[] },
    delayMs: number
) {
    return http.post('*/api/v1/registry/validate', async () => {
        await new Promise((resolve) => setTimeout(resolve, delayMs));
        return HttpResponse.json(result, { status: 200 });
    });
}

export function mockValidationNetworkError() {
    return http.post('*/api/v1/registry/validate', () => {
        return HttpResponse.error();
    });
}

export function mockValidationNonJsonResponse() {
    return http.post('*/api/v1/registry/validate', () => {
        return HttpResponse.text('Internal Server Error', { status: 200 });
    });
}

// Default network responses for component tests.
export const handlers = [
    http.get('*/api/v1/groups', () => {
        return HttpResponse.json(defaultGroups, { status: 200 });
    }),

    http.get('*/api/v1/groups/root', () => {
        return HttpResponse.json(defaultGroups, { status: 200 });
    }),

    http.get('*/api/v1/groups/:id', () => {
        return HttpResponse.json(defaultGroups[0], { status: 200 });
    }),

    http.get('*/api/v1/registry', () => {
        return HttpResponse.json([], { status: 200 });
    }),
    http.get('*/api/v1/registry/enabled', () => {
        return HttpResponse.json([], { status: 200 });
    }),

    http.get('*/api/v1/registry/search', () => {
        return HttpResponse.json([], { status: 200 });
    }),
    http.get('*/api/v1/registry/search/suggestions', () => {
        return HttpResponse.json([], { status: 200 });
    }),

    http.get('*/api/v1/versions/service/:id', () => {
        return HttpResponse.json([], { status: 200 });
    }),
    http.get('*/api/v1/versions/:id', () => {
        return HttpResponse.json({}, { status: 200 });
    }),

    http.get('*/api/v1/diffs/service/:id', () => {
        return HttpResponse.json([], { status: 200 });
    }),
    http.get('*/api/v1/diffs/:id', () => {
        return HttpResponse.json({}, { status: 200 });
    }),
    http.post('*/api/v1/diffs/compare', () => {
        return HttpResponse.json({}, { status: 200 });
    }),

    http.get('*/api/v1/audit/service/:id', () => {
        return HttpResponse.json([], { status: 200 });
    }),
    http.get('*/api/v1/audit/recent', () => {
        return HttpResponse.json([], { status: 200 });
    }),

    http.post('*/api/v1/pull/service/:id', () => {
        return HttpResponse.json({ success: true, hasChanges: false }, { status: 200 });
    }),
    http.post('*/api/v1/pull/all', () => {
        return HttpResponse.json({ success: true, hasChanges: false }, { status: 200 });
    }),

    http.get('*/api/v1/settings', () => {
        return HttpResponse.json([], { status: 200 });
    }),
    http.get('*/api/v1/settings/:category', () => {
        return HttpResponse.json({}, { status: 200 });
    }),
    http.get('*/api/v1/settings/categories', () => {
        return HttpResponse.json([], { status: 200 });
    }),

    http.get('*/api/v1/settings/public', () => {
        return HttpResponse.json([], { status: 200 });
    }),
    http.put('*/api/v1/settings/:category/:key', () => {
        return HttpResponse.json({}, { status: 200 });
    }),

    http.post('*/api/v1/registry/validate', () => {
        return HttpResponse.json({ valid: true, errors: [] }, { status: 200 });
    }),

    http.post('*/api/v1/registry', () => {
        return HttpResponse.json({}, { status: 201 });
    }),
    http.put('*/api/v1/registry/:id', () => {
        return HttpResponse.json({}, { status: 200 });
    }),
    http.delete('*/api/v1/registry/:id', () => {
        return new HttpResponse(null, { status: 204 });
    }),

    // Test-only catch-all: avoid real network calls to unknown /api/v1 routes.
    http.all('*/api/v1/*', ({ request }) => {
        const method = request.method.toUpperCase();
        if (method === 'DELETE') {
            return new HttpResponse(null, { status: 204 });
        }
        if (method === 'POST' || method === 'PUT' || method === 'PATCH') {
            return HttpResponse.json({}, { status: 200 });
        }
        if (method === 'GET') {
            return HttpResponse.json([], { status: 200 });
        }
        return HttpResponse.json({}, { status: 200 });
    }),
];
