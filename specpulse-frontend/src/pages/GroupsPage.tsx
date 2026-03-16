import {useState} from 'react';
import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query';
import {groupsApi} from '@/services/api';
import type {CreateGroupRequest, ServiceGroup} from '@/types';
import GroupTree from '@/components/GroupTree';
import GroupForm from '@/components/GroupForm';

export default function GroupsPage() {
    const [selectedGroup, setSelectedGroup] = useState<ServiceGroup | null>(null);
    const [showForm, setShowForm] = useState(false);
    const [editingGroup, setEditingGroup] = useState<ServiceGroup | null>(null);
    const queryClient = useQueryClient();

    const {data: groups = [], isLoading} = useQuery({
        queryKey: ['groups'],
        queryFn: async () => {
            const response = await groupsApi.getAll();
            return response.data;
        },
    });

    // Fetch detailed group info when a group is selected
    const {data: selectedGroupDetails} = useQuery({
        queryKey: ['group', selectedGroup?.id],
        queryFn: async () => {
            if (!selectedGroup?.id) return null;
            const response = await groupsApi.getById(selectedGroup.id, false);
            return response.data;
        },
        enabled: !!selectedGroup?.id,
    });

    const createMutation = useMutation({
        mutationFn: async (data: CreateGroupRequest) => {
            const response = await groupsApi.create(data);
            return response.data;
        },
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['groups']});
            setShowForm(false);
        },
    });

    const updateMutation = useMutation({
        mutationFn: async ({id, data}: { id: number; data: CreateGroupRequest }) => {
            const response = await groupsApi.update(id, data);
            return response.data;
        },
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['groups']});
            setShowForm(false);
            setEditingGroup(null);
        },
    });

    const deleteMutation = useMutation({
        mutationFn: async (id: number) => {
            await groupsApi.delete(id);
        },
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['groups']});
            setSelectedGroup(null);
        },
    });

    const handleCreate = (data: CreateGroupRequest) => {
        createMutation.mutate(data);
    };

    const handleUpdate = (data: CreateGroupRequest) => {
        if (editingGroup) {
            updateMutation.mutate({id: editingGroup.id, data});
        }
    };

    const handleEdit = (group: ServiceGroup) => {
        setEditingGroup(group);
        setShowForm(true);
    };

    const handleDelete = (group: ServiceGroup) => {
        deleteMutation.mutate(group.id);
    };

    if (isLoading) {
        return <div className="text-center py-12">Loading...</div>;
    }

    return (
            <div>
                <div className="sm:flex sm:items-center">
                    <div className="sm:flex-auto">
                        <h1 className="text-2xl font-semibold text-gray-900">Service Groups</h1>
                        <p className="mt-2 text-sm text-gray-700">
                            Organize your services into logical groups (environments, domains, teams, etc.)
                        </p>
                    </div>
                    <div className="mt-4 sm:ml-16 sm:mt-0 sm:flex-none">
                        <button
                                type="button"
                                onClick={() => {
                                    setEditingGroup(null);
                                    setShowForm(true);
                                }}
                                className="inline-block rounded-md bg-blue-600 px-3 py-2 text-center text-sm font-semibold text-white shadow-sm hover:bg-blue-500 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-600"
                        >
                            Create Group
                        </button>
                    </div>
                </div>

                <div className="mt-8 grid grid-cols-1 gap-6 lg:grid-cols-3">
                    {/* Groups Tree */}
                    <div className="lg:col-span-2">
                        <div className="overflow-hidden shadow ring-1 ring-black ring-opacity-5 sm:rounded-lg">
                            <div className="bg-gray-50 px-4 py-3 border-b border-gray-200">
                                <h2 className="text-sm font-semibold text-gray-900">Groups</h2>
                            </div>
                            <div className="bg-white p-4">
                                {groups.length === 0 ? (
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
                                                        d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z"
                                                />
                                            </svg>
                                            <p className="mt-4 text-gray-500">No groups yet</p>
                                            <p className="mt-2 text-sm text-gray-400">
                                                Create your first group to organize services
                                            </p>
                                        </div>
                                ) : (
                                        <GroupTree
                                                groups={groups}
                                                selectedGroupId={selectedGroup?.id || null}
                                                onSelectGroup={setSelectedGroup}
                                                onEditGroup={handleEdit}
                                                onDeleteGroup={handleDelete}
                                        />
                                )}
                            </div>
                        </div>
                    </div>

                    {/* Group Details */}
                    <div>
                        <div className="overflow-hidden shadow ring-1 ring-black ring-opacity-5 sm:rounded-lg">
                            <div className="bg-gray-50 px-4 py-3 border-b border-gray-200">
                                <h2 className="text-sm font-semibold text-gray-900">Group Details</h2>
                            </div>
                            <div className="bg-white p-4">
                                {selectedGroupDetails ? (
                                        <div className="space-y-4">
                                            <div>
                                                <div className="flex items-center gap-2">
                                                    {selectedGroupDetails.icon && (
                                                            <span className="text-2xl">
                                                    {selectedGroupDetails.icon}
                                                </span>
                                                    )}
                                                    {selectedGroupDetails.color && (
                                                            <span
                                                                    className="w-4 h-4 rounded-full"
                                                                    style={{
                                                                        backgroundColor: selectedGroupDetails.color,
                                                                    }}
                                                            />
                                                    )}
                                                    <h3 className="text-lg font-semibold text-gray-900">
                                                        {selectedGroupDetails.name}
                                                    </h3>
                                                </div>
                                                {selectedGroupDetails.parentGroupName && (
                                                        <p className="text-sm text-gray-500 mt-1">
                                                            Parent:{' '}
                                                            <span className="font-medium">
                                                    {selectedGroupDetails.parentGroupName}
                                                </span>
                                                        </p>
                                                )}
                                            </div>

                                            {selectedGroupDetails.description && (
                                                    <div>
                                                        <h4 className="text-sm font-medium text-gray-700">
                                                            Description
                                                        </h4>
                                                        <p className="mt-1 text-sm text-gray-600">
                                                            {selectedGroupDetails.description}
                                                        </p>
                                                    </div>
                                            )}

                                            <div className="pt-4 border-t border-gray-200">
                                                <div className="flex items-center justify-between">
                                                    <span className="text-sm text-gray-500">Services</span>
                                                    <span className="text-sm font-medium text-gray-900">
                                                {selectedGroupDetails.serviceCount || 0}
                                            </span>
                                                </div>
                                            </div>

                                            <div className="pt-4 border-t border-gray-200">
                                                <div className="flex items-center justify-between">
                                                    <span className="text-sm text-gray-500">Created</span>
                                                    <span className="text-sm text-gray-900">
                                                {new Date(
                                                        selectedGroupDetails.createdAt
                                                ).toLocaleDateString()}
                                            </span>
                                                </div>
                                            </div>

                                            <div className="pt-4 border-t border-gray-200">
                                                <button
                                                        onClick={() => handleEdit(selectedGroupDetails)}
                                                        className="w-full rounded-md bg-blue-600 px-4 py-2 text-sm font-semibold text-white shadow-sm hover:bg-blue-500"
                                                >
                                                    Edit Group
                                                </button>
                                            </div>
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
                                                        d="M15 15l-2 5L9 9l11 4-5 2zm0 0l5 5M7.188 2.239l.777 2.897M5.136 7.965l-2.898-.777M13.95 4.05l-2.122 2.122m-5.657 5.656l-2.12 2.122"
                                                />
                                            </svg>
                                            <p className="mt-4 text-gray-500">Select a group</p>
                                            <p className="mt-2 text-sm text-gray-400">
                                                Click on a group to view details
                                            </p>
                                        </div>
                                )}
                            </div>
                        </div>
                    </div>
                </div>

                {showForm && (
                        <GroupForm
                                onSubmit={editingGroup ? handleUpdate : handleCreate}
                                onCancel={() => {
                                    setShowForm(false);
                                    setEditingGroup(null);
                                }}
                                initialData={editingGroup}
                        />
                )}
            </div>
    );
}
