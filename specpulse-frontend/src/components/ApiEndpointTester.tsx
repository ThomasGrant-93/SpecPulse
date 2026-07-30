import { useState } from 'react';
import type { AnySpec, ApiEndpoint, AuthCredentials, AuthCredentialsMap } from '@/types/openapi';
import { apiTester, type TestResponse } from '@/services/apiTester';
import { useHeadersSession } from '@/features/spec/HeadersProvider';
import {
    formatHeaderValueForSend,
    isSensitiveHeaderName,
    isValidHeaderName,
    normalizeHeaderName,
    // maskSensitiveValue is intentionally not used: inputs use password type for masking.
} from '@/utils/headerUtils';

interface ApiEndpointTesterProps {
    endpoint: ApiEndpoint;
    baseUrl: string;
    spec: AnySpec;
    authCredentials: AuthCredentialsMap;
    canExecute: boolean;
}

function base64Utf8(input: string): string {
    return btoa(unescape(encodeURIComponent(input)));
}

function getSchemeKeyNames(securityRequirement: Record<string, unknown>): string[] {
    return Object.keys(securityRequirement || {});
}

function isCredentialSetForScheme(schemeDef: any, cred: AuthCredentials | undefined): boolean {
    if (!schemeDef || !cred) return false;

    const type = schemeDef.type;
    if (type === 'apiKey') {
        return typeof cred.value === 'string' && cred.value.trim() !== '';
    }

    if (type === 'http') {
        const scheme = schemeDef.scheme;
        if (scheme === 'basic') {
            return (
                typeof cred.username === 'string' &&
                cred.username.trim() !== '' &&
                typeof cred.password === 'string' &&
                cred.password.trim() !== ''
            );
        }
        // bearer and other http schemes
        const token = cred.token ?? cred.value;
        return typeof token === 'string' && token.trim() !== '';
    }

    if (type === 'basic') {
        return (
            typeof cred.username === 'string' &&
            cred.username.trim() !== '' &&
            typeof cred.password === 'string' &&
            cred.password.trim() !== ''
        );
    }

    if (type === 'oauth2' || type === 'openIdConnect') {
        const token = cred.token ?? cred.value;
        return typeof token === 'string' && token.trim() !== '';
    }

    // Fallback: treat as bearer token.
    const token = cred.token ?? cred.value;
    return typeof token === 'string' && token.trim() !== '';
}

