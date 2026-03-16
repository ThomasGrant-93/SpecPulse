import '@testing-library/jest-dom';
import {cleanup} from '@testing-library/react';
import {afterEach, vi} from 'vitest';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';

// Create a default QueryClient for tests
const testQueryClient = new QueryClient({
    defaultOptions: {
        queries: {
            retry: false,
            logger: {
                log: console.log,
                warn: console.warn,
                error: () => {}, // Silence error logs in tests
            },
        },
    },
});

// Cleanup after each test
afterEach(() => {
    cleanup();
    testQueryClient.clear();
});

// Mock window.matchMedia
Object.defineProperty(window, 'matchMedia', {
    writable: true,
    value: vi.fn().mockImplementation((query) => ({
        matches: false,
        media: query,
        onchange: null,
        addListener: vi.fn(),
        removeListener: vi.fn(),
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
        dispatchEvent: vi.fn(),
    })),
});

// Mock window.scrollTo
Object.defineProperty(window, 'scrollTo', {
    writable: true,
    value: vi.fn(),
});

// Export for use in tests
export {testQueryClient};
