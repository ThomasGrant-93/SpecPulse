import {act, renderHook} from '@testing-library/react';
import {describe, expect, it} from 'vitest';
import {useModal} from './useModal';

describe('useModal', () => {
    it('should initialize with closed state by default', () => {
        const {result} = renderHook(() => useModal());

        expect(result.current.isOpen).toBe(false);
    });

    it('should initialize with provided initial state', () => {
        const {result} = renderHook(() => useModal(true));

        expect(result.current.isOpen).toBe(true);
    });

    it('should open modal when open() is called', () => {
        const {result} = renderHook(() => useModal(false));

        act(() => {
            result.current.open();
        });

        expect(result.current.isOpen).toBe(true);
    });

    it('should close modal when close() is called', () => {
        const {result} = renderHook(() => useModal(true));

        act(() => {
            result.current.close();
        });

        expect(result.current.isOpen).toBe(false);
    });

    it('should toggle modal state when toggle() is called', () => {
        const {result} = renderHook(() => useModal(false));

        act(() => {
            result.current.toggle();
        });

        expect(result.current.isOpen).toBe(true);

        act(() => {
            result.current.toggle();
        });

        expect(result.current.isOpen).toBe(false);
    });

    it('should have stable function references (memoized)', () => {
        const {result, rerender} = renderHook(() => useModal());

        const firstOpen = result.current.open;
        const firstClose = result.current.close;
        const firstToggle = result.current.toggle;

        rerender();

        expect(result.current.open).toBe(firstOpen);
        expect(result.current.close).toBe(firstClose);
        expect(result.current.toggle).toBe(firstToggle);
    });

    it('should handle multiple open calls without issues', () => {
        const {result} = renderHook(() => useModal(false));

        act(() => {
            result.current.open();
            result.current.open();
            result.current.open();
        });

        expect(result.current.isOpen).toBe(true);
    });

    it('should handle multiple close calls without issues', () => {
        const {result} = renderHook(() => useModal(true));

        act(() => {
            result.current.close();
            result.current.close();
            result.current.close();
        });

        expect(result.current.isOpen).toBe(false);
    });
});
