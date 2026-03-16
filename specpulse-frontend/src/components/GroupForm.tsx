import {useEffect, useState} from 'react';
import {useQuery} from '@tanstack/react-query';
import {groupsApi} from '@/services/api';
import type {CreateGroupRequest, ServiceGroup} from '@/types';

interface GroupFormProps {
    onSubmit: (data: CreateGroupRequest) => void;
    onCancel: () => void;
    initialData?: ServiceGroup | null;
}

export default function GroupForm({onSubmit, onCancel, initialData}: GroupFormProps) {
    const [name, setName] = useState('');
    const [description, setDescription] = useState('');
    const [parentGroupId, setParentGroupId] = useState<number | undefined>(undefined);
    const [color, setColor] = useState('');
    const [icon, setIcon] = useState('');

    const {data: groups = []} = useQuery({
        queryKey: ['groups'],
        queryFn: async () => {
            const response = await groupsApi.getAll();
            return response.data;
        },
    });

    useEffect(() => {
        if (initialData) {
            setName(initialData.name);
            setDescription(initialData.description || '');
            setParentGroupId(initialData.parentGroupId);
            setColor(initialData.color || '');
            setIcon(initialData.icon || '');
        }
    }, [initialData]);

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        onSubmit({
            name,
            description: description || undefined,
            parentGroupId,
            color: color || undefined,
            icon: icon || undefined,
            sortOrder: 0,
        });
    };

    // Filter out current group and its descendants from parent options
    const availableParents = initialData
            ? groups.filter((g) => g.id !== initialData.id)
            : groups;

    return (
            <div className="fixed inset-0 bg-gray-500 bg-opacity-75 transition-opacity">
                <div className="fixed inset-0 z-10 overflow-y-auto">
                    <div className="flex min-h-full items-end justify-center p-4 text-center sm:items-center sm:p-0">
                        <div className="relative transform overflow-hidden rounded-lg bg-white px-4 pb-4 pt-5 text-left shadow-xl transition-all sm:my-8 sm:w-full sm:max-w-lg sm:p-6">
                            <form onSubmit={handleSubmit}>
                                <h3 className="text-lg font-semibold leading-6 text-gray-900 mb-4">
                                    {initialData ? 'Edit Group' : 'Create New Group'}
                                </h3>

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
                                                onChange={(e) => setName(e.target.value)}
                                                className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm border p-2"
                                                placeholder="production"
                                        />
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

                                    <div>
                                        <label
                                                htmlFor="parentGroup"
                                                className="block text-sm font-medium text-gray-700"
                                        >
                                            Parent Group
                                        </label>
                                        <select
                                                id="parentGroup"
                                                value={parentGroupId || ''}
                                                onChange={(e) =>
                                                        setParentGroupId(
                                                                e.target.value ? Number(e.target.value) : undefined
                                                        )
                                                }
                                                className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm border p-2"
                                        >
                                            <option value="">None (root level)</option>
                                            {availableParents.map((group) => (
                                                    <option key={group.id} value={group.id}>
                                                        {group.name}
                                                    </option>
                                            ))}
                                        </select>
                                    </div>

                                    <div className="grid grid-cols-2 gap-4">
                                        <div>
                                            <label
                                                    htmlFor="color"
                                                    className="block text-sm font-medium text-gray-700"
                                            >
                                                Color
                                            </label>
                                            <div className="mt-1 flex gap-2">
                                                <input
                                                        type="color"
                                                        id="color"
                                                        value={color || '#3B82F6'}
                                                        onChange={(e) => setColor(e.target.value)}
                                                        className="h-9 w-16 rounded border-gray-300"
                                                />
                                                <input
                                                        type="text"
                                                        value={color}
                                                        onChange={(e) => setColor(e.target.value)}
                                                        className="flex-1 rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm border p-2"
                                                        placeholder="#3B82F6"
                                                />
                                            </div>
                                        </div>

                                        <div>
                                            <label
                                                    htmlFor="icon"
                                                    className="block text-sm font-medium text-gray-700"
                                            >
                                                Icon (emoji)
                                            </label>
                                            <input
                                                    type="text"
                                                    id="icon"
                                                    value={icon}
                                                    onChange={(e) => setIcon(e.target.value)}
                                                    className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm border p-2"
                                                    placeholder="📁"
                                            />
                                        </div>
                                    </div>
                                </div>

                                <div className="mt-6 flex justify-end space-x-3">
                                    <button
                                            type="button"
                                            onClick={onCancel}
                                            className="rounded-md bg-white px-4 py-2 text-sm font-semibold text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 hover:bg-gray-50"
                                    >
                                        Cancel
                                    </button>
                                    <button
                                            type="submit"
                                            className="rounded-md bg-blue-600 px-4 py-2 text-sm font-semibold text-white shadow-sm hover:bg-blue-500"
                                    >
                                        {initialData ? 'Save Changes' : 'Create Group'}
                                    </button>
                                </div>
                            </form>
                        </div>
                    </div>
                </div>
            </div>
    );
}
