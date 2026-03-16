import {useEffect, useRef, useState} from 'react';
import {logger} from '@/utils/logger';

interface SearchWithSuggestionsProps {
    value: string;
    onChange: (value: string) => void;
    onClear: () => void;
    placeholder?: string;
}

interface Suggestion {
    text: string;
    type: 'name' | 'description';
}

export default function SearchWithSuggestions({
                                                  value,
                                                  onChange,
                                                  onClear,
                                                  placeholder = 'Search services...',
                                              }: SearchWithSuggestionsProps) {
    const [suggestions, setSuggestions] = useState<Suggestion[]>([]);
    const [showSuggestions, setShowSuggestions] = useState(false);
    const [selectedIndex, setSelectedIndex] = useState(-1);
    const wrapperRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        const abortController = new AbortController();

        if (value.length >= 2) {
            const timeoutId = setTimeout(async () => {
                try {
                    const response = await fetch(
                            `/api/v1/registry/search/suggestions?q=${encodeURIComponent(value)}`,
                            {signal: abortController.signal}
                    );
                    if (response.ok) {
                        const data = await response.json();
                        setSuggestions(
                                data.map((s: string) => ({text: s, type: 'name' as const}))
                        );
                        setShowSuggestions(true);
                    }
                } catch (error) {
                    if (error instanceof Error && error.name !== 'AbortError') {
                        logger.error('Failed to fetch suggestions:', error);
                    }
                }
            }, 200);

            return () => {
                clearTimeout(timeoutId);
                abortController.abort();
            };
        } else {
            setSuggestions([]);
            setShowSuggestions(false);
        }
    }, [value]);

    useEffect(() => {
        function handleClickOutside(event: MouseEvent) {
            if (wrapperRef.current && !wrapperRef.current.contains(event.target as Node)) {
                setShowSuggestions(false);
            }
        }

        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
        // Prevent browser's find-in-page (Ctrl+F / Cmd+F / F3)
        if ((e.ctrlKey || e.metaKey) && e.key === 'f') {
            e.preventDefault();
            e.stopPropagation();
            return;
        }
        if (e.key === 'F3') {
            e.preventDefault();
            e.stopPropagation();
            return;
        }

        if (e.key === 'ArrowDown') {
            e.preventDefault();
            setSelectedIndex((prev) => (prev < suggestions.length - 1 ? prev + 1 : prev));
        } else if (e.key === 'ArrowUp') {
            e.preventDefault();
            setSelectedIndex((prev) => (prev > 0 ? prev - 1 : prev));
        } else if (e.key === 'Enter' && selectedIndex >= 0) {
            e.preventDefault();
            onChange(suggestions[selectedIndex].text);
            setShowSuggestions(false);
        } else if (e.key === 'Escape') {
            setShowSuggestions(false);
        }
    };

    const handleSelectSuggestion = (suggestion: string) => {
        onChange(suggestion);
        setShowSuggestions(false);
    };

    return (
            <div ref={wrapperRef} className="relative">
                <div className="relative">
                    <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                        <svg
                                className="h-5 w-5 text-gray-400"
                                fill="none"
                                stroke="currentColor"
                                viewBox="0 0 24 24"
                        >
                            <path
                                    strokeLinecap="round"
                                    strokeLinejoin="round"
                                    strokeWidth={2}
                                    d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
                            />
                        </svg>
                    </div>
                    <input
                            type="text"
                            value={value}
                            onChange={(e) => onChange(e.target.value)}
                            onKeyDown={handleKeyDown}
                            onFocus={() => value.length >= 2 && setShowSuggestions(true)}
                            placeholder={placeholder}
                            className="w-full pl-10 pr-10 py-2 border border-gray-300 rounded-lg shadow-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
                            // Prevent browser's find-in-page when typing (Ctrl+F, Cmd+F, F3)
                            onKeyDownCapture={(e) => {
                                if ((e.ctrlKey || e.metaKey) && e.key === 'f') {
                                    e.preventDefault();
                                    e.stopPropagation();
                                }
                                if (e.key === 'F3') {
                                    e.preventDefault();
                                    e.stopPropagation();
                                }
                            }}
                    />
                    {value && (
                            <button
                                    onClick={onClear}
                                    className="absolute inset-y-0 right-0 pr-3 flex items-center text-gray-400 hover:text-gray-600"
                            >
                                <svg
                                        className="h-5 w-5"
                                        fill="none"
                                        stroke="currentColor"
                                        viewBox="0 0 24 24"
                                >
                                    <path
                                            strokeLinecap="round"
                                            strokeLinejoin="round"
                                            strokeWidth={2}
                                            d="M6 18L18 6M6 6l12 12"
                                    />
                                </svg>
                            </button>
                    )}
                </div>

                {showSuggestions && suggestions.length > 0 && (
                        <div className="absolute z-50 w-full mt-1 bg-white border border-gray-200 rounded-lg shadow-lg max-h-60 overflow-auto">
                            {suggestions.map((suggestion, index) => (
                                    <button
                                            key={suggestion.text}
                                            onClick={() => handleSelectSuggestion(suggestion.text)}
                                            className={`w-full px-4 py-2 text-left flex items-center justify-between hover:bg-gray-50 ${
                                                    index === selectedIndex ? 'bg-blue-50' : ''
                                            }`}
                                    >
                                        <div className="flex items-center">
                                            <svg
                                                    className="h-4 w-4 text-gray-400 mr-2"
                                                    fill="none"
                                                    stroke="currentColor"
                                                    viewBox="0 0 24 24"
                                            >
                                                <path
                                                        strokeLinecap="round"
                                                        strokeLinejoin="round"
                                                        strokeWidth={2}
                                                        d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
                                                />
                                            </svg>
                                            <span className="text-sm text-gray-900">{suggestion.text}</span>
                                        </div>
                                        <span className="text-xs text-gray-500">
                                {suggestion.type === 'name' ? 'Name' : 'Description'}
                            </span>
                                    </button>
                            ))}
                            <div className="px-4 py-2 bg-gray-50 border-t border-gray-200">
                                <p className="text-xs text-gray-500">
                                    Press <kbd className="px-1 py-0.5 bg-gray-200 rounded">↑</kbd>{' '}
                                    <kbd className="px-1 py-0.5 bg-gray-200 rounded">↓</kbd> to navigate,{' '}
                                    <kbd className="px-1 py-0.5 bg-gray-200 rounded">Enter</kbd> to select
                                </p>
                            </div>
                        </div>
                )}
            </div>
    );
}
