import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it} from 'vitest';
import {useSearch} from './useSearch';

describe('useSearch', () => {
    beforeEach(() => {
        vi.useFakeTimers();
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    it('should initialize with empty query', () => {
        const {result} = renderHook(() => useSearch());

        expect(result.current.query).toBe('');
        expect(result.current.debouncedQuery).toBe('');
        expect(result.current.isActive).toBe(false);
    });

    it('should update query immediately when setQuery is called', () => {
        const {result} = renderHook(() => useSearch());

        act(() => {
            result.current.setQuery('test');
        });

        expect(result.current.query).toBe('test');
        expect(result.current.debouncedQuery).toBe('');
        expect(result.current.isActive).toBe(true);
    });

    it('should update debouncedQuery after debounce delay', () => {
        const {result} = renderHook(() => useSearch({debounceMs: 300}));

        act(() => {
            result.current.setQuery('test');
            vi.advanceTimersByTime(300);
        });

        expect(result.current.query).toBe('test');
        expect(result.current.debouncedQuery).toBe('test');
    });

    it('should trim query by default', () => {
        const {result} = renderHook(() => useSearch());

        act(() => {
            result.current.setQuery('  test query  ');
        });

        expect(result.current.query).toBe('test query');
    });

    it('should not trim query when trim option is false', () => {
        const {result} = renderHook(() => useSearch({trim: false}));

        act(() => {
            result.current.setQuery('  test query  ');
        });

        expect(result.current.query).toBe('  test query  ');
    });

    it('should use custom debounce delay', () => {
        const {result} = renderHook(() => useSearch({debounceMs: 500}));

        act(() => {
            result.current.setQuery('test');
            vi.advanceTimersByTime(400);
        });

        expect(result.current.debouncedQuery).toBe('');

        act(() => {
            vi.advanceTimersByTime(100);
        });

        expect(result.current.debouncedQuery).toBe('test');
    });

    it('should clear query and debouncedQuery', () => {
        const {result} = renderHook(() => useSearch());

        act(() => {
            result.current.setQuery('test');
            vi.advanceTimersByTime(300);
        });

        expect(result.current.query).toBe('test');
        expect(result.current.debouncedQuery).toBe('test');

        act(() => {
            result.current.clear();
        });

        expect(result.current.query).toBe('');
        expect(result.current.debouncedQuery).toBe('');
        expect(result.current.isActive).toBe(false);
    });

    it('should indicate when search is active', () => {
        const {result} = renderHook(() => useSearch());

        expect(result.current.isActive).toBe(false);

        act(() => {
            result.current.setQuery('a');
        });

        expect(result.current.isActive).toBe(true);

        act(() => {
            result.current.clear();
        });

        expect(result.current.isActive).toBe(false);
    });

    it('should cancel previous debounce when new query is set', () => {
        const {result} = renderHook(() => useSearch({debounceMs: 300}));

        act(() => {
            result.current.setQuery('first');
            vi.advanceTimersByTime(200);
            result.current.setQuery('second');
            vi.advanceTimersByTime(200);
        });

        expect(result.current.debouncedQuery).toBe('');

        act(() => {
            vi.advanceTimersByTime(100);
        });

        expect(result.current.debouncedQuery).toBe('second');
    });

    it('should handle empty query', () => {
        const {result} = renderHook(() => useSearch());

        act(() => {
            result.current.setQuery('');
        });

        expect(result.current.query).toBe('');
        expect(result.current.isActive).toBe(false);
    });

    it('should handle special characters in query', () => {
        const {result} = renderHook(() => useSearch());

        act(() => {
            result.current.setQuery('test@#$%^&*()');
        });

        expect(result.current.query).toBe('test@#$%^&*()');
        expect(result.current.isActive).toBe(true);
    });
});