function applySecurityToRequest(params: {
    spec: AnySpec;
    endpointSecurity: ApiEndpoint['security'] | undefined;
    authCredentials: AuthCredentialsMap;
}): { headers: Record<string, string>; queryParams: Record<string, string> } {
    const { spec, endpointSecurity, authCredentials } = params;
    const isSwagger2 = typeof (spec as any).swagger === 'string';

    const securitySchemes: Record<string, any> = isSwagger2
        ? ((spec as any).securityDefinitions as Record<string, any> | undefined) || {}
        : ((spec as any).components?.securitySchemes as Record<string, any> | undefined) || {};

    const globalSecurity = (spec as any).security as any[] | undefined;
    // Per spec semantics: operation.security = [] explicitly disables security requirements.
    // So if endpointSecurity is provided (even empty), it must override global.
    const requirements =
        endpointSecurity !== undefined ? (endpointSecurity as any[]) : globalSecurity;

    if (!requirements || requirements.length === 0) {
        return { headers: {}, queryParams: {} };
    }

    // Each object in `requirements` is an OR branch.
    let chosenRequirement: Record<string, unknown> | null = null;
    for (const req of requirements) {
        const keys = getSchemeKeyNames(req as any);
        if (keys.length === 0) continue;
        const satisfiable = keys.every((schemeName) => {
            const schemeDef = securitySchemes[schemeName];
            const cred = authCredentials[schemeName];
            return isCredentialSetForScheme(schemeDef, cred);
        });
        if (satisfiable) {
            chosenRequirement = req as Record<string, unknown>;
            break;
        }
    }
    if (!chosenRequirement) {
        // Best-effort fallback: pick the first OR branch and only apply schemes we have credentials for.
        chosenRequirement = requirements[0] as Record<string, unknown>;
    }

    const headers: Record<string, string> = {};
    const queryParams: Record<string, string> = {};

    const schemeNames = getSchemeKeyNames(chosenRequirement);
    for (const schemeName of schemeNames) {
        const schemeDef = securitySchemes[schemeName];
        const cred = authCredentials[schemeName];
        if (!schemeDef || !cred) continue;

        const type = schemeDef.type;
        if (type === 'apiKey') {
            const location = schemeDef.in;
            const paramName = schemeDef.name;
            const value = cred.value;
            if (typeof value !== 'string' || value.trim() === '') continue;
            if (location === 'query') {
                if (typeof paramName === 'string' && paramName.trim() !== '') {
                    queryParams[paramName] = value;
                }
            } else if (location === 'cookie') {
                if (typeof paramName === 'string' && paramName.trim() !== '') {
                    headers['Cookie'] = `${paramName}=${value}`;
                }
            } else {
                // header or unknown
                if (typeof paramName === 'string' && paramName.trim() !== '') {
                    headers[paramName] = value;
                }
            }
            continue;
        }

        if (type === 'http') {
            const httpScheme = schemeDef.scheme;
            if (httpScheme === 'basic') {
                if (
                    typeof cred.username !== 'string' ||
                    cred.username.trim() === '' ||
                    typeof cred.password !== 'string'
                ) {
                    continue;
                }
                const token = `${cred.username}:${cred.password}`;
                headers['Authorization'] = `Basic ${base64Utf8(token)}`;
            } else {
                const token = cred.token ?? cred.value;
                if (typeof token !== 'string' || token.trim() === '') continue;
                headers['Authorization'] = `Bearer ${token}`;
            }
            continue;
        }

        if (type === 'basic') {
            if (
                typeof cred.username !== 'string' ||
                cred.username.trim() === '' ||
                typeof cred.password !== 'string'
            ) {
                continue;
            }
            const token = `${cred.username}:${cred.password}`;
            headers['Authorization'] = `Basic ${base64Utf8(token)}`;
            continue;
        }

        if (type === 'oauth2' || type === 'openIdConnect') {
            const token = cred.token ?? cred.value;
            if (typeof token !== 'string' || token.trim() === '') continue;
            headers['Authorization'] = `Bearer ${token}`;
            continue;
        }

        // Fallback
        const token = cred.token ?? cred.value;
        if (typeof token === 'string' && token.trim() !== '') {
            headers['Authorization'] = `Bearer ${token}`;
        }
    }

    return { headers, queryParams };
}

const methodColors: Record<string, string> = {
    get: 'bg-blue-600 hover:bg-blue-700',
    post: 'bg-green-600 hover:bg-green-700',
    put: 'bg-yellow-600 hover:bg-yellow-700',
    delete: 'bg-red-600 hover:bg-red-700',
    patch: 'bg-orange-600 hover:bg-orange-700',
};

