import {useNavigate, useParams} from 'react-router-dom';
import {useQuery} from '@tanstack/react-query';
import {diffsApi, registryApi, versionsApi} from '@/services/api';
import DiffViewer from '@/components/DiffViewer';
import SideBySideDiff from '@/components/SideBySideDiff';
import {useState} from 'react';

export default function DiffsPage() {
    const {id} = useParams<{ id: string }>();
    const navigate = useNavigate();
    const serviceId = Number(id);
    const [selectedFromVersion, setSelectedFromVersion] = useState<number | null>(null);
    const [selectedToVersion, setSelectedToVersion] = useState<number | null>(null);

    // Fetch spec content for selected versions
    const {data: fromVersionSpec} = useQuery({
        queryKey: ['version-spec', selectedFromVersion],
        queryFn: async () => {
            if (!selectedFromVersion) return null;
            const response = await versionsApi.getById(selectedFromVersion);
            return response.data.specContent;
        },
        enabled: !!selectedFromVersion,
    });

    const {data: toVersionSpec} = useQuery({
        queryKey: ['version-spec', selectedToVersion],
        queryFn: async () => {
            if (!selectedToVersion) return null;
            const response = await versionsApi.getById(selectedToVersion);
            return response.data.specContent;
        },
        enabled: !!selectedToVersion,
    });

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

    const {data: diffs = []} = useQuery({
        queryKey: ['diffs', serviceId],
        queryFn: async () => {
            const response = await diffsApi.getByService(serviceId);
            return response.data;
        },
        enabled: !isNaN(serviceId),
    });

    const breakingChangesCount = diffs.filter((d) => d.hasBreakingChanges).length;

    return (
            <div>
                {/* Header */}
                <div className="mb-6">
                    <button
                            onClick={() => navigate(`/services/${serviceId}`)}
                            className="text-sm text-blue-600 hover:text-blue-800 mb-4 flex items-center gap-1"
                    >
                        ← Back to service
                    </button>

                    <div className="flex items-center justify-between">
                        <div>
                            <h1 className="text-2xl font-bold text-gray-900">
                                {service?.name} - Breaking Changes
                            </h1>
                            <p className="text-gray-600 mt-1">
                                Compare OpenAPI specifications between versions
                            </p>
                        </div>
                        {breakingChangesCount > 0 && (
                                <span className="inline-flex items-center rounded-md bg-red-100 px-3 py-2 text-sm font-medium text-red-800">
                            ⚠️ {breakingChangesCount} breaking change
                                    {breakingChangesCount !== 1 ? 's' : ''} detected
                        </span>
                        )}
                    </div>
                </div>

                {/* Compare Versions Section */}
                <div className="bg-white rounded-lg shadow p-6 mb-8">
                    <div className="flex items-center justify-between mb-4">
                        <h2 className="text-lg font-semibold text-gray-900">Compare Versions</h2>
                        <div className="flex items-center gap-2 text-sm text-gray-500">
                        <span className="flex items-center gap-1">
                            <span className="w-2 h-2 rounded-full bg-red-400"></span> Old
                        </span>
                            <span className="flex items-center gap-1">
                            <span className="w-2 h-2 rounded-full bg-green-400"></span> New
                        </span>
                        </div>
                    </div>

                    <div className="flex items-center gap-4 mb-6">
                        <div className="flex-1">
                            <label className="block text-xs font-medium text-gray-500 mb-1.5 uppercase tracking-wide">
                                From Version
                            </label>
                            <div className="relative">
                                <select
                                        value={selectedFromVersion || ''}
                                        onChange={(e) => setSelectedFromVersion(Number(e.target.value))}
                                        className="w-full appearance-none rounded-lg border border-gray-300 bg-white px-4 py-2.5 pr-10 text-sm font-medium text-gray-900 shadow-sm focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20 transition-all"
                                >
                                    <option value="">Select version...</option>
                                    {versions.map((version, index) => (
                                            <option key={version.id} value={version.id}>
                                                v{versions.length - index} —{' '}
                                                {version.specTitle || 'Unknown'} (
                                                {new Date(version.pulledAt).toLocaleDateString()})
                                            </option>
                                    ))}
                                </select>
                                <div className="pointer-events-none absolute inset-y-0 right-0 flex items-center px-3 text-gray-400">
                                    <svg
                                            className="h-4 w-4"
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
                                </div>
                            </div>
                        </div>

                        <div className="pt-6">
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
                                        d="M17 8l4 4m0 0l-4 4m4-4H3"
                                />
                            </svg>
                        </div>

                        <div className="flex-1">
                            <label className="block text-xs font-medium text-gray-500 mb-1.5 uppercase tracking-wide">
                                To Version
                            </label>
                            <div className="relative">
                                <select
                                        value={selectedToVersion || ''}
                                        onChange={(e) => setSelectedToVersion(Number(e.target.value))}
                                        className="w-full appearance-none rounded-lg border border-gray-300 bg-white px-4 py-2.5 pr-10 text-sm font-medium text-gray-900 shadow-sm focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20 transition-all"
                                >
                                    <option value="">Select version...</option>
                                    {versions.map((version, index) => (
                                            <option
                                                    key={version.id}
                                                    value={version.id}
                                                    disabled={version.id === selectedFromVersion}
                                            >
                                                v{versions.length - index} —{' '}
                                                {version.specTitle || 'Unknown'} (
                                                {new Date(version.pulledAt).toLocaleDateString()})
                                            </option>
                                    ))}
                                </select>
                                <div className="pointer-events-none absolute inset-y-0 right-0 flex items-center px-3 text-gray-400">
                                    <svg
                                            className="h-4 w-4"
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
                                </div>
                            </div>
                        </div>
                    </div>

                    {selectedFromVersion && selectedToVersion && fromVersionSpec && toVersionSpec && (
                            <div className="mt-6">
                                <SideBySideDiff oldSpec={fromVersionSpec} newSpec={toVersionSpec}/>
                            </div>
                    )}

                    {selectedFromVersion &&
                            selectedToVersion &&
                            (!fromVersionSpec || !toVersionSpec) && (
                                    <div className="mt-4 text-center py-8">
                                        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto"></div>
                                        <p className="mt-2 text-gray-600">Loading spec content...</p>
                                    </div>
                            )}
                </div>

                {/* Historical Diffs */}
                <div className="bg-white rounded-lg shadow">
                    <div className="border-b px-6 py-4">
                        <h2 className="text-lg font-semibold text-gray-900">
                            Historical Comparisons ({diffs.length})
                        </h2>
                    </div>
                    <DiffViewer diffs={diffs}/>
                </div>
            </div>
    );
}
