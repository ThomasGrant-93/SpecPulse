import {useCallback, useEffect, useState} from 'react';

interface UseDebounceOptions {
    delay: number;
}

interface UseDebounceReturn<T> {
    debouncedValue: T;
    isDebouncing: boolean;
    reset: () => void;
}

export function useDebounce<T>(
    value: T,
    options: UseDebounceOptions = {delay: 300}
): UseDebounceReturn<T> {
    const {delay} = options;
    const [debouncedValue, setDebouncedValue] = useState<T>(value);
    const [isDebouncing, setIsDebouncing] = useState(false);

    useEffect(() => {
        setIsDebouncing(true);

        const handler = setTimeout(() => {
            setDebouncedValue(value);
            setIsDebouncing(false);
        }, delay);

        return () => {
            clearTimeout(handler);
            setIsDebouncing(false);
        };
    }, [value, delay]);

    const reset = useCallback(() => {
        setDebouncedValue(value);
        setIsDebouncing(false);
    }, [value]);

    return {
        debouncedValue,
        isDebouncing,
        reset,
    };
}
