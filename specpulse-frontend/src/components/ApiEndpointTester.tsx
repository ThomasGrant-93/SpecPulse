import {useState} from 'react';
import type {ApiEndpoint} from '@/types/openapi';
import {apiTester, type TestResponse} from '@/services/apiTester';

interface ApiEndpointTesterProps {
    endpoint: ApiEndpoint;
    baseUrl: string;
}

const methodColors: Record<string, string> = {
    get: 'bg-blue-600 hover:bg-blue-700',
    post: 'bg-green-600 hover:bg-green-700',
    put: 'bg-yellow-600 hover:bg-yellow-700',
    delete: 'bg-red-600 hover:bg-red-700',
    patch: 'bg-orange-600 hover:bg-orange-700',
};

export default function ApiEndpointTester({endpoint, baseUrl}: ApiEndpointTesterProps) {
    const [pathParams, setPathParams] = useState<Record<string, string>>({});
    const [queryParams, setQueryParams] = useState<Record<string, string>>({});
    const [headerParams, setHeaderParams] = useState<Record<string, string>>({});
    const [requestBody, setRequestBody] = useState<string>('');
    const [response, setResponse] = useState<TestResponse | null>(null);
    const [isLoading, setIsLoading] = useState(false);
    const [expanded, setExpanded] = useState(false);

    // Extract path parameters from the URL
    const pathParamNames = endpoint.path.match(/\{([^}]+)\}/g)?.map((s) => s.slice(1, -1)) || [];

    // Separate parameters by location
    const pathParameters = endpoint.parameters?.filter((p) => p.in === 'path') || [];
    const queryParameters = endpoint.parameters?.filter((p) => p.in === 'query') || [];
    const headerParameters = endpoint.parameters?.filter((p) => p.in === 'header') || [];

    const handleExecute = async () => {
        setIsLoading(true);
        setResponse(null);

        // Build the full URL with path parameters
        let fullUrl = baseUrl + endpoint.path;
        pathParamNames.forEach((paramName) => {
            fullUrl = fullUrl.replace(`{${paramName}}`, pathParams[paramName] || `{${paramName}}`);
        });

        // Prepare headers
        const headers: Record<string, string> = {
            'Content-Type': 'application/json',
            ...headerParams,
        };

        // Parse request body
        let body = undefined;
        if (requestBody && ['post', 'put', 'patch'].includes(endpoint.method)) {
            try {
                body = JSON.parse(requestBody);
            } catch (e) {
                body = requestBody;
            }
        }

        const result = await apiTester.execute({
            url: fullUrl,
            method: endpoint.method,
            parameters: queryParams,
            headers,
            body,
        });

        setResponse(result);
        setIsLoading(false);
        setExpanded(true);
    };

    const updatePathParam = (name: string, value: string) => {
        setPathParams((prev) => ({...prev, [name]: value}));
    };

    const updateQueryParam = (name: string, value: string) => {
        setQueryParams((prev) => ({...prev, [name]: value}));
    };

    const updateHeaderParam = (name: string, value: string) => {
        setHeaderParams((prev) => ({...prev, [name]: value}));
    };

    return (
            <div className="border rounded-lg mt-4 overflow-hidden">
                {/* Try It Out Button */}
                <div className="bg-gray-50 px-4 py-3 flex items-center justify-between">
                    <span className="text-sm font-medium text-gray-700">Try it out</span>
                    <button
                            onClick={handleExecute}
                            disabled={isLoading}
                            className={`px-4 py-2 rounded text-white text-sm font-medium transition-colors ${
                                    methodColors[endpoint.method] || 'bg-blue-600'
                            } ${isLoading ? 'opacity-50 cursor-not-allowed' : ''}`}
                    >
                        {isLoading ? 'Executing...' : 'Execute'}
                    </button>
                </div>

                {/* Parameters */}
                <div className="p-4 space-y-4">
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
                                                        placeholder={typeof param.schema?.example === 'string' ? param.schema.example : ''}
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
                                                        placeholder={typeof param.schema?.example === 'string' ? param.schema.example : ''}
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

                    {/* Header Parameters */}
                    {headerParameters.length > 0 && (
                            <div>
                                <h4 className="text-sm font-semibold text-gray-900 mb-2">Headers</h4>
                                <div className="space-y-2">
                                    {headerParameters.map((param) => (
                                            <div key={param.name} className="flex items-center gap-2">
                                                <label className="text-sm text-gray-700 w-32 font-mono">
                                                    {param.name}
                                                </label>
                                                <input
                                                        type="text"
                                                        value={headerParams[param.name] || ''}
                                                        onChange={(e) =>
                                                                updateHeaderParam(param.name, e.target.value)
                                                        }
                                                        placeholder={typeof param.schema?.example === 'string' ? param.schema.example : ''}
                                                        className="flex-1 rounded border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                                                />
                                            </div>
                                    ))}
                                </div>
                            </div>
                    )}

                    {/* Request Body */}
                    {endpoint.requestBody && (
                            <div>
                                <h4 className="text-sm font-semibold text-gray-900 mb-2">Request Body</h4>
                                <textarea
                                        value={requestBody}
                                        onChange={(e) => setRequestBody(e.target.value)}
                                        rows={8}
                                        className="w-full rounded border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm font-mono"
                                        placeholder={JSON.stringify({example: 'value'}, null, 2)}
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
