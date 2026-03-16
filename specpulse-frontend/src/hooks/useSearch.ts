import {useCallback, useRef, useState} from 'react';

interface UseSearchOptions {
    debounceMs?: number;
    trim?: boolean;
}

interface UseSearchReturn {
    query: string;
    setQuery: (query: string) => void;
    debouncedQuery: string;
    clear: () => void;
    isActive: boolean;
}

export function useSearch(options: UseSearchOptions = {}): UseSearchReturn {
    const {debounceMs = 300, trim = true} = options;
    const timeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

    const [query, setQueryState] = useState('');
    const [debouncedQuery, setDebouncedQuery] = useState('');

    const setQuery = useCallback(
        (newQuery: string) => {
            const processedQuery = trim ? newQuery.trim() : newQuery;
            setQueryState(processedQuery);

            // Cancel previous timeout
            if (timeoutRef.current) {
                clearTimeout(timeoutRef.current);
            }

            // Debounce the actual search query
            timeoutRef.current = setTimeout(() => {
                setDebouncedQuery(processedQuery);
            }, debounceMs);
        },
        [debounceMs, trim]
    );

    const clear = useCallback(() => {
        setQueryState('');
        setDebouncedQuery('');
        if (timeoutRef.current) {
            clearTimeout(timeoutRef.current);
        }
    }, []);

    const isActive = query.length > 0;

    return {
        query,
        setQuery,
        debouncedQuery,
        clear,
        isActive,
    };
}
