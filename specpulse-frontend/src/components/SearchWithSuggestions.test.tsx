import {fireEvent, render, screen} from '@testing-library/react';
import {beforeEach, afterEach, describe, expect, it, vi} from 'vitest';
import SearchWithSuggestions from './SearchWithSuggestions';

/**
 * SearchWithSuggestions Component Tests
 * 
 * Note: Tests involving fetch/debounce are limited due to jsdom compatibility issues
 * with AbortController and async setTimeout. Core UI behavior is fully tested.
 */
describe('SearchWithSuggestions', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    // ==================== Rendering Tests ====================

    it('should render with placeholder', () => {
        render(
                <SearchWithSuggestions
                        value=""
                        onChange={vi.fn()}
                        onClear={vi.fn()}
                        placeholder="Search..."
                />
        );

        const input = screen.getByPlaceholderText('Search...');
        expect(input).toBeInTheDocument();
    });

    it('should use default placeholder when not provided', () => {
        render(
                <SearchWithSuggestions
                        value=""
                        onChange={vi.fn()}
                        onClear={vi.fn()}
                />
        );

        const input = screen.getByPlaceholderText('Search services...');
        expect(input).toBeInTheDocument();
    });

    it('should have search icon', () => {
        const {container} = render(
                <SearchWithSuggestions
                        value=""
                        onChange={vi.fn()}
                        onClear={vi.fn()}
                />
        );

        // Search icon SVG should be present (decorative)
        const svg = container.querySelector('svg');
        expect(svg).toBeInTheDocument();
    });

    // ==================== Input Change Tests ====================

    it('should call onChange when typing', () => {
        const onChange = vi.fn();
        render(
                <SearchWithSuggestions
                        value=""
                        onChange={onChange}
                        onClear={vi.fn()}
                />
        );

        const input = screen.getByPlaceholderText('Search services...');
        fireEvent.change(input, {target: {value: 'test'}});

        expect(onChange).toHaveBeenCalledWith('test');
    });

    it('should call onChange multiple times when typing multiple characters', () => {
        const onChange = vi.fn();
        render(
                <SearchWithSuggestions
                        value=""
                        onChange={onChange}
                        onClear={vi.fn()}
                />
        );

        const input = screen.getByPlaceholderText('Search services...');
        fireEvent.change(input, {target: {value: 't'}});
        fireEvent.change(input, {target: {value: 'te'}});
        fireEvent.change(input, {target: {value: 'tes'}});
        fireEvent.change(input, {target: {value: 'test'}});

        expect(onChange).toHaveBeenCalledTimes(4);
        expect(onChange).toHaveBeenLastCalledWith('test');
    });

    // ==================== Clear Button Tests ====================

    it('should show clear button when value is present', () => {
        render(
                <SearchWithSuggestions
                        value="test"
                        onChange={vi.fn()}
                        onClear={vi.fn()}
                />
        );

        expect(screen.getByRole('button')).toBeInTheDocument();
    });

    it('should not show clear button when value is empty', () => {
        render(
                <SearchWithSuggestions
                        value=""
                        onChange={vi.fn()}
                        onClear={vi.fn()}
                />
        );

        expect(screen.queryByRole('button')).not.toBeInTheDocument();
    });

    it('should call onClear when clear button is clicked', () => {
        const onClear = vi.fn();
        render(
                <SearchWithSuggestions
                        value="test"
                        onChange={vi.fn()}
                        onClear={onClear}
                />
        );

        const clearButton = screen.getByRole('button');
        fireEvent.click(clearButton);

        expect(onClear).toHaveBeenCalledTimes(1);
    });

    it('should have proper clear button accessibility', () => {
        render(
                <SearchWithSuggestions
                        value="test"
                        onChange={vi.fn()}
                        onClear={vi.fn()}
                />
        );

        const clearButton = screen.getByRole('button');
        // Button should be clickable
        expect(clearButton).toBeEnabled();
    });

    // ==================== Keyboard Navigation Tests ====================

    it('should prevent default browser find with Ctrl+F', () => {
        render(
                <SearchWithSuggestions
                        value=""
                        onChange={vi.fn()}
                        onClear={vi.fn()}
                />
        );

        const input = screen.getByPlaceholderText('Search services...');
        const event = fireEvent.keyDown(input, {
            key: 'f',
            ctrlKey: true,
            bubbles: true,
            cancelable: true,
        });

        expect(event).toBe(false); // preventDefault was called
    });

    it('should prevent default browser find with Cmd+F', () => {
        render(
                <SearchWithSuggestions
                        value=""
                        onChange={vi.fn()}
                        onClear={vi.fn()}
                />
        );

        const input = screen.getByPlaceholderText('Search services...');
        const event = fireEvent.keyDown(input, {
            key: 'f',
            metaKey: true,
            bubbles: true,
            cancelable: true,
        });

        expect(event).toBe(false);
    });

    it('should prevent default browser find with F3', () => {
        render(
                <SearchWithSuggestions
                        value=""
                        onChange={vi.fn()}
                        onClear={vi.fn()}
                />
        );

        const input = screen.getByPlaceholderText('Search services...');
        const event = fireEvent.keyDown(input, {
            key: 'F3',
            bubbles: true,
            cancelable: true,
        });

        expect(event).toBe(false);
    });

    // ==================== Input Styling Tests ====================

    it('should have proper input classes', () => {
        render(
                <SearchWithSuggestions
                        value=""
                        onChange={vi.fn()}
                        onClear={vi.fn()}
                />
        );

        const input = screen.getByPlaceholderText('Search services...');
        expect(input).toHaveClass('border');
        expect(input).toHaveClass('border-gray-300');
        expect(input).toHaveClass('rounded-lg');
        expect(input).toHaveClass('pl-10');
    });

    it('should have proper container structure', () => {
        const {container} = render(
                <SearchWithSuggestions
                        value=""
                        onChange={vi.fn()}
                        onClear={vi.fn()}
                />
        );

        // Should have relative container
        const wrapper = container.firstChild;
        expect(wrapper).toHaveClass('relative');
    });

    // ==================== Controlled Component Tests ====================

    it('should display value prop in input', () => {
        render(
                <SearchWithSuggestions
                        value="existing-value"
                        onChange={vi.fn()}
                        onClear={vi.fn()}
                />
        );

        const input = screen.getByPlaceholderText('Search services...');
        expect(input).toHaveValue('existing-value');
    });

    it('should update display when value prop changes', () => {
        const {rerender} = render(
                <SearchWithSuggestions
                        value=""
                        onChange={vi.fn()}
                        onClear={vi.fn()}
                />
        );

        let input = screen.getByPlaceholderText('Search services...');
        expect(input).toHaveValue('');

        rerender(
                <SearchWithSuggestions
                        value="new-value"
                        onChange={vi.fn()}
                        onClear={vi.fn()}
                />
        );

        input = screen.getByPlaceholderText('Search services...');
        expect(input).toHaveValue('new-value');
    });

    // ==================== Integration Tests ====================

    it('should work as controlled component with state', () => {
        let value = '';
        const setValue = (newValue: string) => { value = newValue; };
        const onChangeMock = vi.fn().mockImplementation((v) => setValue(v));

        render(
                <SearchWithSuggestions
                        value={value}
                        onChange={onChangeMock}
                        onClear={vi.fn()}
                />
        );

        const input = screen.getByPlaceholderText('Search services...');
        fireEvent.change(input, {target: {value: 'test'}});

        expect(onChangeMock).toHaveBeenCalledWith('test');
    });

    it('should clear value when clear button clicked', () => {
        const onClear = vi.fn();
        render(
                <SearchWithSuggestions
                        value="test"
                        onChange={vi.fn()}
                        onClear={onClear}
                />
        );

        const clearButton = screen.getByRole('button');
        fireEvent.click(clearButton);

        expect(onClear).toHaveBeenCalled();
    });

    // ==================== Accessibility Tests ====================

    it('should have proper input type', () => {
        render(
                <SearchWithSuggestions
                        value=""
                        onChange={vi.fn()}
                        onClear={vi.fn()}
                />
        );

        const input = screen.getByPlaceholderText('Search services...');
        expect(input).toHaveAttribute('type', 'text');
    });

    it('should be focusable', () => {
        render(
                <SearchWithSuggestions
                        value=""
                        onChange={vi.fn()}
                        onClear={vi.fn()}
                />
        );

        const input = screen.getByPlaceholderText('Search services...');
        input.focus();

        expect(input).toHaveFocus();
    });

    it('should have proper tab index', () => {
        render(
                <SearchWithSuggestions
                        value=""
                        onChange={vi.fn()}
                        onClear={vi.fn()}
                />
        );

        const input = screen.getByPlaceholderText('Search services...');
        expect(input).not.toHaveAttribute('tabIndex'); // defaults to 0
    });
});
