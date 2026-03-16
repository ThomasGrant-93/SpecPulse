import {usePagination} from '@/hooks';
import type {SpecVersion} from '@/types';

interface VersionListProps {
    versions: SpecVersion[];
    serviceId: number;
}

export default function VersionList({versions, serviceId}: VersionListProps) {
    const {
        data: paginatedVersions,
        page,
        totalPages,
        totalItems,
        nextPage,
        previousPage,
        setPageSize,
    } = usePagination(versions, {initialPage: 1, initialPageSize: 5});

    if (versions.length === 0) {
        return (
                <div className="text-center py-8 bg-white rounded-lg shadow">
                    <p className="text-gray-500">
                        No versions yet. Click "Pull Now" to fetch the first version.
                    </p>
                </div>
        );
    }

    return (
            <div>
                <div className="overflow-hidden shadow ring-1 ring-black ring-opacity-5 sm:rounded-lg">
                    <table className="min-w-full divide-y divide-gray-300">
                        <thead className="bg-gray-50">
                        <tr>
                            <th className="py-3.5 pl-4 pr-3 text-left text-sm font-semibold text-gray-900">
                                #
                            </th>
                            <th className="px-3 py-3.5 text-left text-sm font-semibold text-gray-900">
                                Title
                            </th>
                            <th className="px-3 py-3.5 text-left text-sm font-semibold text-gray-900">
                                Hash
                            </th>
                            <th className="px-3 py-3.5 text-left text-sm font-semibold text-gray-900">
                                Size
                            </th>
                            <th className="px-3 py-3.5 text-left text-sm font-semibold text-gray-900">
                                Pulled At
                            </th>
                            <th className="px-3 py-3.5 text-right text-sm font-semibold text-gray-900">
                                Actions
                            </th>
                        </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-200 bg-white">
                        {paginatedVersions.map((version, index) => {
                            const globalIndex = (page - 1) * 5 + index;
                            return (
                                    <tr
                                            key={version.id}
                                            className={globalIndex === 0 ? 'bg-green-50' : ''}
                                    >
                                        <td className="whitespace-nowrap py-4 pl-4 pr-3 text-sm">
                                        <span
                                                className={`inline-flex items-center rounded-md px-2 py-1 text-xs font-medium ${
                                                        globalIndex === 0
                                                                ? 'bg-green-100 text-green-800'
                                                                : 'bg-gray-100 text-gray-800'
                                                }`}
                                        >
                                            {globalIndex === 0 && '✓ '}v
                                            {versions.length - globalIndex}
                                        </span>
                                        </td>
                                        <td className="whitespace-nowrap py-4 pl-4 pr-3 text-sm font-medium text-gray-900">
                                            {version.specTitle || 'Unknown'}
                                            {version.specVersion && (
                                                    <span className="ml-2 text-xs text-gray-500">
                                                v{version.specVersion}
                                            </span>
                                            )}
                                        </td>
                                        <td className="whitespace-nowrap px-3 py-4 text-sm text-gray-500 font-mono text-xs">
                                            {version.versionHash.substring(0, 16)}...
                                        </td>
                                        <td className="whitespace-nowrap px-3 py-4 text-sm text-gray-500">
                                            {version.fileSizeBytes
                                                    ? Math.round(version.fileSizeBytes / 1024)
                                                    : 0}{' '}
                                            KB
                                        </td>
                                        <td className="whitespace-nowrap px-3 py-4 text-sm text-gray-500">
                                            <div className="flex flex-col">
                                            <span>
                                                {new Date(version.pulledAt).toLocaleDateString()}
                                            </span>
                                                <span className="text-xs text-gray-400">
                                                {new Date(version.pulledAt).toLocaleTimeString()}
                                            </span>
                                            </div>
                                        </td>
                                        <td className="whitespace-nowrap px-3 py-4 text-sm text-right">
                                            <a
                                                    href={`/services/${serviceId}/spec?version=${version.id}`}
                                                    className="inline-flex items-center gap-1 rounded-md bg-purple-600 px-3 py-1.5 text-xs font-semibold text-white shadow-sm hover:bg-purple-500 transition-colors"
                                            >
                                                <svg
                                                        className="h-3.5 w-3.5"
                                                        fill="none"
                                                        stroke="currentColor"
                                                        viewBox="0 0 24 24"
                                                >
                                                    <path
                                                            strokeLinecap="round"
                                                            strokeLinejoin="round"
                                                            strokeWidth={2}
                                                            d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"
                                                    />
                                                    <path
                                                            strokeLinecap="round"
                                                            strokeLinejoin="round"
                                                            strokeWidth={2}
                                                            d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"
                                                    />
                                                </svg>
                                                View Spec
                                            </a>
                                        </td>
                                    </tr>
                            );
                        })}
                        </tbody>
                    </table>
                </div>

                {/* Pagination */}
                {totalItems > 0 && (
                        <div className="mt-4 flex items-center justify-between">
                            <div className="flex items-center gap-2">
                                <select
                                        value={5}
                                        onChange={(e) => setPageSize(Number(e.target.value))}
                                        className="rounded-md border-gray-300 py-1 pl-2 pr-8 text-sm focus:border-blue-500 focus:ring-blue-500"
                                >
                                    <option value={5}>5 / page</option>
                                    <option value={10}>10 / page</option>
                                    <option value={20}>20 / page</option>
                                </select>
                                <span className="text-sm text-gray-600">
                            Page {page} of {totalPages}
                        </span>
                            </div>
                            <div className="flex gap-2">
                                <button
                                        onClick={() => previousPage()}
                                        disabled={page === 1}
                                        className={`rounded-md px-3 py-1.5 text-sm font-semibold ${
                                                page === 1
                                                        ? 'bg-gray-100 text-gray-400 cursor-not-allowed'
                                                        : 'bg-white text-gray-700 hover:bg-gray-50 border border-gray-300'
                                        }`}
                                >
                                    Previous
                                </button>
                                <button
                                        onClick={() => nextPage()}
                                        disabled={page === totalPages}
                                        className={`rounded-md px-3 py-1.5 text-sm font-semibold ${
                                                page === totalPages
                                                        ? 'bg-gray-100 text-gray-400 cursor-not-allowed'
                                                        : 'bg-white text-gray-700 hover:bg-gray-50 border border-gray-300'
                                        }`}
                                >
                                    Next
                                </button>
                            </div>
                        </div>
                )}
            </div>
    );
}
