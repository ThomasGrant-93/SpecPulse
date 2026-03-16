import {useState} from 'react';
import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query';
import {pullApi, registryApi} from '@/services/api';
import type {CreateServiceRequest, Service} from '@/types';
import ServiceList from '@/components/ServiceList';
import ServiceForm from '@/components/ServiceForm';
import SearchWithSuggestions from '@/components/SearchWithSuggestions';
import BulkGroupAssignment from '@/components/BulkGroupAssignment';
import {useDebounce, usePagination} from '@/hooks';
import {logger} from '@/utils/logger';

export default function ServicesPage() {
    const [showForm, setShowForm] = useState(false);
    const [editingService, setEditingService] = useState<Service | undefined>(undefined);
    const [pullStatus, setPullStatus] = useState<Record<number, 'loading' | 'success' | 'error'>>(
            {}
    );
    const [searchQuery, setSearchQuery] = useState('');
    const [selectedServiceIds, setSelectedServiceIds] = useState<number[]>([]);
    const [showBulkAssign, setShowBulkAssign] = useState(false);
    const queryClient = useQueryClient();

    // Debounce search query to avoid excessive API calls
    const {debouncedValue: debouncedSearchQuery} = useDebounce(searchQuery, {delay: 300});

    const {
        data: services = [],
        isLoading,
        error,
    } = useQuery({
        queryKey: ['services', debouncedSearchQuery],
        queryFn: async () => {
            if (debouncedSearchQuery) {
                const response = await fetch(
                        `/api/v1/registry/search?q=${encodeURIComponent(debouncedSearchQuery)}`
                );
                return response.json();
            }
            const response = await registryApi.getAll();
            return response.data;
        },
    });

    const pagination = usePagination(services, {initialPage: 1, initialPageSize: 10});
    const paginatedServices = pagination.data as typeof services;
    const {page, totalPages, totalItems, nextPage, previousPage, setPageSize} = pagination;

    const createMutation = useMutation({
        mutationFn: async (data: CreateServiceRequest) => {
            // Ensure groupId is explicitly included in the request
            const requestData = {
                name: data.name,
                openApiUrl: data.openApiUrl,
                description: data.description,
                enabled: data.enabled ?? true,
                groupId: data.groupId ?? null,
            };
            logger.debug('[ServicesPage] createMutation sending:', requestData);
            const response = await registryApi.create(requestData);
            logger.debug('[ServicesPage] createMutation response:', response.data);
            return response.data;
        },
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['services'], exact: false});
            queryClient.invalidateQueries({queryKey: ['groups'], exact: false});
            setShowForm(false);
            setEditingService(undefined);
        },
        onError: (error: Error) => {
            logger.error('[ServicesPage] createMutation error:', error);
            alert(`Failed to create service: ${error.message}`);
        },
    });

    const updateMutation = useMutation({
        mutationFn: async ({id, data}: { id: number; data: CreateServiceRequest }) => {
            // Ensure groupId is explicitly included in the request
            const requestData = {
                name: data.name,
                openApiUrl: data.openApiUrl,
                description: data.description,
                enabled: data.enabled,
                groupId: data.groupId ?? null,
            };
            logger.debug('[ServicesPage] updateMutation sending:', requestData);
            const response = await registryApi.update(id, requestData);
            logger.debug('[ServicesPage] updateMutation response:', response.data);
            return response.data;
        },
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['services'], exact: false});
            queryClient.invalidateQueries({queryKey: ['groups'], exact: false});
            setShowForm(false);
            setEditingService(undefined);
        },
        onError: (error: Error) => {
            logger.error('[ServicesPage] updateMutation error:', error);
            alert(`Failed to update service: ${error.message}`);
        },
    });

    const deleteMutation = useMutation({
        mutationFn: async (id: number) => {
            await registryApi.delete(id);
        },
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['services']});
        },
    });

    const pullMutation = useMutation({
        mutationFn: async (serviceId: number) => {
            setPullStatus((prev) => ({...prev, [serviceId]: 'loading'}));
            try {
                const response = await pullApi.triggerService(serviceId);
                setPullStatus((prev) => ({
                    ...prev,
                    [serviceId]: response.data.hasChanges ? 'success' : 'success',
                }));
                queryClient.invalidateQueries({queryKey: ['services'], exact: false});
                return response.data;
            } catch (error) {
                setPullStatus((prev) => ({...prev, [serviceId]: 'error'}));
                throw error;
            }
        },
    });

    const handleEdit = (service: Service): void => {
        setEditingService(service);
        setShowForm(true);
    };

    const handleSubmit = (data: CreateServiceRequest): void => {
        logger.debug('[ServicesPage] handleSubmit called with:', data);
        if (editingService) {
            updateMutation.mutate({id: editingService.id, data});
        } else {
            logger.debug('[ServicesPage] Creating service with data:', data);
            createMutation.mutate(data);
        }
    };

    if (isLoading) {
        return <div className="text-center py-12">Loading...</div>;
    }

    if (error) {
        return (
                <div className="rounded-md bg-red-50 p-4">
                    <p className="text-red-800">Failed to load services</p>
                </div>
        );
    }

    return (
            <div>
                <div className="sm:flex sm:items-center">
                    <div className="sm:flex-auto">
                        <h1 className="text-2xl font-semibold text-gray-900">Services</h1>
                        <p className="mt-2 text-sm text-gray-700">
                            Registered services with OpenAPI specifications
                        </p>
                    </div>
                    <div className="mt-4 sm:ml-16 sm:mt-0 sm:flex-none space-x-2">
                        <button
                                type="button"
                                onClick={() => {
                                    setEditingService(undefined);
                                    setShowForm(true);
                                }}
                                className="inline-block rounded-md bg-blue-600 px-3 py-2 text-center text-sm font-semibold text-white shadow-sm hover:bg-blue-500 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-600"
                        >
                            Add Service
                        </button>
                        <button
                                type="button"
                                onClick={() => pullApi.triggerAll()}
                                className="inline-block rounded-md bg-green-600 px-3 py-2 text-center text-sm font-semibold text-white shadow-sm hover:bg-green-500 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-green-600"
                        >
                            Pull All
                        </button>
                        {selectedServiceIds.length > 0 && (
                                <button
                                        type="button"
                                        onClick={() => setShowBulkAssign(true)}
                                        className="inline-block rounded-md bg-purple-600 px-3 py-2 text-center text-sm font-semibold text-white shadow-sm hover:bg-purple-500 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-purple-600"
                                >
                                    Assign {selectedServiceIds.length} to Group
                                </button>
                        )}
                    </div>
                </div>

                {/* Search Bar */}
                <div className="mt-6">
                    <SearchWithSuggestions
                            value={searchQuery}
                            onChange={setSearchQuery}
                            onClear={() => setSearchQuery('')}
                            placeholder="Search services by name, description, or URL..."
                    />
                    {searchQuery && (
                            <div className="mt-2 flex items-center justify-between">
                                <p className="text-sm text-gray-600">
                                    Showing results for "<span className="font-medium">{searchQuery}</span>"
                                    {services.length === 0 && (
                                            <span className="text-red-600 ml-1">- No matches found</span>
                                    )}
                                </p>
                                <span className="text-sm text-gray-500">
                            {services.length} result{services.length !== 1 ? 's' : ''}
                        </span>
                            </div>
                    )}
                </div>

                <div className="mt-6">
                    <ServiceList
                            services={paginatedServices}
                            onDelete={deleteMutation.mutate}
                            onPull={pullMutation.mutate}
                            onEdit={handleEdit}
                            pullStatus={pullStatus}
                            selectedServiceIds={selectedServiceIds}
                            onToggleSelectService={(id: number) => {
                                setSelectedServiceIds((prev) =>
                                        prev.includes(id) ? prev.filter((sid) => sid !== id) : [...prev, id]
                                );
                            }}
                    />

                    {/* Pagination */}
                    {totalItems > 0 && (
                            <div className="mt-6 flex items-center justify-between border-t border-gray-200 pt-4">
                                <div className="flex items-center gap-4">
                                    <p className="text-sm text-gray-700">
                                        Showing <span className="font-medium">{(page - 1) * 10 + 1}</span>{' '}
                                        to{' '}
                                        <span className="font-medium">
                                    {Math.min(page * 10, totalItems)}
                                </span>{' '}
                                        of <span className="font-medium">{totalItems}</span> results
                                    </p>
                                    <select
                                            value={10}
                                            onChange={(e) => setPageSize(Number(e.target.value))}
                                            className="rounded-md border-gray-300 py-1 pl-2 pr-8 text-sm focus:border-blue-500 focus:ring-blue-500"
                                    >
                                        <option value={5}>5 / page</option>
                                        <option value={10}>10 / page</option>
                                        <option value={20}>20 / page</option>
                                        <option value={50}>50 / page</option>
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

                {showForm && (
                        <ServiceForm
                                onSubmit={handleSubmit}
                                onCancel={() => {
                                    setShowForm(false);
                                    setEditingService(undefined);
                                }}
                                initialData={editingService}
                        />
                )}

                {showBulkAssign && (
                        <BulkGroupAssignment
                                serviceIds={selectedServiceIds}
                                onSuccess={() => {
                                    setSelectedServiceIds([]);
                                    setShowBulkAssign(false);
                                }}
                                onCancel={() => {
                                    setSelectedServiceIds([]);
                                    setShowBulkAssign(false);
                                }}
                        />
                )}
            </div>
    );
}
