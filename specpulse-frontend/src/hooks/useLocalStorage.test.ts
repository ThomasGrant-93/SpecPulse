import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it} from 'vitest';
import {useLocalStorage} from './useLocalStorage';

describe('useLocalStorage', () => {
    beforeEach(() => {
        localStorage.clear();
    });

    it('should return initial value when no stored value exists', () => {
        const {result} = renderHook(() =>
            useLocalStorage('test-key', 'default-value')
        );

        expect(result.current.value).toBe('default-value');
    });

    it('should return stored value on initial load', () => {
        localStorage.setItem('test-key', JSON.stringify('stored-value'));

        const {result} = renderHook(() =>
            useLocalStorage('test-key', 'default-value')
        );

        expect(result.current.value).toBe('stored-value');
    });

    it('should update value and localStorage when setValue is called', () => {
        const {result} = renderHook(() =>
            useLocalStorage('test-key', 'default-value')
        );

        act(() => {
            result.current.setValue('new-value');
        });

        expect(result.current.value).toBe('new-value');
        expect(localStorage.getItem('test-key')).toBe(JSON.stringify('new-value'));
    });

    it('should parse array values correctly', () => {
        localStorage.setItem('test-array', JSON.stringify(['item1', 'item2']));

        const {result} = renderHook(() =>
            useLocalStorage<string[]>('test-array', [])
        );

        expect(result.current.value).toEqual(['item1', 'item2']);
    });

    it('should parse object values correctly', () => {
        localStorage.setItem('test-object', JSON.stringify({key: 'value'}));

        const {result} = renderHook(() =>
            useLocalStorage<Record<string, string>>('test-object', {})
        );

        expect(result.current.value).toEqual({key: 'value'});
    });

    it('should return default value for invalid JSON in localStorage', () => {
        localStorage.setItem('test-key', 'invalid-json');

        const {result} = renderHook(() =>
            useLocalStorage('test-key', 'default-value')
        );

        expect(result.current.value).toBe('default-value');
    });

    it('should remove value from localStorage when removeValue is called', () => {
        localStorage.setItem('test-key', JSON.stringify('stored-value'));

        const {result} = renderHook(() =>
            useLocalStorage('test-key', 'default-value')
        );

        expect(result.current.value).toBe('stored-value');

        act(() => {
            result.current.removeValue();
        });

        expect(result.current.value).toBeNull();
        expect(localStorage.getItem('test-key')).toBeNull();
    });
});
