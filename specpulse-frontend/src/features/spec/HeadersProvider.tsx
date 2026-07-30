import type { ReactNode } from 'react';
import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { normalizeHeaderName } from '@/utils/headerUtils';

export type HeaderEntry = {
    name: string;
    value: string;
    enabled: boolean;
    // When true, execution must fail if the header is missing/empty.
    required: boolean;
};

type HeadersSessionState = {
    global: Record<string, HeaderEntry>; // headerKey -> entry
    endpoint: Record<string, Record<string, HeaderEntry>>; // endpointId -> headerKey -> entry
    request: Record<string, Record<string, HeaderEntry>>; // endpointId -> headerKey -> entry
};

const SESSION_STORAGE_KEY = 'specpulse:headers:v1';

const defaultState: HeadersSessionState = {
    global: {},
    endpoint: {},
    request: {},
};

type HeadersSessionContextValue = {
    state: HeadersSessionState;
    upsertGlobalHeader: (entry: Omit<HeaderEntry, 'name'> & { name: string }) => void;
    removeGlobalHeader: (headerName: string) => void;
    upsertEndpointHeader: (
        endpointId: string,
        entry: Omit<HeaderEntry, 'name'> & { name: string }
    ) => void;
    removeEndpointHeader: (endpointId: string, headerName: string) => void;
    upsertRequestHeader: (
        endpointId: string,
        entry: Omit<HeaderEntry, 'name'> & { name: string }
    ) => void;
    removeRequestHeader: (endpointId: string, headerName: string) => void;
};

const HeadersSessionContext = createContext<HeadersSessionContextValue | null>(null);

function safeLoadState(): HeadersSessionState {
    try {
        const raw = window.sessionStorage.getItem(SESSION_STORAGE_KEY);
        if (!raw) return defaultState;

        const parsed = JSON.parse(raw) as HeadersSessionState;
        if (!parsed || typeof parsed !== 'object') return defaultState;
        return {
            global: parsed.global && typeof parsed.global === 'object' ? parsed.global : {},
            endpoint: parsed.endpoint && typeof parsed.endpoint === 'object' ? parsed.endpoint : {},
            request: parsed.request && typeof parsed.request === 'object' ? parsed.request : {},
        };
    } catch {
        return defaultState;
    }
}

export function HeadersProvider({ children }: { children: ReactNode }) {
    const [state, setState] = useState<HeadersSessionState>(() => safeLoadState());

    useEffect(() => {
        try {
            window.sessionStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(state));
        } catch {
            // ignore
        }
    }, [state]);

    const upsertGlobalHeader: HeadersSessionContextValue['upsertGlobalHeader'] = (entry) => {
        const key = normalizeHeaderName(entry.name);
        setState((prev) => ({
            ...prev,
            global: {
                ...prev.global,
                [key]: entry,
            },
        }));
    };

    const removeGlobalHeader: HeadersSessionContextValue['removeGlobalHeader'] = (headerName) => {
        const key = normalizeHeaderName(headerName);
        setState((prev) => {
            const { [key]: _ignored, ...rest } = prev.global;
            return { ...prev, global: rest };
        });
    };

    const upsertEndpointHeader: HeadersSessionContextValue['upsertEndpointHeader'] = (
        endpointId,
        entry
    ) => {
        const headerKey = normalizeHeaderName(entry.name);
        setState((prev) => {
            const current = prev.endpoint[endpointId] ?? {};
            return {
                ...prev,
                endpoint: {
                    ...prev.endpoint,
                    [endpointId]: {
                        ...current,
                        [headerKey]: entry,
                    },
                },
            };
        });
    };

    const removeEndpointHeader: HeadersSessionContextValue['removeEndpointHeader'] = (
        endpointId,
        headerName
    ) => {
        const headerKey = normalizeHeaderName(headerName);
        setState((prev) => {
            const current = prev.endpoint[endpointId] ?? {};
            if (!(headerKey in current)) return prev;

            const { [headerKey]: _ignored, ...rest } = current;
            const nextEndpoint = { ...prev.endpoint };
            if (Object.keys(rest).length === 0) {
                delete nextEndpoint[endpointId];
            } else {
                nextEndpoint[endpointId] = rest;
            }
            return {
                ...prev,
                endpoint: {
                    ...nextEndpoint,
                },
            };
        });
    };

    const upsertRequestHeader: HeadersSessionContextValue['upsertRequestHeader'] = (
        endpointId,
        entry
    ) => {
        const headerKey = normalizeHeaderName(entry.name);
        setState((prev) => {
            const current = prev.request[endpointId] ?? {};
            return {
                ...prev,
                request: {
                    ...prev.request,
                    [endpointId]: {
                        ...current,
                        [headerKey]: entry,
                    },
                },
            };
        });
    };

    const removeRequestHeader: HeadersSessionContextValue['removeRequestHeader'] = (
        endpointId,
        headerName
    ) => {
        const headerKey = normalizeHeaderName(headerName);
        setState((prev) => {
            const current = prev.request[endpointId] ?? {};
            if (!(headerKey in current)) return prev;

            const { [headerKey]: _ignored, ...rest } = current;
            const nextRequest = { ...prev.request };
            if (Object.keys(rest).length === 0) {
                delete nextRequest[endpointId];
            } else {
                nextRequest[endpointId] = rest;
            }
            return {
                ...prev,
                request: {
                    ...nextRequest,
                },
            };
        });
    };

    const value = useMemo<HeadersSessionContextValue>(
        () => ({
            state,
            upsertGlobalHeader,
            removeGlobalHeader,
            upsertEndpointHeader,
            removeEndpointHeader,
            upsertRequestHeader,
            removeRequestHeader,
        }),
        [state]
    );

    return (
        <HeadersSessionContext.Provider value={value}>{children}</HeadersSessionContext.Provider>
    );
}

export function useHeadersSession(): HeadersSessionContextValue {
    const ctx = useContext(HeadersSessionContext);
    if (!ctx) {
        throw new Error('useHeadersSession must be used within HeadersProvider');
    }
    return ctx;
}
