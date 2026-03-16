import {useState} from 'react';
import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query';
import {settingsApi} from '@/services/api';
import {logger} from '@/utils/logger';
import type {SettingValue} from '@/types';
import SettingField from '@/components/SettingField';

const categoryIcons: Record<string, string> = {
    general: '⚙️',
    scheduler: '⏰',
    pull: '🔄',
    notifications: '🔔',
    security: '🔒',
    audit: '📋',
};

export default function SettingsPage() {
    const [selectedCategory, setSelectedCategory] = useState<string>('general');
    const [editingSetting, setEditingSetting] = useState<{
        key: string;
        value: SettingValue;
    } | null>(null);
    const queryClient = useQueryClient();

    const {data: categories = [], isLoading} = useQuery({
        queryKey: ['settings'],
        queryFn: async () => {
            const response = await settingsApi.getAll();
            return response.data;
        },
    });

    const updateMutation = useMutation({
        mutationFn: async ({
                               category,
                               key,
                               value,
                           }: {
            category: string;
            key: string;
            value: SettingValue;
        }) => {
            const response = await settingsApi.update(category, key, value);
            return response.data;
        },
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['settings']});
            setEditingSetting(null);
        },
        onError: (error: Error) => {
            logger.error('Failed to update setting:', error);
            alert(`Failed to update setting: ${error.message}`);
        },
    });

    const handleSave = (category: string, key: string, value: SettingValue): void => {
        updateMutation.mutate({category, key, value});
    };

    const selectedCategoryData = categories.find((c) => c.name === selectedCategory);

    if (isLoading) {
        return <div className="text-center py-12">Loading settings...</div>;
    }

    return (
            <div>
                <div className="sm:flex sm:items-center">
                    <div className="sm:flex-auto">
                        <h1 className="text-2xl font-semibold text-gray-900">Settings</h1>
                        <p className="mt-2 text-sm text-gray-700">
                            Configure application settings, scheduler, notifications, and security
                        </p>
                    </div>
                </div>

                <div className="mt-8 grid grid-cols-1 gap-6 lg:grid-cols-4">
                    {/* Categories Sidebar */}
                    <div className="lg:col-span-1">
                        <div className="overflow-hidden shadow ring-1 ring-black ring-opacity-5 sm:rounded-lg">
                            <nav className="divide-y divide-gray-200" aria-label="Settings categories">
                                {categories.map((category) => (
                                        <button
                                                key={category.name}
                                                onClick={() => setSelectedCategory(category.name)}
                                                className={`group flex w-full items-center justify-between px-4 py-4 text-sm font-medium hover:bg-gray-50 ${
                                                        selectedCategory === category.name
                                                                ? 'bg-blue-50 text-blue-600'
                                                                : 'text-gray-900'
                                                }`}
                                        >
                                            <div className="flex items-center gap-3">
                                        <span className="text-xl">
                                            {categoryIcons[category.name] || '📁'}
                                        </span>
                                                <span>{category.displayName}</span>
                                            </div>
                                            {category.settings.length > 0 && (
                                                    <span
                                                            className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${
                                                                    selectedCategory === category.name
                                                                            ? 'bg-blue-100 text-blue-800'
                                                                            : 'bg-gray-100 text-gray-800'
                                                            }`}
                                                    >
                                            {category.settings.length}
                                        </span>
                                            )}
                                        </button>
                                ))}
                            </nav>
                        </div>

                        {/* Info Card */}
                        <div className="mt-6 rounded-md bg-blue-50 p-4">
                            <div className="flex">
                                <div className="flex-shrink-0">
                                    <svg
                                            className="h-5 w-5 text-blue-400"
                                            fill="currentColor"
                                            viewBox="0 0 20 20"
                                    >
                                        <path
                                                fillRule="evenodd"
                                                d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z"
                                                clipRule="evenodd"
                                        />
                                    </svg>
                                </div>
                                <div className="ml-3 flex-1 md:flex md:justify-between">
                                    <p className="text-sm text-blue-700">
                                        Changes are applied immediately. System settings cannot be
                                        modified.
                                    </p>
                                </div>
                            </div>
                        </div>
                    </div>

                    {/* Settings Content */}
                    <div className="lg:col-span-3">
                        <div className="overflow-hidden shadow ring-1 ring-black ring-opacity-5 sm:rounded-lg">
                            <div className="bg-gray-50 px-4 py-3 border-b border-gray-200">
                                <div className="flex items-center gap-3">
                                <span className="text-2xl">
                                    {categoryIcons[selectedCategory] || '📁'}
                                </span>
                                    <div>
                                        <h2 className="text-lg font-semibold text-gray-900">
                                            {selectedCategoryData?.displayName}
                                        </h2>
                                        {selectedCategoryData?.description && (
                                                <p className="text-sm text-gray-500">
                                                    {selectedCategoryData.description}
                                                </p>
                                        )}
                                    </div>
                                </div>
                            </div>
                            <div className="bg-white px-4">
                                {selectedCategoryData?.settings &&
                                selectedCategoryData.settings.length > 0 ? (
                                        <div>
                                            {selectedCategoryData.settings.map((setting) => (
                                                    <SettingField
                                                            key={setting.id}
                                                            setting={setting}
                                                            value={
                                                                editingSetting?.key === setting.key
                                                                        ? editingSetting.value
                                                                        : setting.value
                                                            }
                                                            onChange={(newValue) =>
                                                                    setEditingSetting({
                                                                        key: setting.key,
                                                                        value: newValue
                                                                    })
                                                            }
                                                            onSave={() => {
                                                                if (editingSetting) {
                                                                    handleSave(setting.category, setting.key, editingSetting.value);
                                                                }
                                                            }}
                                                            onCancel={() => setEditingSetting(null)}
                                                            isSaving={updateMutation.isPending}
                                                    />
                                            ))}
                                        </div>
                                ) : (
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
                                                        d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"
                                                />
                                                <path
                                                        strokeLinecap="round"
                                                        strokeLinejoin="round"
                                                        strokeWidth={2}
                                                        d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"
                                                />
                                            </svg>
                                            <p className="mt-4 text-gray-500">No settings in this category</p>
                                        </div>
                                )}
                            </div>
                        </div>
                    </div>
                </div>
            </div>
    );
}
