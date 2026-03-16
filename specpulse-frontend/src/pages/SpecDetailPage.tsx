import {useParams, useSearchParams} from 'react-router-dom';
import {useQuery} from '@tanstack/react-query';
import {registryApi, versionsApi} from '@/services/api';
import ApiSpecViewer from '@/components/ApiSpecViewer';
import type {OpenAPISpec} from '@/types/openapi';
import {useState} from 'react';

export default function SpecDetailPage() {
    const {id} = useParams<{ id: string }>();
    const [searchParams] = useSearchParams();
    const serviceId = Number(id);
    const versionIdParam = searchParams.get('version');

    const [customBaseUrl, setCustomBaseUrl] = useState<string>('');
    const [showBaseUrlInput, setShowBaseUrlInput] = useState(false);

    const {data: service} = useQuery({
        queryKey: ['service', serviceId],
        queryFn: async () => {
            const response = await registryApi.getById(serviceId);
            return response.data;
        },
        enabled: !isNaN(serviceId),
    });

    const {data: versions = []} = useQuery({
        queryKey: ['versions', serviceId],
        queryFn: async () => {
            const response = await versionsApi.getByService(serviceId);
            return response.data;
        },
        enabled: !isNaN(serviceId),
    });

    const selectedVersionId = versionIdParam ? Number(versionIdParam) : versions[0]?.id;

    const {data: specData, isLoading: specLoading} = useQuery({
        queryKey: ['spec', selectedVersionId],
        queryFn: async () => {
            const response = await versionsApi.getById(selectedVersionId);
            return response.data;
        },
        enabled: !!selectedVersionId,
    });

    // Parse the spec content - with JSONB it's already parsed
    const spec: OpenAPISpec | null = specData?.specContent
            ? typeof specData.specContent === 'string'
                    ? JSON.parse(specData.specContent)
                    : specData.specContent
            : null;

    // Determine base URL: custom or derived from spec URL
    const baseUrl =
            customBaseUrl ||
            (service?.openApiUrl
                    ? service.openApiUrl
                            .replace(/\/openapi\.json$/, '')
                            .replace(/\/swagger\.json$/, '')
                            .replace(/\/api\/docs\/json$/, '')
                    : '');

    if (!service) {
        return <div className="text-center py-12">Loading...</div>;
    }

    return (
            <div>
                {/* Header */}
                <div className="mb-6">
                    <div className="flex items-center justify-between">
                        <div>
                            <h1 className="text-2xl font-bold text-gray-900">
                                {spec?.info?.title || service.name}
                            </h1>
                            <p className="text-gray-600 mt-1">
                                {spec?.info?.version && `Version: ${spec.info.version}`}
                            </p>
                        </div>
                        <div className="flex items-center gap-2">
                            <button
                                    onClick={() => setShowBaseUrlInput(!showBaseUrlInput)}
                                    className="text-sm text-gray-600 hover:text-gray-900 underline"
                            >
                                {showBaseUrlInput ? 'Hide' : 'Set'} Base URL
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
                                <label className="block text-sm font-medium text-gray-700 mb-2">
                                    Base URL for API requests
                                </label>
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
                                            // Prevent browser's find-in-page when typing
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
                                <p className="mt-2 text-xs text-gray-500">
                                    Example:{' '}
                                    <code className="bg-gray-200 px-1.5 py-0.5 rounded">
                                        https://api.example.com
                                    </code>{' '}
                                    or{' '}
                                    <code className="bg-gray-200 px-1.5 py-0.5 rounded">
                                        http://192.168.1.100:8080
                                    </code>
                                    {baseUrl && !customBaseUrl && (
                                            <span className="block mt-1">
                                    Current (auto-detected):{' '}
                                                <code className="bg-gray-200 px-1.5 py-0.5 rounded">
                                        {baseUrl}
                                    </code>
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
                                        v{versions.length - index} - {version.specTitle || 'Unknown'} (
                                        {new Date(version.pulledAt).toLocaleDateString()})
                                    </option>
                            ))}
                        </select>
                    </div>
                </div>

                {/* Spec Viewer */}
                {specLoading ? (
                        <div className="text-center py-12">Loading specification...</div>
                ) : spec ? (
                        <ApiSpecViewer spec={spec} baseUrl={baseUrl}/>
                ) : (
                        <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
                            <p className="text-yellow-800">
                                Unable to parse OpenAPI specification. The content may not be valid JSON.
                            </p>
                        </div>
                )}
            </div>
    );
}
