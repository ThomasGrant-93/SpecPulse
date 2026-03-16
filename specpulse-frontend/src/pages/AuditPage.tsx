import {useMemo, useState} from 'react';
import {useQuery} from '@tanstack/react-query';
import {auditApi} from '@/services/api';
import {usePagination} from '@/hooks';

const EVENT_TYPES = [
    'ALL',
    'SPEC_FETCH_FAILED',
    'SPEC_VERSION_CREATED',
    'SPEC_VERSION_SKIPPED',
    'DIFF_ANALYZED',
    'BREAKING_CHANGE_DETECTED',
    'SERVICE_CREATED',
    'SERVICE_UPDATED',
    'SERVICE_DELETED',
];

export default function AuditPage() {
    const [selectedEventType, setSelectedEventType] = useState('ALL');
    const [searchQuery, setSearchQuery] = useState('');

    const {data: logs = [], isLoading} = useQuery({
        queryKey: ['audit-recent'],
        queryFn: async () => {
            const response = await auditApi.getRecent(500);
            return response.data;
        },
    });

    // Filter logs
    const filteredLogs = useMemo(() => {
        return logs.filter((log) => {
            const matchesType = selectedEventType === 'ALL' || log.eventType === selectedEventType;
            const matchesSearch =
                    searchQuery === '' ||
                    log.eventType.toLowerCase().includes(searchQuery.toLowerCase()) ||
                    (log.eventDetails &&
                            log.eventDetails.toLowerCase().includes(searchQuery.toLowerCase()));
            return matchesType && matchesSearch;
        });
    }, [logs, selectedEventType, searchQuery]);

    const {
        data: paginatedLogs,
        page,
        totalPages,
        totalItems,
        nextPage,
        previousPage,
        setPageSize,
    } = usePagination(filteredLogs, {initialPage: 1, initialPageSize: 20});

    const getEventColor = (eventType: string) => {
        if (eventType.includes('FAILED') || eventType.includes('ERROR')) {
            return 'bg-red-100 text-red-800';
        }
        if (eventType.includes('BREAKING')) {
            return 'bg-orange-100 text-orange-800';
        }
        if (eventType.includes('CREATED')) {
            return 'bg-green-100 text-green-800';
        }
        return 'bg-gray-100 text-gray-800';
    };

    if (isLoading) {
        return <div className="text-center py-12">Loading...</div>;
    }

    return (
            <div>
                <h1 className="text-2xl font-semibold text-gray-900">Audit Log</h1>
                <p className="mt-2 text-sm text-gray-600">System events and history</p>

                {/* Filters */}
                <div className="mt-6 grid grid-cols-1 gap-4 sm:grid-cols-2">
                    <div>
                        <label htmlFor="eventType" className="block text-sm font-medium text-gray-700">
                            Filter by Event Type
                        </label>
                        <select
                                id="eventType"
                                value={selectedEventType}
                                onChange={(e) => setSelectedEventType(e.target.value)}
                                className="mt-1 block w-full rounded-md border-gray-300 py-2 pl-3 pr-10 text-base focus:border-blue-500 focus:outline-none focus:ring-blue-500 sm:text-sm"
                        >
                            {EVENT_TYPES.map((type) => (
                                    <option key={type} value={type}>
                                        {type}
                                    </option>
                            ))}
                        </select>
                    </div>
                    <div>
                        <label htmlFor="search" className="block text-sm font-medium text-gray-700">
                            Search
                        </label>
                        <input
                                type="text"
                                id="search"
                                value={searchQuery}
                                onChange={(e) => setSearchQuery(e.target.value)}
                                placeholder="Search events..."
                                className="mt-1 block w-full rounded-md border-gray-300 py-2 px-3 focus:border-blue-500 focus:outline-none focus:ring-blue-500 sm:text-sm"
                        />
                    </div>
                </div>

                {/* Results summary */}
                <div className="mt-4 flex items-center justify-between">
                    <p className="text-sm text-gray-600">
                        Showing <span className="font-medium">{filteredLogs.length}</span> of{' '}
                        <span className="font-medium">{logs.length}</span> events
                    </p>
                    {selectedEventType !== 'ALL' || searchQuery !== '' ? (
                            <button
                                    onClick={() => {
                                        setSelectedEventType('ALL');
                                        setSearchQuery('');
                                    }}
                                    className="text-sm text-blue-600 hover:text-blue-800"
                            >
                                Clear filters
                            </button>
                    ) : null}
                </div>

                {/* Table */}
                <div className="mt-4 overflow-hidden shadow ring-1 ring-black ring-opacity-5 sm:rounded-lg">
                    <table className="min-w-full divide-y divide-gray-300">
                        <thead className="bg-gray-50">
                        <tr>
                            <th className="py-3.5 pl-4 pr-3 text-left text-sm font-semibold text-gray-900">
                                Event Type
                            </th>
                            <th className="px-3 py-3.5 text-left text-sm font-semibold text-gray-900">
                                Details
                            </th>
                            <th className="px-3 py-3.5 text-left text-sm font-semibold text-gray-900">
                                Timestamp
                            </th>
                        </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-200 bg-white">
                        {paginatedLogs.map((log) => (
                                <tr key={log.id}>
                                    <td className="whitespace-nowrap py-4 pl-4 pr-3 text-sm">
                                    <span
                                            className={`inline-flex rounded-full px-2 text-xs font-semibold ${getEventColor(log.eventType)}`}
                                    >
                                        {log.eventType}
                                    </span>
                                    </td>
                                    <td className="px-3 py-4 text-sm text-gray-500 max-w-md truncate">
                                        {log.eventDetails || '-'}
                                    </td>
                                    <td className="whitespace-nowrap px-3 py-4 text-sm text-gray-500">
                                        {new Date(log.createdAt).toLocaleString()}
                                    </td>
                                </tr>
                        ))}
                        </tbody>
                    </table>
                </div>

                {/* Pagination */}
                {totalItems > 0 && (
                        <div className="mt-6 flex items-center justify-between border-t border-gray-200 pt-4">
                            <div className="flex items-center gap-4">
                                <p className="text-sm text-gray-700">
                                    Showing <span className="font-medium">{(page - 1) * 20 + 1}</span> to{' '}
                                    <span className="font-medium">{Math.min(page * 20, totalItems)}</span>{' '}
                                    of <span className="font-medium">{totalItems}</span> results
                                </p>
                                <select
                                        value={20}
                                        onChange={(e) => setPageSize(Number(e.target.value))}
                                        className="rounded-md border-gray-300 py-1 pl-2 pr-8 text-sm focus:border-blue-500 focus:ring-blue-500"
                                >
                                    <option value={10}>10 / page</option>
                                    <option value={20}>20 / page</option>
                                    <option value={50}>50 / page</option>
                                    <option value={100}>100 / page</option>
                                </select>
                            </div>
                            <div className="flex gap-2">
                                <button
                                        onClick={() => previousPage()}
                                        disabled={page === 1}
                                        className={`rounded-md px-3 py-2 text-sm font-semibold ${
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
                                        className={`rounded-md px-3 py-2 text-sm font-semibold ${
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