export default function ApiEndpointTester({
    endpoint,
    baseUrl,
    spec,
    authCredentials,
    canExecute,
}: ApiEndpointTesterProps) {
    const [pathParams, setPathParams] = useState<Record<string, string>>({});
    const [queryParams, setQueryParams] = useState<Record<string, string>>({});
    const [requestBody, setRequestBody] = useState<string>('');
    const [headerError, setHeaderError] = useState<string | null>(null);

    const {
        state: headersState,
        upsertEndpointHeader,
        removeEndpointHeader,
        upsertRequestHeader,
        removeRequestHeader,
    } = useHeadersSession();

    const endpointId = `${endpoint.method}-${endpoint.path}`;
    const endpointHeaderEntries = headersState.endpoint[endpointId] ?? {};
    const requestHeaderEntries = headersState.request[endpointId] ?? {};

    const [newCustomHeaderName, setNewCustomHeaderName] = useState<string>('');
    const [newCustomHeaderValue, setNewCustomHeaderValue] = useState<string>('');
    const [newCustomHeaderEnabled, setNewCustomHeaderEnabled] = useState<boolean>(true);
    const [newCustomHeaderRequired, setNewCustomHeaderRequired] = useState<boolean>(false);
    const [response, setResponse] = useState<TestResponse | null>(null);
    const [isLoading, setIsLoading] = useState(false);
    const [expanded, setExpanded] = useState(false);

    // Extract path parameters from the URL
    const pathParamNames = endpoint.path.match(/\{([^}]+)\}/g)?.map((s) => s.slice(1, -1)) || [];

    // Separate parameters by location
    const pathParameters = endpoint.parameters?.filter((p) => p.in === 'path') || [];
    const queryParameters = endpoint.parameters?.filter((p) => p.in === 'query') || [];
    const headerParameters = endpoint.parameters?.filter((p) => p.in === 'header') || [];

    const getHeaderParamExampleValue = (p: any): string => {
        const raw = p?.example ?? p?.schema?.example;
        if (typeof raw === 'string') return raw;
        if (typeof raw === 'number' || typeof raw === 'boolean') return String(raw);
        return '';
    };

    const getEndpointHeaderStoredEntry = (p: any) => {
        const key = normalizeHeaderName(String(p.name));
        return endpointHeaderEntries[key];
    };

    const getEndpointHeaderEnabled = (p: any): boolean => {
        const required = p.required === true;
        if (required) return true;
        const stored = getEndpointHeaderStoredEntry(p);
        return stored?.enabled ?? false;
    };

    const getEndpointHeaderValue = (p: any): string => {
        const stored = getEndpointHeaderStoredEntry(p);
        if (stored) return stored.value;
        return getHeaderParamExampleValue(p);
    };

    const handleExecute = async () => {
        if (!canExecute) {
            setHeaderError('Execution is not allowed for your role');
            return;
        }
        setIsLoading(true);
        setResponse(null);
        setHeaderError(null);

        try {
            // Build the full URL with path parameters.
            // Normalize joining to avoid missing/double slashes.
            const normalizedBase = baseUrl.replace(/\/$/, '');
            const normalizedPath = endpoint.path.startsWith('/') ? endpoint.path : `/${endpoint.path}`;
            let fullUrl = `${normalizedBase}${normalizedPath}`;
            pathParamNames.forEach((paramName) => {
                fullUrl = fullUrl.replace(`{${paramName}}`, pathParams[paramName] || `{${paramName}}`);
            });

            const { headers: securityHeaders, queryParams: securityQueryParams } = applySecurityToRequest({
                spec,
                endpointSecurity: endpoint.security,
                authCredentials,
            });

            const effectiveHeadersByKey: Record<string, { name: string; value: string }> = {};

            const setHeader = (name: string, rawValue: string) => {
                if (!isValidHeaderName(name)) return;
                const trimmed = rawValue.trim();
                if (!trimmed) return;
                const key = normalizeHeaderName(name);
                effectiveHeadersByKey[key] = {
                    name,
                    value: formatHeaderValueForSend(name, trimmed),
                };
            };

            // 1) Global headers
            Object.values(headersState.global).forEach((h) => {
                if (h.enabled) {
                    setHeader(h.name, h.value);
                }
            });

            // 2) Security headers from configured auth schemes
            Object.entries(securityHeaders).forEach(([name, value]) => {
                setHeader(name, String(value));
            });

            // 3) Endpoint header parameters (spec-driven)
            for (const p of headerParameters) {
                const key = normalizeHeaderName(String(p.name));
                const required = p.required === true;

                if (required) {
                    const value = getEndpointHeaderValue(p);
                    if (value.trim()) {
                        setHeader(String(p.name), value);
                    }
                    continue;
                }

                const enabled = getEndpointHeaderEnabled(p);
                if (!enabled) {
                    delete effectiveHeadersByKey[key];
                    continue;
                }

                const value = getEndpointHeaderValue(p);
                if (value.trim()) {
                    setHeader(String(p.name), value);
                }
            }

            // 4) Request-specific custom headers
            Object.values(requestHeaderEntries).forEach((h) => {
                const key = normalizeHeaderName(h.name);
                if (!h.enabled) {
                    delete effectiveHeadersByKey[key];
                    return;
                }

                if (h.value.trim()) {
                    setHeader(h.name, h.value);
                } else {
                    // Enabled but empty -> override by omission.
                    delete effectiveHeadersByKey[key];
                }
            });

            // Ensure Content-Type exists unless overridden.
            const contentTypeKey = normalizeHeaderName('Content-Type');
            if (!effectiveHeadersByKey[contentTypeKey]) {
                setHeader('Content-Type', 'application/json');
            }

            // Validate required endpoint headers & required custom headers.
            const missingRequired = new Map<string, string>(); // key -> display name
            for (const p of headerParameters) {
                if (p.required === true) {
                    const key = normalizeHeaderName(String(p.name));
                    if (!effectiveHeadersByKey[key]) {
                        missingRequired.set(key, String(p.name));
                    }
                }
            }
            for (const h of Object.values(requestHeaderEntries)) {
                if (h.required) {
                    const key = normalizeHeaderName(h.name);
                    if (!effectiveHeadersByKey[key]) {
                        missingRequired.set(key, h.name);
                    }
                }
            }

            // Validate required auth headers (header-based schemes only).
            const effectiveHeaderKeys = new Set(Object.keys(effectiveHeadersByKey));
            const missingAuthHeaders: string[] = [];

            const isSwagger2 = typeof (spec as any).swagger === 'string';
            const securitySchemes: Record<string, any> = isSwagger2
                ? (((spec as any).securityDefinitions as Record<string, any> | undefined) ?? {})
                : (((spec as any).components?.securitySchemes as Record<string, any> | undefined) ?? {});

            const globalSecurity = (spec as any).security as any[] | undefined;
            const requirements = endpoint.security !== undefined ? (endpoint.security as any[]) : globalSecurity;

            const expectedHeadersForSchemeDef = (schemeDef: any): string[] => {
                if (!schemeDef || typeof schemeDef !== 'object') return [];
                const type = schemeDef.type;
                if (type === 'apiKey') {
                    if (schemeDef.in === 'header' && typeof schemeDef.name === 'string') {
                        return [schemeDef.name];
                    }
                    return [];
                }
                if (type === 'http') {
                    return ['Authorization'];
                }
                if (type === 'basic') {
                    return ['Authorization'];
                }
                if (type === 'oauth2' || type === 'openIdConnect') {
                    return ['Authorization'];
                }
                return [];
            };

            if (requirements && requirements.length > 0) {
                let authSatisfied = false;
                const missingAuthByKey = new Map<string, string>();

                for (const req of requirements) {
                    if (!req || typeof req !== 'object') continue;
                    const schemeNames = Object.keys(req as any);
                    if (schemeNames.length === 0) continue;

                    const missingInBranch = new Set<string>();
                    let branchHasHeaderAuthRequirement = false;

                    for (const schemeName of schemeNames) {
                        const schemeDef = securitySchemes[schemeName];
                        const expectedHeaders = expectedHeadersForSchemeDef(schemeDef);
                        if (expectedHeaders.length === 0) continue;
                        branchHasHeaderAuthRequirement = true;

                        for (const headerName of expectedHeaders) {
                            const key = normalizeHeaderName(headerName);
                            if (!effectiveHeaderKeys.has(key)) {
                                missingInBranch.add(key);
                                missingAuthByKey.set(key, headerName);
                            }
                        }
                    }

                    if (!branchHasHeaderAuthRequirement) {
                        // No header-based auth in this branch -> it is satisfied from a header perspective.
                        authSatisfied = true;
                        break;
                    }

                    if (missingInBranch.size === 0) {
                        authSatisfied = true;
                        break;
                    }
                }

                if (!authSatisfied) {
                    missingAuthHeaders.push(...Array.from(missingAuthByKey.values()));
                }
            }

            if (missingRequired.size > 0 || missingAuthHeaders.length > 0) {
                const parts: string[] = [];
                if (missingRequired.size > 0) {
                    parts.push(`Missing required headers: ${Array.from(missingRequired.values()).join(', ')}`);
                }
                if (missingAuthHeaders.length > 0) {
                    parts.push(
                        `Missing required authentication headers: ${missingAuthHeaders.join(', ')}`
                    );
                }
                setHeaderError(parts.join(' · '));
                setIsLoading(false);
                return;
            }

            const headers: Record<string, string> = {};
            for (const h of Object.values(effectiveHeadersByKey)) {
                headers[h.name] = h.value;
            }

            // Parse request body
            let body = undefined;
            if (requestBody && ['post', 'put', 'patch'].includes(endpoint.method)) {
                try {
                    body = JSON.parse(requestBody);
                } catch {
                    body = requestBody;
                }
            }

            const result = await apiTester.execute({
                url: fullUrl,
                method: endpoint.method,
                parameters: { ...securityQueryParams, ...queryParams },
                headers,
                body,
            });

            setResponse(result);
            setExpanded(true);
        } catch (e: any) {
            setHeaderError(e?.message ? String(e.message) : 'Request failed');
        } finally {
            setIsLoading(false);
        }
    };

    const updatePathParam = (name: string, value: string) => {
        setPathParams((prev) => ({ ...prev, [name]: value }));
    };

    const updateQueryParam = (name: string, value: string) => {
        setQueryParams((prev) => ({ ...prev, [name]: value }));
    };

    return (
        <div className="border rounded-lg mt-4 overflow-hidden">
            {/* Try It Out Button */}
            <div className="bg-gray-50 px-4 py-3 flex items-center justify-between">
                <span className="text-sm font-medium text-gray-700">Try it out</span>
                <button
                    onClick={handleExecute}
                    disabled={isLoading || !canExecute}
                    className={`px-4 py-2 rounded text-white text-sm font-medium transition-colors ${
                        methodColors[endpoint.method] || 'bg-blue-600'
                    } ${isLoading ? 'opacity-50 cursor-not-allowed' : ''}`}
                >
                    {isLoading ? 'Executing...' : 'Execute'}
                </button>
            </div>

            {/* Parameters */}
            <div className="p-4 space-y-4">
                {headerError && (
                    <div
                        className="rounded-md bg-red-50 text-red-800 px-3 py-2 text-sm"
                        role="alert"
                    >
                        {headerError}
                    </div>
                )}
                {/* Path Parameters */}
                {pathParameters.length > 0 && (
                    <div>
                        <h4 className="text-sm font-semibold text-gray-900 mb-2">
                            Path Parameters
                        </h4>
                        <div className="space-y-2">
                            {pathParameters.map((param) => (
                                <div key={param.name} className="flex items-center gap-2">
                                    <label className="text-sm text-gray-700 w-32 font-mono">
                                        {param.name}
                                    </label>
                                    <input
                                        type="text"
                                        value={pathParams[param.name] || ''}
                                        onChange={(e) =>
                                            updatePathParam(param.name, e.target.value)
                                        }
                                        placeholder={
                                            typeof param.schema?.example === 'string'
                                                ? param.schema.example
                                                : ''
                                        }
                                        className="flex-1 rounded border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                                    />
                                    {param.required && (
                                        <span className="text-red-500 text-xs">*</span>
                                    )}
                                </div>
                            ))}
                        </div>
                    </div>
                )}

                {/* Query Parameters */}
                {queryParameters.length > 0 && (
                    <div>
                        <h4 className="text-sm font-semibold text-gray-900 mb-2">
                            Query Parameters
                        </h4>
                        <div className="space-y-2">
                            {queryParameters.map((param) => (
                                <div key={param.name} className="flex items-center gap-2">
                                    <label className="text-sm text-gray-700 w-32 font-mono">
                                        {param.name}
                                    </label>
                                    <input
                                        type="text"
                                        value={queryParams[param.name] || ''}
                                        onChange={(e) =>
                                            updateQueryParam(param.name, e.target.value)
                                        }
                                        placeholder={
                                            typeof param.schema?.example === 'string'
                                                ? param.schema.example
                                                : ''
                                        }
                                        className="flex-1 rounded border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                                    />
                                    {param.required && (
                                        <span className="text-red-500 text-xs">*</span>
                                    )}
                                </div>
                            ))}
                        </div>
                    </div>
                )}

                {/* Endpoint Header Parameters */}
                {headerParameters.length > 0 && (
                    <div>
                        <h4 className="text-sm font-semibold text-gray-900 mb-2">Header Parameters</h4>
                        <div className="space-y-2">
                            {headerParameters.map((param) => {
                                const name = String(param.name);
                                const required = param.required === true;
                                const enabled = getEndpointHeaderEnabled(param);
                                const value = getEndpointHeaderValue(param);
                                const sensitive = isSensitiveHeaderName(name);

                                return (
                                    <div key={name} className="flex items-center gap-2">
                                        <label className="text-sm text-gray-700 w-32 font-mono flex items-center gap-1">
                                            <span>{name}</span>
                                            {required && <span className="text-red-500 text-xs">*</span>}
                                        </label>

                                        {!required && (
                                            <input
                                                aria-label={`Enable ${name}`}
                                                type="checkbox"
                                                checked={enabled}
                                                onChange={(e) => {
                                                    const nextEnabled = e.target.checked;
                                                    if (nextEnabled) {
                                                        upsertEndpointHeader(endpointId, {
                                                            name,
                                                            value,
                                                            enabled: true,
                                                            required: false,
                                                        });
                                                    } else {
                                                        removeEndpointHeader(endpointId, name);
                                                    }
                                                }}
                                            />
                                        )}

                                        <input
                                            type={sensitive ? 'password' : 'text'}
                                            value={value}
                                            onChange={(e) => {
                                                const nextValue = e.target.value;
                                                const nextEnabled = required ? true : enabled;
                                                upsertEndpointHeader(endpointId, {
                                                    name,
                                                    value: nextValue,
                                                    enabled: nextEnabled,
                                                    required,
                                                });
                                            }}
                                            className="flex-1 rounded border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                                        />
                                    </div>
                                );
                            })}
                        </div>
                    </div>
                )}

                {/* Custom Headers */}
                <div>
                    <h4 className="text-sm font-semibold text-gray-900 mb-2">Custom Headers</h4>

                    {Object.values(requestHeaderEntries).length > 0 && (
                        <div className="space-y-2 mb-3">
                            {Object.values(requestHeaderEntries)
                                .slice()
                                .sort((a, b) => a.name.localeCompare(b.name))
                                .map((h) => {
                                    const sensitive = isSensitiveHeaderName(h.name);
                                    return (
                                        <div key={normalizeHeaderName(h.name)} className="flex items-center gap-2">
                                            <label className="text-sm text-gray-700 w-32 font-mono flex items-center gap-1">
                                                <span>{h.name}</span>
                                                {h.required && <span className="text-red-500 text-xs">*</span>}
                                            </label>

                                            <input
                                                aria-label={`Enable ${h.name}`}
                                                type="checkbox"
                                                checked={h.enabled}
                                                disabled={h.required}
                                                onChange={(e) => {
                                                    upsertRequestHeader(endpointId, {
                                                        name: h.name,
                                                        value: h.value,
                                                        enabled: e.target.checked,
                                                        required: h.required,
                                                    });
                                                }}
                                            />

                                            <input
                                                type={sensitive ? 'password' : 'text'}
                                                value={h.value}
                                                onChange={(e) => {
                                                    upsertRequestHeader(endpointId, {
                                                        name: h.name,
                                                        value: e.target.value,
                                                        enabled: h.enabled,
                                                        required: h.required,
                                                    });
                                                }}
                                                className="flex-1 rounded border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                                            />

                                            {!h.required && (
                                                <button
                                                    type="button"
                                                    onClick={() => removeRequestHeader(endpointId, h.name)}
                                                    className="text-xs text-red-600 hover:underline"
                                                >
                                                    Remove
                                                </button>
                                            )}
                                        </div>
                                    );
                                })}
                        </div>
                    )}

                    <div className="rounded-md border border-gray-200 p-3 space-y-3">
                        <div className="flex items-center gap-2">
                            <input
                                type="text"
                                value={newCustomHeaderName}
                                onChange={(e) => setNewCustomHeaderName(e.target.value)}
                                placeholder="Header name"
                                className="w-40 rounded border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm font-mono"
                            />
                            <input
                                type={newCustomHeaderName && isSensitiveHeaderName(newCustomHeaderName) ? 'password' : 'text'}
                                value={newCustomHeaderValue}
                                onChange={(e) => setNewCustomHeaderValue(e.target.value)}
                                placeholder="Value"
                                className="flex-1 rounded border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm font-mono"
                            />
                            <button
                                type="button"
                                onClick={() => {
                                    const name = newCustomHeaderName.trim();
                                    if (!name) {
                                        setHeaderError('Header name is required');
                                        return;
                                    }

                                    if (!isValidHeaderName(name)) {
                                        setHeaderError(`Invalid header name: ${name}`);
                                        return;
                                    }

                                    const enabled = newCustomHeaderRequired ? true : newCustomHeaderEnabled;
                                    upsertRequestHeader(endpointId, {
                                        name,
                                        value: newCustomHeaderValue,
                                        enabled,
                                        required: newCustomHeaderRequired,
                                    });

                                    setNewCustomHeaderName('');
                                    setNewCustomHeaderValue('');
                                    setNewCustomHeaderEnabled(true);
                                    setNewCustomHeaderRequired(false);
                                }}
                                className="px-3 py-1 rounded bg-blue-600 text-white text-sm font-medium hover:bg-blue-500"
                            >
                                Add
                            </button>
                        </div>

                        <div className="flex items-center gap-4">
                            <label className="flex items-center gap-2 text-sm text-gray-700">
                                <input
                                    type="checkbox"
                                    checked={newCustomHeaderEnabled}
                                    disabled={newCustomHeaderRequired}
                                    onChange={(e) => setNewCustomHeaderEnabled(e.target.checked)}
                                />
                                Enabled
                            </label>

                            <label className="flex items-center gap-2 text-sm text-gray-700">
                                <input
                                    type="checkbox"
                                    checked={newCustomHeaderRequired}
                                    onChange={(e) => {
                                        const next = e.target.checked;
                                        setNewCustomHeaderRequired(next);
                                        if (next) setNewCustomHeaderEnabled(true);
                                    }}
                                />
                                Required
                            </label>
                        </div>
                    </div>
                </div>

                {/* Request Body */}
                {endpoint.requestBody && (
                    <div>
                        <h4 className="text-sm font-semibold text-gray-900 mb-2">Request Body</h4>
                        <textarea
                            value={requestBody}
                            onChange={(e) => setRequestBody(e.target.value)}
                            rows={8}
                            className="w-full rounded border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm font-mono"
                            placeholder={JSON.stringify({ example: 'value' }, null, 2)}
                        />
                    </div>
                )}
            </div>

            {/* Response */}
            {response && (
                <div className="border-t">
                    <button
                        onClick={() => setExpanded(!expanded)}
                        className="w-full px-4 py-3 text-left flex items-center justify-between bg-gray-50 hover:bg-gray-100"
                    >
                        <div className="flex items-center gap-3">
                            <span className="text-sm font-medium text-gray-700">Response</span>
                            <span
                                className={`px-2 py-0.5 rounded text-xs font-semibold ${
                                    response.status >= 200 && response.status < 300
                                        ? 'bg-green-100 text-green-800'
                                        : response.status >= 400
                                          ? 'bg-red-100 text-red-800'
                                          : 'bg-yellow-100 text-yellow-800'
                                }`}
                            >
                                {response.status} {response.statusText}
                            </span>
                            <span className="text-xs text-gray-500">{response.duration}ms</span>
                        </div>
                        <svg
                            className={`w-5 h-5 text-gray-400 transition-transform ${
                                expanded ? 'rotate-180' : ''
                            }`}
                            fill="none"
                            stroke="currentColor"
                            viewBox="0 0 24 24"
                        >
                            <path
                                strokeLinecap="round"
                                strokeLinejoin="round"
                                strokeWidth={2}
                                d="M19 9l-7 7-7-7"
                            />
                        </svg>
                    </button>

                    {expanded && (
                        <div className="p-4 space-y-4">
                            {/* Response Body */}
                            <div>
                                <h5 className="text-xs font-semibold text-gray-700 mb-2">Body</h5>
                                <pre className="bg-gray-900 text-gray-100 rounded p-3 overflow-x-auto text-sm max-h-96 overflow-y-auto">
                                    {JSON.stringify(response.body, null, 2)}
                                </pre>
                            </div>

                            {/* Response Headers */}
                            <div>
                                <h5 className="text-xs font-semibold text-gray-700 mb-2">
                                    Headers
                                </h5>
                                <div className="bg-gray-50 rounded p-3 overflow-x-auto">
                                    <table className="min-w-full text-sm">
                                        <tbody>
                                            {Object.entries(response.headers).map(
                                                ([key, value]) => (
                                                    <tr key={key}>
                                                        <td className="py-1 font-mono text-gray-700 pr-4">
                                                            {key}
                                                        </td>
                                                        <td className="py-1 text-gray-600">
                                                            {value}
                                                        </td>
                                                    </tr>
                                                )
                                            )}
                                        </tbody>
                                    </table>
                                </div>
                            </div>
                        </div>
                    )}
                </div>
            )}
        </div>
    );
}
