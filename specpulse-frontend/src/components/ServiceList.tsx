import {memo} from 'react';
import {Link} from 'react-router-dom';
import type {Service} from '@/types';

interface ServiceListProps {
    services: Service[];
    onDelete: (id: number) => void;
    onPull: (id: number) => void;
    onEdit: (service: Service) => void;
    pullStatus: Record<number, 'loading' | 'success' | 'error' | undefined>;
    selectedServiceIds?: number[];
    onToggleSelectService?: (id: number) => void;
}

const ServiceList = memo(function ServiceList({
                                                  services,
                                                  onDelete,
                                                  onPull,
                                                  onEdit,
                                                  pullStatus,
                                                  selectedServiceIds = [],
                                                  onToggleSelectService,
                                              }: ServiceListProps) {
    if (services.length === 0) {
        return (
                <div className="text-center py-12" role="status" aria-label="No services available">
                    <p className="text-gray-500">No services registered</p>
                </div>
        );
    }

    return (
            <div className="overflow-hidden shadow ring-1 ring-black ring-opacity-5 sm:rounded-lg">
                <table
                        className="min-w-full divide-y divide-gray-300"
                        role="table"
                        aria-label="Services table"
                >
                    <thead className="bg-gray-50">
                    <tr>
                        <th
                                scope="col"
                                className="relative px-6 py-3.5"
                        >
                            {onToggleSelectService && (
                                    <input
                                            type="checkbox"
                                            className="absolute left-4 top-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                                            checked={services.length > 0 && services.every((s) => selectedServiceIds.includes(s.id))}
                                            onChange={(e) => {
                                                if (e.target.checked) {
                                                    services.forEach((s) => onToggleSelectService(s.id));
                                                } else {
                                                    services.forEach((s) => onToggleSelectService(s.id));
                                                }
                                            }}
                                            aria-label="Select all services"
                                    />
                            )}
                        </th>
                        <th
                                scope="col"
                                className="py-3.5 pl-4 pr-3 text-left text-sm font-semibold text-gray-900 sm:pl-6"
                        >
                            Name
                        </th>
                        <th
                                scope="col"
                                className="px-3 py-3.5 text-left text-sm font-semibold text-gray-900"
                        >
                            OpenAPI URL
                        </th>
                        <th
                                scope="col"
                                className="px-3 py-3.5 text-left text-sm font-semibold text-gray-900"
                        >
                            Latest Version
                        </th>
                        <th
                                scope="col"
                                className="px-3 py-3.5 text-left text-sm font-semibold text-gray-900"
                        >
                            Group
                        </th>
                        <th
                                scope="col"
                                className="px-3 py-3.5 text-left text-sm font-semibold text-gray-900"
                        >
                            Status
                        </th>
                        <th scope="col" className="relative py-3.5 pl-3 pr-4 sm:pr-6">
                            <span className="sr-only">Actions</span>
                        </th>
                    </tr>
                    </thead>
                    <tbody className="divide-y divide-gray-200 bg-white">
                    {services.map((service) => (
                            <tr
                                    key={service.id}
                                    className={selectedServiceIds.includes(service.id) ? 'bg-blue-50' : ''}
                            >
                                <td className="relative whitespace-nowrap py-4 pl-3 pr-4 text-sm sm:pl-6">
                                    {onToggleSelectService && (
                                            <input
                                                    type="checkbox"
                                                    className="absolute left-4 top-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                                                    checked={selectedServiceIds.includes(service.id)}
                                                    onChange={() => onToggleSelectService(service.id)}
                                                    aria-label={`Select service ${service.name}`}
                                            />
                                    )}
                                </td>
                                <td className="whitespace-nowrap py-4 pl-4 pr-3 text-sm font-medium text-gray-900 sm:pl-6">
                                    <Link
                                            to={`/services/${service.id}`}
                                            className="text-blue-600 hover:text-blue-900"
                                    >
                                        {service.name}
                                    </Link>
                                </td>
                                <td className="whitespace-nowrap px-3 py-4 text-sm text-gray-500 max-w-xs truncate">
                                    <span title={service.openApiUrl}>{service.openApiUrl}</span>
                                </td>
                                <td className="whitespace-nowrap px-3 py-4 text-sm">
                                    {service.latestVersion ? (
                                            <div className="flex flex-col">
                                        <span className="font-medium text-gray-900">
                                            {service.latestVersion.specTitle ||
                                                    `v${service.latestVersion.specVersion || '1.0'}`}
                                        </span>
                                                <span className="text-xs text-gray-500">
                                            {new Date(
                                                    service.latestVersion.pulledAt
                                            ).toLocaleDateString()}
                                        </span>
                                                <span className="text-xs font-mono text-gray-400">
                                            {service.latestVersion.versionHash.substring(0, 8)}...
                                        </span>
                                            </div>
                                    ) : (
                                            <span className="text-gray-400">No versions</span>
                                    )}
                                </td>
                                <td className="whitespace-nowrap px-3 py-4 text-sm">
                                    {service.group ? (
                                            <div className="flex items-center gap-2">
                                                {service.group.icon && (
                                                        <span>{service.group.icon}</span>
                                                )}
                                                {service.group.color && (
                                                        <span
                                                                className="w-3 h-3 rounded-full"
                                                                style={{backgroundColor: service.group.color}}
                                                        />
                                                )}
                                                <span className="text-gray-900">{service.group.name}</span>
                                            </div>
                                    ) : (
                                            <span className="text-gray-400">No group</span>
                                    )}
                                </td>
                                <td className="whitespace-nowrap px-3 py-4 text-sm">
                                <span
                                        className={`inline-flex rounded-full px-2 text-xs font-semibold leading-5 ${
                                                service.enabled
                                                        ? 'bg-green-100 text-green-800'
                                                        : 'bg-gray-100 text-gray-800'
                                        }`}
                                        role="status"
                                >
                                    {service.enabled ? 'Active' : 'Disabled'}
                                </span>
                                </td>
                                <td className="relative whitespace-nowrap py-4 pl-3 pr-4 text-right text-sm font-medium sm:pr-6">
                                    <div
                                            className="flex justify-end gap-2"
                                            role="group"
                                            aria-label={`Actions for ${service.name}`}
                                    >
                                        <button
                                                onClick={() => onEdit(service)}
                                                className="text-gray-600 hover:text-gray-900"
                                                title="Edit service"
                                                aria-label={`Edit service ${service.name}`}
                                        >
                                            <svg
                                                    className="w-5 h-5"
                                                    fill="none"
                                                    stroke="currentColor"
                                                    viewBox="0 0 24 24"
                                                    aria-hidden="true"
                                            >
                                                <path
                                                        strokeLinecap="round"
                                                        strokeLinejoin="round"
                                                        strokeWidth={2}
                                                        d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"
                                                />
                                            </svg>
                                        </button>
                                        <button
                                                onClick={() => onPull(service.id)}
                                                disabled={pullStatus[service.id] === 'loading'}
                                                className={`text-sm font-semibold ${
                                                        pullStatus[service.id] === 'loading'
                                                                ? 'text-gray-400 cursor-not-allowed'
                                                                : pullStatus[service.id] === 'success'
                                                                        ? 'text-green-600 hover:text-green-900'
                                                                        : pullStatus[service.id] === 'error'
                                                                                ? 'text-red-600 hover:text-red-900'
                                                                                : 'text-blue-600 hover:text-blue-900'
                                                }`}
                                                aria-label={`Pull service ${service.name}`}
                                                aria-busy={pullStatus[service.id] === 'loading'}
                                        >
                                            {pullStatus[service.id] === 'loading'
                                                    ? 'Pulling...'
                                                    : 'Pull'}
                                        </button>
                                        <button
                                                onClick={() => onDelete(service.id)}
                                                className="text-red-600 hover:text-red-900"
                                                title="Delete service"
                                                aria-label={`Delete service ${service.name}`}
                                        >
                                            <svg
                                                    className="w-5 h-5"
                                                    fill="none"
                                                    stroke="currentColor"
                                                    viewBox="0 0 24 24"
                                                    aria-hidden="true"
                                            >
                                                <path
                                                        strokeLinecap="round"
                                                        strokeLinejoin="round"
                                                        strokeWidth={2}
                                                        d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"
                                                />
                                            </svg>
                                        </button>
                                    </div>
                                </td>
                            </tr>
                    ))}
                    </tbody>
                </table>
            </div>
    );
});

export default ServiceList;
