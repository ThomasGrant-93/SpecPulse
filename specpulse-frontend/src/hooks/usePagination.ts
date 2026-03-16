import {useCallback, useState} from 'react';

interface UsePaginationOptions {
    initialPage?: number;
    initialPageSize?: number;
}

interface UsePaginationReturn<T> {
    data: T[];
    page: number;
    pageSize: number;
    totalPages: number;
    totalItems: number;
    canPaginate: boolean;
    hasNextPage: boolean;
    hasPreviousPage: boolean;
    nextPage: () => void;
    previousPage: () => void;
    setPage: (page: number) => void;
    setPageSize: (size: number) => void;
    reset: () => void;
}

export function usePagination<T>(
    items: T[],
    options: UsePaginationOptions = {}
): UsePaginationReturn<T> {
    const {initialPage = 1, initialPageSize = 10} = options;

    const [page, setPage] = useState(initialPage);
    const [pageSize, setPageSize] = useState(initialPageSize);

    const totalItems = items.length;
    const totalPages = Math.max(1, Math.ceil(totalItems / pageSize));

    const startIndex = (page - 1) * pageSize;
    const endIndex = startIndex + pageSize;
    const data = items.slice(startIndex, endIndex);

    const canPaginate = totalItems > pageSize;
    const hasNextPage = page < totalPages;
    const hasPreviousPage = page > 1;

    const nextPage = useCallback(() => {
        if (hasNextPage) {
            setPage((prev) => Math.min(prev + 1, totalPages));
        }
    }, [hasNextPage, totalPages]);

    const previousPage = useCallback(() => {
        if (hasPreviousPage) {
            setPage((prev) => Math.max(prev - 1, 1));
        }
    }, [hasPreviousPage]);

    const handleSetPageSize = useCallback((size: number) => {
        setPageSize(size);
        setPage(1); // Reset to first page when changing page size
    }, []);

    const reset = useCallback(() => {
        setPage(initialPage);
        setPageSize(initialPageSize);
    }, [initialPage, initialPageSize]);

    return {
        data,
        page,
        pageSize,
        totalPages,
        totalItems,
        canPaginate,
        hasNextPage,
        hasPreviousPage,
        nextPage,
        previousPage,
        setPage,
        setPageSize: handleSetPageSize,
        reset,
    };
}
