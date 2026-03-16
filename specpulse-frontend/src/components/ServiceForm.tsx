import {useEffect, useState} from 'react';
import type {CreateServiceRequest, Service} from '@/types';
import GroupSelector from './GroupSelector';
import {logger} from '@/utils/logger';

interface ServiceFormProps {
    onSubmit: (data: CreateServiceRequest) => void;
    onCancel: () => void;
    initialData?: Service;
}

export default function ServiceForm({onSubmit, onCancel, initialData}: ServiceFormProps) {
    const [name, setName] = useState('');
    const [openApiUrl, setOpenApiUrl] = useState('');
    const [description, setDescription] = useState('');
    const [enabled, setEnabled] = useState(true);
    const [groupId, setGroupId] = useState<number | null>(null);
    const [isValidation, setIsValidation] = useState(false);
    const [validationErrors, setValidationErrors] = useState<string[]>([]);
    const [validationSuccess, setValidationSuccess] = useState(false);

    useEffect(() => {
        if (initialData) {
            setName(initialData.name);
            setOpenApiUrl(initialData.openApiUrl);
            setDescription(initialData.description || '');
            setEnabled(initialData.enabled);
            setGroupId(initialData.groupId || null);
            setValidationSuccess(true); // Already saved, so valid
        }
    }, [initialData]);

    const validateOpenApiSpec = async (): Promise<boolean> => {
        if (!name || !openApiUrl) {
            setValidationErrors(['Please fill in Name and OpenAPI URL fields']);
            return false;
        }

        setIsValidation(true);
        setValidationErrors([]);
        setValidationSuccess(false);

        try {
            const response = await fetch('/api/v1/registry/validate', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({name, openApiUrl}),
            });

            const result = await response.json();

            if (!result.valid) {
                setValidationErrors(result.errors);
                setIsValidation(false);
                return false;
            }

            setValidationSuccess(true);
            setIsValidation(false);
            return true;
        } catch (error) {
            setValidationErrors(['Failed to validate OpenAPI spec. Please check the URL.']);
            setIsValidation(false);
            return false;
        }
    };

    const handleValidateClick = async () => {
        await validateOpenApiSpec();
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        // Skip validation when editing existing service
        if (!initialData) {
            const isValid = await validateOpenApiSpec();
            if (!isValid) {
                return;
            }
        }

        // Ensure groupId is null or number, not undefined
        const finalGroupId = groupId === undefined ? null : groupId;
        const requestData = {name, openApiUrl, description, enabled, groupId: finalGroupId};
        logger.debug('[ServiceForm] Submitting data:', requestData);
        onSubmit(requestData);
    };

    return (
            <div className="fixed inset-0 bg-gray-500 bg-opacity-75 transition-opacity">
                <div className="fixed inset-0 z-10 overflow-y-auto">
                    <div className="flex min-h-full items-end justify-center p-4 text-center sm:items-center sm:p-0">
                        <div className="relative transform overflow-hidden rounded-lg bg-white px-4 pb-4 pt-5 text-left shadow-xl transition-all sm:my-8 sm:w-full sm:max-w-lg sm:p-6">
                            <form onSubmit={handleSubmit}>
                                <h3 className="text-lg font-semibold leading-6 text-gray-900 mb-4">
                                    {initialData ? 'Edit Service' : 'Add New Service'}
                                </h3>

                                {/* Validation Errors */}
                                {validationErrors.length > 0 && (
                                        <div className="mb-4 rounded-md bg-red-50 p-4">
                                            <div className="flex">
                                                <div className="flex-shrink-0">
                                                    <svg
                                                            className="h-5 w-5 text-red-400"
                                                            fill="currentColor"
                                                            viewBox="0 0 20 20"
                                                    >
                                                        <path
                                                                fillRule="evenodd"
                                                                d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z"
                                                                clipRule="evenodd"
                                                        />
                                                    </svg>
                                                </div>
                                                <div className="ml-3">
                                                    <h3 className="text-sm font-medium text-red-800">
                                                        OpenAPI Validation Failed
                                                    </h3>
                                                    <ul className="mt-2 text-sm text-red-700 list-disc list-inside space-y-1">
                                                        {validationErrors.map((error, idx) => (
                                                                <li key={idx}>{error}</li>
                                                        ))}
                                                    </ul>
                                                </div>
                                            </div>
                                        </div>
                                )}

                                {/* Validation Success */}
                                {validationSuccess && (
                                        <div className="mb-4 rounded-md bg-green-50 p-4">
                                            <div className="flex">
                                                <div className="flex-shrink-0">
                                                    <svg
                                                            className="h-5 w-5 text-green-400"
                                                            fill="currentColor"
                                                            viewBox="0 0 20 20"
                                                    >
                                                        <path
                                                                fillRule="evenodd"
                                                                d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z"
                                                                clipRule="evenodd"
                                                        />
                                                    </svg>
                                                </div>
                                                <div className="ml-3">
                                                    <p className="text-sm font-medium text-green-800">
                                                        OpenAPI specification is valid!
                                                    </p>
                                                </div>
                                            </div>
                                        </div>
                                )}

                                <div className="space-y-4">
                                    <div>
                                        <label
                                                htmlFor="name"
                                                className="block text-sm font-medium text-gray-700"
                                        >
                                            Name *
                                        </label>
                                        <input
                                                type="text"
                                                id="name"
                                                required
                                                value={name}
                                                onChange={(e) => {
                                                    setName(e.target.value);
                                                    setValidationSuccess(false);
                                                }}
                                                className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm border p-2"
                                                placeholder="my-service"
                                        />
                                    </div>

                                    <div>
                                        <label
                                                htmlFor="openApiUrl"
                                                className="block text-sm font-medium text-gray-700"
                                        >
                                            OpenAPI URL *
                                        </label>
                                        <div className="flex gap-2">
                                            <input
                                                    type="url"
                                                    id="openApiUrl"
                                                    required
                                                    value={openApiUrl}
                                                    onChange={(e) => {
                                                        setOpenApiUrl(e.target.value);
                                                        setValidationSuccess(false);
                                                    }}
                                                    className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm border p-2"
                                                    placeholder="https://api.example.com/openapi.json"
                                            />
                                            {!initialData && (
                                                    <button
                                                            type="button"
                                                            onClick={handleValidateClick}
                                                            disabled={isValidation || !openApiUrl}
                                                            className={`mt-1 px-3 py-2 rounded-md text-sm font-semibold whitespace-nowrap ${
                                                                    isValidation
                                                                            ? 'bg-gray-100 text-gray-400 cursor-not-allowed'
                                                                            : validationSuccess
                                                                                    ? 'bg-green-100 text-green-700 hover:bg-green-200'
                                                                                    : 'bg-blue-100 text-blue-700 hover:bg-blue-200'
                                                            } disabled:opacity-50 disabled:cursor-not-allowed`}
                                                    >
                                                        {isValidation
                                                                ? 'Checking...'
                                                                : validationSuccess
                                                                        ? '✓ Valid'
                                                                        : 'Validate'}
                                                    </button>
                                            )}
                                        </div>
                                        <p className="mt-1 text-xs text-gray-500">
                                            OpenAPI 3.x specification URL (Swagger 2.0 is not supported)
                                        </p>
                                    </div>

                                    <div>
                                        <label
                                                htmlFor="description"
                                                className="block text-sm font-medium text-gray-700"
                                        >
                                            Description
                                        </label>
                                        <textarea
                                                id="description"
                                                value={description}
                                                onChange={(e) => setDescription(e.target.value)}
                                                rows={3}
                                                className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm border p-2"
                                        />
                                    </div>

                                    <div className="flex items-center">
                                        <input
                                                type="checkbox"
                                                id="enabled"
                                                checked={enabled}
                                                onChange={(e) => setEnabled(e.target.checked)}
                                                className="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                                        />
                                        <label
                                                htmlFor="enabled"
                                                className="ml-2 block text-sm text-gray-700"
                                        >
                                            Enabled (pull automatically)
                                        </label>
                                    </div>

                                    <GroupSelector
                                            value={groupId}
                                            onChange={setGroupId}
                                            label="Service Group"
                                    />
                                </div>

                                <div className="mt-6 flex justify-end space-x-3">
                                    <button
                                            type="button"
                                            onClick={onCancel}
                                            disabled={isValidation}
                                            className="rounded-md bg-white px-4 py-2 text-sm font-semibold text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
                                    >
                                        Cancel
                                    </button>
                                    <button
                                            type="submit"
                                            disabled={isValidation}
                                            className="rounded-md bg-blue-600 px-4 py-2 text-sm font-semibold text-white shadow-sm hover:bg-blue-500 disabled:opacity-50 disabled:cursor-not-allowed"
                                    >
                                        {isValidation
                                                ? 'Validating...'
                                                : initialData
                                                        ? 'Save Changes'
                                                        : 'Save'}
                                    </button>
                                </div>
                            </form>
                        </div>
                    </div>
                </div>
            </div>
    );
}
