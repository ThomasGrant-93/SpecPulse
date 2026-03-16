import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {act, renderHook} from '@testing-library/react';
import {useDebounce} from './useDebounce';

describe('useDebounce', () => {
    beforeEach(() => {
        vi.useFakeTimers();
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    it('should return initial value immediately', () => {
        const {result} = renderHook(() => useDebounce('initial', {delay: 300}));

        expect(result.current.debouncedValue).toBe('initial');
        // Initial state should be debouncing since we just set the value
        expect(result.current.isDebouncing).toBe(true);
    });

    it('should debounce value changes', () => {
        const {result, rerender} = renderHook(({value}) => useDebounce(value, {delay: 300}), {
            initialProps: {value: 'initial'},
        });

        // Change value
        rerender({value: 'updated'});

        // Should be debouncing
        expect(result.current.isDebouncing).toBe(true);
        expect(result.current.debouncedValue).toBe('initial');

        // Fast-forward time
        act(() => {
            vi.advanceTimersByTime(300);
        });

        // Should have updated value
        expect(result.current.isDebouncing).toBe(false);
        expect(result.current.debouncedValue).toBe('updated');
    });

    it('should not update debounced value before delay', () => {
        const {result, rerender} = renderHook(({value}) => useDebounce(value, {delay: 500}), {
            initialProps: {value: 'initial'},
        });

        rerender({value: 'updated'});

        // Advance time but not enough
        act(() => {
            vi.advanceTimersByTime(400);
        });

        expect(result.current.isDebouncing).toBe(true);
        expect(result.current.debouncedValue).toBe('initial');

        // Complete the delay
        act(() => {
            vi.advanceTimersByTime(100);
        });

        expect(result.current.isDebouncing).toBe(false);
        expect(result.current.debouncedValue).toBe('updated');
    });

    it('should reset debouncing when value changes multiple times', () => {
        const {result, rerender} = renderHook(({value}) => useDebounce(value, {delay: 300}), {
            initialProps: {value: 'initial'},
        });

        // Multiple rapid changes
        rerender({value: 'update1'});
        rerender({value: 'update2'});
        rerender({value: 'update3'});

        expect(result.current.isDebouncing).toBe(true);
        expect(result.current.debouncedValue).toBe('initial');

        // Fast-forward time
        act(() => {
            vi.advanceTimersByTime(300);
        });

        // Should have the last value
        expect(result.current.debouncedValue).toBe('update3');
        expect(result.current.isDebouncing).toBe(false);
    });

    it('should provide reset function', () => {
        const {result, rerender} = renderHook(({value}) => useDebounce(value, {delay: 300}), {
            initialProps: {value: 'initial'},
        });

        rerender({value: 'updated'});
        expect(result.current.isDebouncing).toBe(true);

        // Reset
        act(() => {
            result.current.reset();
        });

        expect(result.current.isDebouncing).toBe(false);
        expect(result.current.debouncedValue).toBe('updated');
    });

    it('should use default delay when not provided', () => {
        const {result, rerender} = renderHook(({value}) => useDebounce(value), {
            initialProps: {value: 'initial'},
        });

        rerender({value: 'updated'});
        expect(result.current.isDebouncing).toBe(true);

        // Default delay is 300ms
        act(() => {
            vi.advanceTimersByTime(300);
        });

        expect(result.current.debouncedValue).toBe('updated');
        expect(result.current.isDebouncing).toBe(false);
    });

    it('should handle custom delay', () => {
        const {result, rerender} = renderHook(() => useDebounce('test', {delay: 1000}));

        rerender({value: 'updated'});

        act(() => {
            vi.advanceTimersByTime(500);
        });

        expect(result.current.isDebouncing).toBe(true);

        act(() => {
            vi.advanceTimersByTime(500);
        });

        expect(result.current.isDebouncing).toBe(false);
    });
});
