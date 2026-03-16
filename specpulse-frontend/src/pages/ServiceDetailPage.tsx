import {useNavigate, useParams} from 'react-router-dom';
import {useQuery} from '@tanstack/react-query';
import {pullApi, registryApi, versionsApi} from '@/services/api';
import VersionList from '@/components/VersionList';
import type {SpecDiff} from '@/types';

export default function ServiceDetailPage() {
    const {id} = useParams<{ id: string }>();
    const navigate = useNavigate();
    const serviceId = Number(id);

    const {data: service, error} = useQuery({
        queryKey: ['service', serviceId],
        queryFn: async () => {
            const response = await registryApi.getById(serviceId);
            return response.data;
        },
        enabled: !isNaN(serviceId),
        retry: (failureCount, err: unknown) => {
            // Don't retry on 404 errors
            const axiosError = err as { response?: { status?: number } };
            return axiosError.response?.status !== 404 && failureCount < 3;
        },
    });

    const {data: versions = [], refetch: refetchVersions} = useQuery({
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
            const response = await fetch(`/api/v1/diffs/service/${serviceId}`).then((r) =>
                    r.json()
            );
            return response as SpecDiff[];
        },
        enabled: !isNaN(serviceId),
    });

    const handlePull = async () => {
        await pullApi.triggerService(serviceId);
        refetchVersions();
    };

    const handleViewDiffs = () => {
        navigate(`/services/${serviceId}/diffs`);
    };

    const breakingChangesCount = diffs.filter((d) => d.hasBreakingChanges).length;

    // Handle 404 and other errors
    if (error) {
        const isNotFound = (error as { response?: { status?: number } })?.response?.status === 404;
        return (
                <div className="text-center py-12">
                    <svg
                            className="mx-auto h-12 w-12 text-gray-400"
                            fill="none"
                            stroke="currentColor"
                            viewBox="0 0 24 24"
                    >
                        <path
                                strokeLinecap="round"
                                strokeLinejoin="round"
                                strokeWidth={2}
                                d="M9.172 16.172a4 4 0 015.656 0M9 10h.01M15 10h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
                        />
                    </svg>
                    <h3 className="mt-4 text-lg font-semibold text-gray-900">
                        {isNotFound ? 'Service Not Found' : 'Error Loading Service'}
                    </h3>
                    <p className="mt-2 text-sm text-gray-600">
                        {isNotFound
                                ? `The service with ID ${serviceId} does not exist or has been deleted.`
                                : 'An unexpected error occurred while loading the service.'}
                    </p>
                    <button
                            onClick={() => navigate('/')}
                            className="mt-6 rounded-md bg-blue-600 px-4 py-2 text-sm font-semibold text-white shadow-sm hover:bg-blue-500"
                    >
                        Back to Services
                    </button>
                </div>
        );
    }

    if (!service) {
        return <div className="text-center py-12">Loading...</div>;
    }

    return (
            <div>
                <div className="mb-8">
                    <div className="flex items-center justify-between">
                        <div>
                            <h1 className="text-2xl font-semibold text-gray-900">{service.name}</h1>
                            <p className="mt-2 text-sm text-gray-600">
                                {service.description || 'No description'}
                            </p>
                        </div>
                        <div className="flex gap-2">
                            <button
                                    onClick={handleViewDiffs}
                                    className={`rounded-md px-4 py-2 text-sm font-semibold shadow-sm ${
                                            breakingChangesCount > 0
                                                    ? 'bg-red-600 text-white hover:bg-red-500'
                                                    : 'bg-gray-600 text-white hover:bg-gray-500'
                                    }`}
                            >
                                {breakingChangesCount > 0
                                        ? `⚠️ ${breakingChangesCount} Breaking`
                                        : 'View Changes'}
                            </button>
                            <button
                                    onClick={handlePull}
                                    className="rounded-md bg-blue-600 px-4 py-2 text-sm font-semibold text-white shadow-sm hover:bg-blue-500"
                            >
                                Pull Now
                            </button>
                        </div>
                    </div>
                    <div className="mt-4 flex items-center space-x-4">
                        <span className="text-sm text-gray-500">URL: </span>
                        <a
                                href={service.openApiUrl}
                                target="_blank"
                                rel="noopener noreferrer"
                                className="text-blue-600 hover:text-blue-800"
                        >
                            {service.openApiUrl}
                        </a>
                        <span
                                className={`inline-flex rounded-full px-2 text-xs font-semibold leading-5 ${
                                        service.enabled
                                                ? 'bg-green-100 text-green-800'
                                                : 'bg-gray-100 text-gray-800'
                                }`}
                        >
                        {service.enabled ? 'Active' : 'Disabled'}
                    </span>
                    </div>
                </div>

                <div>
                    <div className="flex items-center justify-between mb-4">
                        <h2 className="text-lg font-semibold text-gray-900">
                            Versions ({versions.length})
                        </h2>
                        <p className="text-sm text-gray-500">
                            Click "View Spec" to view a specific version
                        </p>
                    </div>
                    <VersionList versions={versions} serviceId={serviceId}/>
                </div>
            </div>
    );
}
