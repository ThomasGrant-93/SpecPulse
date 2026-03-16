import {useMemo, useState} from 'react';
import type {ApiEndpoint, ApiTagGroup, HttpMethod, OpenAPISpec} from '@/types/openapi';
import ApiEndpointTester from './ApiEndpointTester';

interface ApiSpecViewerProps {
    spec: OpenAPISpec;
    baseUrl: string;
}

const methodColors: Record<HttpMethod, string> = {
    get: 'bg-blue-100 text-blue-800 border-blue-300',
    post: 'bg-green-100 text-green-800 border-green-300',
    put: 'bg-yellow-100 text-yellow-800 border-yellow-300',
    delete: 'bg-red-100 text-red-800 border-red-300',
    patch: 'bg-orange-100 text-orange-800 border-orange-300',
    options: 'bg-purple-100 text-purple-800 border-purple-300',
    head: 'bg-indigo-100 text-indigo-800 border-indigo-300',
    trace: 'bg-gray-100 text-gray-800 border-gray-300',
};

export default function ApiSpecViewer({spec, baseUrl}: ApiSpecViewerProps) {
    const [expandedEndpoints, setExpandedEndpoints] = useState<Set<string>>(new Set());
    const [selectedTag, setSelectedTag] = useState<string | null>(null);

    // Group endpoints by tags
    const groupedEndpoints = useMemo(() => {
        const groups: Map<string, ApiTagGroup> = new Map();
        const untagged: ApiEndpoint[] = [];

        Object.entries(spec.paths).forEach(([path, pathItem]) => {
            const methods: (HttpMethod | 'get' | 'post' | 'put' | 'delete' | 'patch')[] = [
                'get',
                'post',
                'put',
                'delete',
                'patch',
                'options',
                'head',
                'trace',
            ];

            methods.forEach((method) => {
                const operation = pathItem[method as HttpMethod];
                if (operation) {
                    const endpoint: ApiEndpoint = {
                        path,
                        method: method as HttpMethod,
                        operationId: operation.operationId,
                        summary: operation.summary,
                        description: operation.description,
                        tags: operation.tags,
                        parameters: operation.parameters,
                        requestBody: operation.requestBody,
                        responses: operation.responses,
                        deprecated: operation.deprecated,
                    };

                    if (operation.tags && operation.tags.length > 0) {
                        operation.tags.forEach((tag) => {
                            if (!groups.has(tag)) {
                                const tagInfo = spec.tags?.find((t) => t.name === tag);
                                groups.set(tag, {
                                    name: tag,
                                    description: tagInfo?.description,
                                    endpoints: [],
                                });
                            }
                            groups.get(tag)!.endpoints.push(endpoint);
                        });
                    } else {
                        untagged.push(endpoint);
                    }
                }
            });
        });

        // Add untagged as a group if there are any
        if (untagged.length > 0) {
            groups.set('Other', {name: 'Other', endpoints: untagged});
        }

        return groups;
    }, [spec]);

    const toggleEndpoint = (id: string) => {
        setExpandedEndpoints((prev) => {
            const next = new Set(prev);
            if (next.has(id)) {
                next.delete(id);
            } else {
                next.add(id);
            }
            return next;
        });
    };

    const tags = Array.from(groupedEndpoints.keys());
    const displayTags = selectedTag ? [selectedTag] : tags;

    return (
            <div className="space-y-4">
                {/* Info Section */}
                <div className="bg-white rounded-lg shadow p-6">
                    <h2 className="text-2xl font-bold text-gray-900">{spec.info.title}</h2>
                    <p className="text-gray-600 mt-1">Version: {spec.info.version}</p>
                    {spec.info.description && (
                            <p className="text-gray-700 mt-3">{spec.info.description}</p>
                    )}
                    {baseUrl && (
                            <div className="mt-3">
                                <span className="text-sm text-gray-500">Base URL: </span>
                                <code className="bg-gray-100 px-2 py-1 rounded text-sm">{baseUrl}</code>
                            </div>
                    )}
                </div>

                {/* Tag Filter */}
                {tags.length > 1 && (
                        <div className="flex flex-wrap gap-2">
                            <button
                                    onClick={() => setSelectedTag(null)}
                                    className={`px-3 py-1 rounded-full text-sm font-medium ${
                                            !selectedTag
                                                    ? 'bg-blue-600 text-white'
                                                    : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                                    }`}
                            >
                                All (
                                {spec.paths
                                        ? Object.values(spec.paths).reduce((acc, pathItem) => {
                                            let count = 0;
                                            ['get', 'post', 'put', 'delete', 'patch'].forEach((m) => {
                                                if (pathItem[m as HttpMethod]) count++;
                                            });
                                            return acc + count;
                                        }, 0)
                                        : 0}
                                )
                            </button>
                            {tags.map((tag) => (
                                    <button
                                            key={tag}
                                            onClick={() => setSelectedTag(tag)}
                                            className={`px-3 py-1 rounded-full text-sm font-medium ${
                                                    selectedTag === tag
                                                            ? 'bg-blue-600 text-white'
                                                            : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                                            }`}
                                    >
                                        {tag} ({groupedEndpoints.get(tag)?.endpoints.length || 0})
                                    </button>
                            ))}
                        </div>
                )}

                {/* Endpoints by Tag */}
                {displayTags.map((tag) => {
                    const group = groupedEndpoints.get(tag);
                    if (!group) return null;

                    return (
                            <div key={tag} className="bg-white rounded-lg shadow">
                                <div className="border-b px-6 py-4">
                                    <h3 className="text-lg font-semibold text-gray-900">{group.name}</h3>
                                    {group.description && (
                                            <p className="text-sm text-gray-600 mt-1">{group.description}</p>
                                    )}
                                </div>
                                <div className="divide-y">
                                    {group.endpoints.map((endpoint) => {
                                        const endpointId = `${endpoint.method}-${endpoint.path}`;
                                        const isExpanded = expandedEndpoints.has(endpointId);

                                        return (
                                                <div key={endpointId} className="hover:bg-gray-50">
                                                    <button
                                                            onClick={() => toggleEndpoint(endpointId)}
                                                            className="w-full px-6 py-4 text-left flex items-center gap-4"
                                                    >
                                            <span
                                                    className={`px-2 py-1 rounded text-xs font-mono font-semibold uppercase border ${
                                                            methodColors[endpoint.method]
                                                    }`}
                                            >
                                                {endpoint.method}
                                            </span>
                                                        <span className="font-mono text-sm text-gray-800 flex-1">
                                                {endpoint.path}
                                            </span>
                                                        {endpoint.deprecated && (
                                                                <span className="px-2 py-1 rounded bg-gray-200 text-gray-600 text-xs">
                                                    Deprecated
                                                </span>
                                                        )}
                                                        <svg
                                                                className={`w-5 h-5 text-gray-400 transition-transform ${
                                                                        isExpanded ? 'rotate-180' : ''
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

                                                    {isExpanded && (
                                                            <div className="px-6 pb-4 space-y-4">
                                                                {/* Try It Out */}
                                                                <ApiEndpointTester
                                                                        endpoint={endpoint}
                                                                        baseUrl={baseUrl}
                                                                />

                                                                {/* Summary & Description */}
                                                                {(endpoint.summary || endpoint.description) && (
                                                                        <div>
                                                                            {endpoint.summary && (
                                                                                    <h4 className="font-semibold text-gray-900">
                                                                                        {endpoint.summary}
                                                                                    </h4>
                                                                            )}
                                                                            {endpoint.description && (
                                                                                    <p className="text-sm text-gray-600 mt-1">
                                                                                        {endpoint.description}
                                                                                    </p>
                                                                            )}
                                                                        </div>
                                                                )}

                                                                {/* Parameters */}
                                                                {endpoint.parameters &&
                                                                        endpoint.parameters.length > 0 && (
                                                                                <div>
                                                                                    <h5 className="font-semibold text-gray-900 text-sm mb-2">
                                                                                        Parameters
                                                                                    </h5>
                                                                                    <div className="bg-gray-50 rounded-lg overflow-hidden">
                                                                                        <table className="min-w-full divide-y divide-gray-200">
                                                                                            <thead className="bg-gray-100">
                                                                                            <tr>
                                                                                                <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase">
                                                                                                    Name
                                                                                                </th>
                                                                                                <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase">
                                                                                                    In
                                                                                                </th>
                                                                                                <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase">
                                                                                                    Required
                                                                                                </th>
                                                                                                <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase">
                                                                                                    Type
                                                                                                </th>
                                                                                            </tr>
                                                                                            </thead>
                                                                                            <tbody className="divide-y divide-gray-200">
                                                                                            {endpoint.parameters.map(
                                                                                                    (param, idx) => (
                                                                                                            <tr key={idx}>
                                                                                                                <td className="px-3 py-2 text-sm font-mono text-gray-900">
                                                                                                                    {param.name}
                                                                                                                </td>
                                                                                                                <td className="px-3 py-2 text-sm text-gray-600">
                                                                                        <span className="px-1.5 py-0.5 rounded bg-gray-200 text-xs">
                                                                                            {
                                                                                                param.in
                                                                                            }
                                                                                        </span>
                                                                                                                </td>
                                                                                                                <td className="px-3 py-2 text-sm text-gray-600">
                                                                                                                    {param.required ? (
                                                                                                                            <span className="text-red-600">
                                                                                                Required
                                                                                            </span>
                                                                                                                    ) : (
                                                                                                                            'Optional'
                                                                                                                    )}
                                                                                                                </td>
                                                                                                                <td className="px-3 py-2 text-sm font-mono text-gray-600">
                                                                                                                    {param
                                                                                                                                    .schema
                                                                                                                                    ?.type ||
                                                                                                                            'any'}
                                                                                                                </td>
                                                                                                            </tr>
                                                                                                    )
                                                                                            )}
                                                                                            </tbody>
                                                                                        </table>
                                                                                    </div>
                                                                                </div>
                                                                        )}

                                                                {/* Request Body */}
                                                                {endpoint.requestBody && (
                                                                        <div>
                                                                            <h5 className="font-semibold text-gray-900 text-sm mb-2">
                                                                                Request Body
                                                                            </h5>
                                                                            <div className="bg-gray-900 rounded-lg p-4 overflow-x-auto">
                                                            <pre className="text-sm text-gray-100">
                                                                {JSON.stringify(
                                                                        endpoint.requestBody,
                                                                        null,
                                                                        2
                                                                )}
                                                            </pre>
                                                                            </div>
                                                                        </div>
                                                                )}

                                                                {/* Responses */}
                                                                {endpoint.responses && (
                                                                        <div>
                                                                            <h5 className="font-semibold text-gray-900 text-sm mb-2">
                                                                                Responses
                                                                            </h5>
                                                                            <div className="space-y-2">
                                                                                {Object.entries(endpoint.responses).map(
                                                                                        ([code, response]) => (
                                                                                                <div
                                                                                                        key={code}
                                                                                                        className={`p-3 rounded-lg border ${
                                                                                                                code.startsWith('2')
                                                                                                                        ? 'bg-green-50 border-green-200'
                                                                                                                        : code.startsWith(
                                                                                                                                '4'
                                                                                                                        )
                                                                                                                                ? 'bg-yellow-50 border-yellow-200'
                                                                                                                                : code.startsWith(
                                                                                                                                        '5'
                                                                                                                                )
                                                                                                                                        ? 'bg-red-50 border-red-200'
                                                                                                                                        : 'bg-gray-50 border-gray-200'
                                                                                                        }`}
                                                                                                >
                                                                                                    <div className="flex items-center gap-2">
                                                                            <span className="font-mono font-semibold">
                                                                                {code}
                                                                            </span>
                                                                                                        <span className="text-gray-700">
                                                                                {
                                                                                    response.description
                                                                                }
                                                                            </span>
                                                                                                    </div>
                                                                                                </div>
                                                                                        )
                                                                                )}
                                                                            </div>
                                                                        </div>
                                                                )}
                                                            </div>
                                                    )}
                                                </div>
                                        );
                                    })}
                                </div>
                            </div>
                    );
                })}
            </div>
    );
}
