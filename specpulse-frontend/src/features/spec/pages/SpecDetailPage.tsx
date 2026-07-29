import { useParams, useSearchParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { registryApi, versionsApi } from '@/features/spec/api';
import ApiSpecViewer from '@/components/ApiSpecViewer';
import type { AnySpec, AuthCredentialsMap } from '@/types/openapi';
import { Modal } from '@/components/Modal';
import { useEffect, useMemo, useState } from 'react';

export default function SpecDetailPage() {
    const { id } = useParams<{ id: string }>();
    const [searchParams] = useSearchParams();
    const serviceId = Number(id);
    const versionIdParam = searchParams.get('version');

    const [customBaseUrl, setCustomBaseUrl] = useState<string>('');
    const [showBaseUrlInput, setShowBaseUrlInput] = useState(false);

    const AUTH_STORAGE_KEY = 'specpulse_auth_credentials';
    const [isAuthorizeOpen, setIsAuthorizeOpen] = useState(false);
    const [authCredentials, setAuthCredentials] = useState<AuthCredentialsMap>(() => {
        try {
            const raw = sessionStorage.getItem(AUTH_STORAGE_KEY);
            if (!raw) return {};
            return JSON.parse(raw) as AuthCredentialsMap;
        } catch {
            return {};
        }
    });

    useEffect(() => {
        try {
            sessionStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(authCredentials));
        } catch {
            // ignore
        }
    }, [authCredentials]);

    const { data: service } = useQuery({
        queryKey: ['service', serviceId],
        queryFn: async () => {
            const response = await registryApi.getById(serviceId);
            return response.data;
        },
        enabled: !isNaN(serviceId),
    });

    const { data: versions = [] } = useQuery({
        queryKey: ['versions', serviceId],
        queryFn: async () => {
            const response = await versionsApi.getByService(serviceId);
            return response.data;
        },
        enabled: !isNaN(serviceId),
    });

    const selectedVersionId = versionIdParam ? Number(versionIdParam) : versions[0]?.id;

    const { data: specData, isLoading: specLoading } = useQuery({
        queryKey: ['spec', selectedVersionId],
        queryFn: async () => {
            const response = await versionsApi.getById(selectedVersionId);
            return response.data;
        },
        enabled: !!selectedVersionId,
    });

    // Parse the spec content - with JSONB it's already parsed
    const spec: AnySpec | null = specData?.specContent
        ? typeof specData.specContent === 'string'
            ? (JSON.parse(specData.specContent) as AnySpec)
            : (specData.specContent as AnySpec)
        : null;

    const autoBaseUrl = (() => {
        if (!spec || !service?.openApiUrl) {
            return '';
        }

        // Swagger 2.0: host + schemes + basePath
        if ('swagger' in spec) {
            const swagger = spec as any;
            const scheme =
                swagger.schemes?.[0] ||
                (() => {
                    try {
                        return new URL(service.openApiUrl).protocol.replace(':', '');
                    } catch {
                        return 'http';
                    }
                })();

            const host =
                swagger.host ||
                (() => {
                    try {
                        return new URL(service.openApiUrl).host;
                    } catch {
                        return '';
                    }
                })();

            const basePath = swagger.basePath || '';
            const basePathNormalized =
                basePath === '/'
                    ? ''
                    : basePath.startsWith('/')
                      ? basePath
                      : basePath
                        ? `/${basePath}`
                        : '';
            const computed = host ? `${scheme}://${host}${basePathNormalized}` : '';
            return computed;
        }

        // OpenAPI 3.x: use servers if present, otherwise trim the spec URL.
        const v3 = spec as any;
        const serverUrl = v3.servers?.[0]?.url;
        if (typeof serverUrl === 'string' && serverUrl.trim() !== '') {
            return serverUrl;
        }

        return service.openApiUrl
            .replace(/\/openapi\.json$/, '')
            .replace(/\/swagger\.json$/, '')
            .replace(/\/api\/docs\/json$/, '');
    })();

    // Many specs use paths like `/live`, while the OpenAPI URL may be hosted under `/api/*/openapi.json`.
    // Stripping a trailing `/api` avoids producing `/api/api/...` in Try-it-out requests.
    const autoBaseUrlNormalized = (() => {
        try {
            const u = new URL(autoBaseUrl);
            if (u.pathname.endsWith('/api/')) {
                u.pathname = u.pathname.slice(0, -4);
            } else if (u.pathname.endsWith('/api')) {
                u.pathname = u.pathname.slice(0, -4);
            }
            // Avoid output like `https://host` => pathname '/' is ok, but `URL.toString()` already keeps it.
            return u.toString().replace(/\/$/, '');
        } catch {
            return autoBaseUrl.endsWith('/api/')
                ? autoBaseUrl.slice(0, -4)
                : autoBaseUrl.endsWith('/api')
                  ? autoBaseUrl.slice(0, -4)
                  : autoBaseUrl;
        }
    })();

    // Determine base URL: custom override or auto-detected.
    const baseUrl = customBaseUrl || autoBaseUrlNormalized;

    const availableSecuritySchemes = useMemo(() => {
        if (!spec) return {};
        const isSwagger2 = typeof (spec as any).swagger === 'string';
        return isSwagger2
            ? ((spec as any).securityDefinitions as Record<string, any> | undefined) || {}
            : ((spec as any).components?.securitySchemes as Record<string, any> | undefined) || {};
    }, [spec]);

    if (!service) {
        return <div className="text-center py-12">Loading...</div>;
    }

    return (
        <div>
            {/* Header */}
            <div className="mb-6">
                <div className="flex items-center justify-between">
                    <div>
                        <h1 className="text-2xl font-bold text-gray-900">{spec?.info?.title || service.name}</h1>
                        <p className="text-gray-600 mt-1">{spec?.info?.version && `Version: ${spec.info.version}`}</p>
                    </div>
                    <div className="flex items-center gap-2">
                        <button
                            onClick={() => setShowBaseUrlInput(!showBaseUrlInput)}
                            className="text-sm text-gray-600 hover:text-gray-900 underline"
                        >
                            {showBaseUrlInput ? 'Hide' : 'Set'} Base URL
                        </button>
                        <button
                            onClick={() => setIsAuthorizeOpen(true)}
                            className="text-sm text-gray-600 hover:text-gray-900 underline"
                        >
                            Authorize
                        </button>
                        <a
                            href={service.openApiUrl}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="text-blue-600 hover:text-blue-800 text-sm"
                        >
                            Open Original →
                        </a>
                    </div>
                </div>

                {/* Base URL Input */}
                {showBaseUrlInput && (
                    <div className="mt-4 p-4 bg-gray-50 rounded-lg border border-gray-200">
                        <label className="block text-sm font-medium text-gray-700 mb-2">Base URL for API requests</label>
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
                                        d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1"
                                    />
                                </svg>
                            </div>
                            <input
                                type="text"
                                value={customBaseUrl}
                                onChange={(e) => setCustomBaseUrl(e.target.value)}
                                placeholder="https://api.example.com"
                                className="w-full pl-10 pr-10 py-2 border border-gray-300 rounded-lg shadow-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
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
                            {customBaseUrl && (
                                <button
                                    onClick={() => setCustomBaseUrl('')}
                                    className="absolute inset-y-0 right-0 pr-3 flex items-center text-gray-400 hover:text-gray-600"
                                >
                                    <svg className="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                                    </svg>
                                </button>
                            )}
                        </div>
                        <p className="mt-2 text-xs text-gray-500">
                            Example:{' '}
                            <code className="bg-gray-200 px-1.5 py-0.5 rounded">https://api.example.com</code>{' '}
                            or{' '}
                            <code className="bg-gray-200 px-1.5 py-0.5 rounded">http://192.168.1.100:8080</code>
                            {baseUrl && !customBaseUrl && (
                                <span className="block mt-1">
                                    Current (auto-detected):{' '}
                                    <code className="bg-gray-200 px-1.5 py-0.5 rounded">{baseUrl}</code>
                                </span>
                            )}
                        </p>
                    </div>
                )}

                {/* Version Selector */}
                <div className="mt-4 flex items-center gap-2">
                    <span className="text-sm text-gray-600">Viewing version:</span>
                    <select
                        value={selectedVersionId || ''}
                        onChange={(e) => {
                            const newParams = new URLSearchParams(searchParams);
                            newParams.set('version', e.target.value);
                            window.history.pushState({}, '', `?${newParams.toString()}`);
                        }}
                        className="rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                    >
                        {versions.map((version, index) => (
                            <option key={version.id} value={version.id}>
                                v{versions.length - index} - {version.specTitle || 'Unknown'} ({new Date(version.pulledAt).toLocaleDateString()})
                            </option>
                        ))}
                    </select>
                </div>
            </div>

            {/* Spec Viewer */}
            {specLoading ? (
                <div className="text-center py-12">Loading specification...</div>
            ) : spec ? (
                <>
                    <ApiSpecViewer spec={spec} baseUrl={baseUrl} authCredentials={authCredentials} />

                    <Modal isOpen={isAuthorizeOpen} onClose={() => setIsAuthorizeOpen(false)} title="Authorize" size="lg">
                        <div className="space-y-4">
                            <div className="text-sm text-gray-600">
                                Provide credentials for supported security schemes. Stored in this browser session.
                            </div>

                            {Object.keys(availableSecuritySchemes).length === 0 ? (
                                <div className="text-sm text-gray-500">No security schemes found in this specification.</div>
                            ) : (
                                <div className="space-y-3">
                                    {Object.entries(availableSecuritySchemes).map(([schemeName, schemeDef]) => {
                                        const type = (schemeDef as any)?.type;
                                        const cred = authCredentials[schemeName] || {};

                                        const setCredPatch = (patch: any) => {
                                            setAuthCredentials((prev) => {
                                                const next = { ...prev };
                                                next[schemeName] = {
                                                    ...(next[schemeName] || {}),
                                                    ...patch,
                                                };
                                                return next;
                                            });
                                        };

                                        const clearScheme = () => {
                                            setAuthCredentials((prev) => {
                                                const next = { ...prev };
                                                delete next[schemeName];
                                                return next;
                                            });
                                        };

                                        return (
                                            <div key={schemeName} className="border rounded-lg p-3 bg-gray-50">
                                                <div className="flex items-start justify-between gap-3">
                                                    <div className="min-w-0">
                                                        <div className="font-mono font-semibold text-gray-900 truncate">{schemeName}</div>
                                                        <div className="text-xs text-gray-600">Type: {String(type)}</div>
                                                    </div>

                                                    <button
                                                        type="button"
                                                        onClick={clearScheme}
                                                        className="text-xs text-red-600 hover:text-red-700 underline"
                                                    >
                                                        Clear
                                                    </button>
                                                </div>

                                                <div className="mt-3 space-y-2">
                                                    {type === 'apiKey' && (
                                                        <div>
                                                            <div className="text-xs font-medium text-gray-700 mb-1">
                                                                API key ({String((schemeDef as any).in)}:{' '}{(schemeDef as any).name})
                                                            </div>
                                                            <input
                                                                type="text"
                                                                value={cred.value || ''}
                                                                onChange={(e) =>
                                                                    setCredPatch({
                                                                        value: e.target.value,
                                                                    })
                                                                }
                                                                className="w-full rounded border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm border p-2"
                                                                placeholder="Enter API key"
                                                            />
                                                        </div>
                                                    )}

                                                    {type === 'http' && (schemeDef as any).scheme === 'basic' && (
                                                        <div className="space-y-2">
                                                            <div>
                                                                <label className="block text-xs font-medium text-gray-700">Username</label>
                                                                <input
                                                                    type="text"
                                                                    value={cred.username || ''}
                                                                    onChange={(e) =>
                                                                        setCredPatch({
                                                                            username: e.target.value,
                                                                        })
                                                                    }
                                                                    className="w-full rounded border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm border p-2"
                                                                    placeholder="Username"
                                                                />
                                                            </div>
                                                            <div>
                                                                <label className="block text-xs font-medium text-gray-700">Password</label>
                                                                <input
                                                                    type="password"
                                                                    value={cred.password || ''}
                                                                    onChange={(e) =>
                                                                        setCredPatch({
                                                                            password: e.target.value,
                                                                        })
                                                                    }
                                                                    className="w-full rounded border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm border p-2"
                                                                    placeholder="Password"
                                                                />
                                                            </div>
                                                        </div>
                                                    )}

                                                    {type === 'basic' && (
                                                        <div className="space-y-2">
                                                            <div>
                                                                <label className="block text-xs font-medium text-gray-700">Username</label>
                                                                <input
                                                                    type="text"
                                                                    value={cred.username || ''}
                                                                    onChange={(e) => setCredPatch({ username: e.target.value })}
                                                                    className="w-full rounded border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm border p-2"
                                                                    placeholder="Username"
                                                                />
                                                            </div>
                                                            <div>
                                                                <label className="block text-xs font-medium text-gray-700">Password</label>
                                                                <input
                                                                    type="password"
                                                                    value={cred.password || ''}
                                                                    onChange={(e) => setCredPatch({ password: e.target.value })}
                                                                    className="w-full rounded border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm border p-2"
                                                                    placeholder="Password"
                                                                />
                                                            </div>
                                                        </div>
                                                    )}

                                                    {type === 'http' && (schemeDef as any).scheme !== 'basic' && (
                                                        <div>
                                                            <div className="text-xs font-medium text-gray-700 mb-1">
                                                                {(schemeDef as any).scheme || 'http'} token
                                                            </div>
                                                            <input
                                                                type="text"
                                                                value={cred.token || ''}
                                                                onChange={(e) => setCredPatch({ token: e.target.value })}
                                                                className="w-full rounded border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm border p-2"
                                                                placeholder="Enter token"
                                                            />
                                                        </div>
                                                    )}

                                                    {(type === 'oauth2' || type === 'openIdConnect') && (
                                                        <div>
                                                            <div className="text-xs font-medium text-gray-700 mb-1">Access token</div>
                                                            <input
                                                                type="text"
                                                                value={cred.token || ''}
                                                                onChange={(e) => setCredPatch({ token: e.target.value })}
                                                                className="w-full rounded border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm border p-2"
                                                                placeholder="Enter access token"
                                                            />
                                                        </div>
                                                    )}

                                                    {!(
                                                        type === 'apiKey' ||
                                                        (type === 'http' && (schemeDef as any).scheme === 'basic') ||
                                                        type === 'basic' ||
                                                        (type === 'http' && (schemeDef as any).scheme !== 'basic') ||
                                                        type === 'oauth2' ||
                                                        type === 'openIdConnect'
                                                    ) && (
                                                        <div>
                                                            <div className="text-xs font-medium text-gray-700 mb-1">Token</div>
                                                            <input
                                                                type="text"
                                                                value={cred.token || ''}
                                                                onChange={(e) => setCredPatch({ token: e.target.value })}
                                                                className="w-full rounded border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm border p-2"
                                                                placeholder="Enter token"
                                                            />
                                                        </div>
                                                    )}
                                                </div>
                                            </div>
                                        );
                                    })}
                                </div>
                            )}

                            <div className="flex justify-end gap-2 pt-2">
                                <button
                                    type="button"
                                    onClick={() => setAuthCredentials({})}
                                    className="px-3 py-2 rounded-md text-sm font-semibold whitespace-nowrap bg-gray-100 text-gray-700 hover:bg-gray-200"
                                >
                                    Clear all
                                </button>
                                <button
                                    type="button"
                                    onClick={() => setIsAuthorizeOpen(false)}
                                    className="px-3 py-2 rounded-md text-sm font-semibold whitespace-nowrap bg-blue-600 text-white hover:bg-blue-700"
                                >
                                    Done
                                </button>
                            </div>
                        </div>
                    </Modal>
                </>
            ) : (
                <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
                    <p className="text-yellow-800">Unable to parse OpenAPI specification. The content may not be valid JSON.</p>
                </div>
            )}
        </div>
    );
}
