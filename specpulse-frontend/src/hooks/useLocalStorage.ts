import {useCallback, useState} from 'react';

interface UseLocalStorageOptions<T> {
    serializer?: (value: T) => string;
    deserializer?: (value: string) => T;
}

interface UseLocalStorageReturn<T> {
    value: T | null;
    setValue: (value: T) => void;
    removeValue: () => void;
}

export function useLocalStorage<T>(
    key: string,
    initialValue: T | null = null,
    options: UseLocalStorageOptions<T> = {}
): UseLocalStorageReturn<T> {
    const {serializer = JSON.stringify, deserializer = JSON.parse} = options;

    const [storedValue, setStoredValue] = useState<T | null>(() => {
        try {
            const item = window.localStorage.getItem(key);
            if (item) {
                return deserializer(item);
            }
            return initialValue;
        } catch (error) {
            console.warn(`Error reading localStorage key "${key}":`, error);
            return initialValue;
        }
    });

    const setValue = useCallback(
        (value: T) => {
            try {
                setStoredValue(value);
                window.localStorage.setItem(key, serializer(value));
            } catch (error) {
                console.warn(`Error setting localStorage key "${key}":`, error);
            }
        },
        [key, serializer]
    );

    const removeValue = useCallback(() => {
        try {
            setStoredValue(null);
            window.localStorage.removeItem(key);
        } catch (error) {
            console.warn(`Error removing localStorage key "${key}":`, error);
        }
    }, [key]);

    return {
        value: storedValue,
        setValue,
        removeValue,
    };
}
