import {describe, expect, it} from 'vitest';
import {act, renderHook} from '@testing-library/react';
import {usePagination} from './usePagination';

describe('usePagination', () => {
    const mockData = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15];

    it('should return paginated data with default settings', () => {
        const {result} = renderHook(() => usePagination(mockData));

        expect(result.current.data).toEqual([1, 2, 3, 4, 5, 6, 7, 8, 9, 10]);
        expect(result.current.page).toBe(1);
        expect(result.current.totalPages).toBe(2);
        expect(result.current.totalItems).toBe(15);
    });

    it('should handle custom page size', () => {
        const {result} = renderHook(() => usePagination(mockData, {initialPageSize: 5}));

        expect(result.current.data).toEqual([1, 2, 3, 4, 5]);
        expect(result.current.totalPages).toBe(3);
    });

    it('should handle custom initial page', () => {
        const {result} = renderHook(() =>
            usePagination(mockData, {initialPage: 2, initialPageSize: 5})
        );

        expect(result.current.data).toEqual([6, 7, 8, 9, 10]);
        expect(result.current.page).toBe(2);
    });

    it('should navigate to next page', () => {
        const {result} = renderHook(() => usePagination(mockData, {initialPageSize: 5}));

        act(() => {
            result.current.nextPage();
        });

        expect(result.current.page).toBe(2);
        expect(result.current.data).toEqual([6, 7, 8, 9, 10]);
    });

    it('should navigate to previous page', () => {
        const {result} = renderHook(() =>
            usePagination(mockData, {initialPage: 2, initialPageSize: 5})
        );

        act(() => {
            result.current.previousPage();
        });

        expect(result.current.page).toBe(1);
        expect(result.current.data).toEqual([1, 2, 3, 4, 5]);
    });

    it('should not go below page 1', () => {
        const {result} = renderHook(() => usePagination(mockData));

        act(() => {
            result.current.previousPage();
        });

        expect(result.current.page).toBe(1);
    });

    it('should not go beyond total pages', () => {
        const {result} = renderHook(() => usePagination(mockData, {initialPageSize: 5}));

        // Try to go beyond multiple times
        act(() => {
            result.current.nextPage();
            result.current.nextPage();
            result.current.nextPage();
            result.current.nextPage();
        });

        // Should stay at last page (3)
        expect(result.current.page).toBe(3);
    });

    it('should change page size and reset to first page', () => {
        const {result} = renderHook(() =>
            usePagination(mockData, {initialPage: 2, initialPageSize: 5})
        );

        expect(result.current.page).toBe(2);

        act(() => {
            result.current.setPageSize(10);
        });

        expect(result.current.page).toBe(1);
        expect(result.current.data).toEqual([1, 2, 3, 4, 5, 6, 7, 8, 9, 10]);
        expect(result.current.totalPages).toBe(2);
    });

    it('should handle empty data', () => {
        const {result} = renderHook(() => usePagination([]));

        expect(result.current.data).toEqual([]);
        expect(result.current.page).toBe(1);
        // With empty data, we have 1 page (minimum)
        expect(result.current.totalPages).toBe(1);
        expect(result.current.totalItems).toBe(0);
    });

    it('should handle data less than page size', () => {
        const smallData = [1, 2, 3];
        const {result} = renderHook(() => usePagination(smallData));

        expect(result.current.data).toEqual([1, 2, 3]);
        expect(result.current.totalPages).toBe(1);
        expect(result.current.totalItems).toBe(3);
    });

    it('should handle exact page size match', () => {
        const exactData = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10];
        const {result} = renderHook(() => usePagination(exactData, {initialPageSize: 10}));

        expect(result.current.data).toEqual(exactData);
        expect(result.current.totalPages).toBe(1);
        expect(result.current.totalItems).toBe(10);
    });

    it('should calculate correct page boundaries', () => {
        const {result} = renderHook(() =>
            usePagination(mockData, {initialPage: 2, initialPageSize: 5})
        );

        // Page 2 with 5 items per page: items 6-10
        expect(result.current.data).toEqual([6, 7, 8, 9, 10]);
    });

    it('should handle last page with fewer items', () => {
        const data = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11];
        const {result} = renderHook(() =>
            usePagination(data, {initialPage: 3, initialPageSize: 5})
        );

        // Last page should have only 1 item
        expect(result.current.data).toEqual([11]);
        expect(result.current.totalPages).toBe(3);
    });

    describe('canPaginate', () => {
        it('should return true when pagination is needed', () => {
            const {result} = renderHook(() => usePagination(mockData, {initialPageSize: 5}));

            expect(result.current.canPaginate).toBe(true);
        });

        it('should return false when pagination is not needed', () => {
            const smallData = [1, 2, 3];
            const {result} = renderHook(() => usePagination(smallData));

            expect(result.current.canPaginate).toBe(false);
        });
    });

    describe('hasNextPage', () => {
        it('should return true when next page exists', () => {
            const {result} = renderHook(() => usePagination(mockData, {initialPageSize: 5}));

            expect(result.current.hasNextPage).toBe(true);
        });

        it('should return false on last page', () => {
            const {result} = renderHook(() =>
                usePagination(mockData, {initialPage: 3, initialPageSize: 5})
            );

            expect(result.current.hasNextPage).toBe(false);
        });
    });

    describe('hasPreviousPage', () => {
        it('should return false on first page', () => {
            const {result} = renderHook(() => usePagination(mockData));

            expect(result.current.hasPreviousPage).toBe(false);
        });

        it('should return true when not on first page', () => {
            const {result} = renderHook(() => usePagination(mockData, {initialPage: 2}));

            expect(result.current.hasPreviousPage).toBe(true);
        });
    });
});
