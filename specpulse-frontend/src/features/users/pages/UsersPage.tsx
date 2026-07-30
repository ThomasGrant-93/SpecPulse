import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Modal } from '@/components/Modal';
import { useAuth } from '@/features/auth/useAuth';
import { adminRolesApi, adminUsersApi } from '@/features/users/api';
import type { AdminRole, AdminUser, CreateAdminUserRequest, UpdateAdminUserRequest } from '@/types';
import { logger } from '@/utils/logger';

function toggleRoleNames(roleNames: string[], roleName: string): string[] {
    if (roleNames.includes(roleName)) {
        return roleNames.filter((n) => n !== roleName);
    }
    return [...roleNames, roleName];
}

export default function UsersPage() {
    const { isAdmin } = useAuth();
    const queryClient = useQueryClient();

    const [isCreateOpen, setIsCreateOpen] = useState(false);
    const [editingUser, setEditingUser] = useState<AdminUser | null>(null);
    const [createForm, setCreateForm] = useState<CreateAdminUserRequest>({
        username: '',
        email: '',
        password: '',
        enabled: true,
        roleNames: [],
    });
    const [editForm, setEditForm] = useState<UpdateAdminUserRequest>({
        email: '',
        enabled: true,
        roleNames: [],
    });

    const {
        data: users = [],
        isLoading: usersLoading,
        error: usersError,
    } = useQuery({
        queryKey: ['adminUsers'],
        queryFn: async () => {
            const response = await adminUsersApi.list();
            return response.data;
        },
    });

    const {
        data: roles = [],
        isLoading: rolesLoading,
        error: rolesError,
    } = useQuery({
        queryKey: ['adminRoles'],
        queryFn: async () => {
            const response = await adminRolesApi.listEnabled();
            return response.data;
        },
    });

    const enabledRoles = useMemo(() => roles.filter((r: AdminRole) => r.enabled), [roles]);

    const createMutation = useMutation({
        mutationFn: async (data: CreateAdminUserRequest) => {
            const response = await adminUsersApi.create(data);
            return response.data;
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['adminUsers'] });
            setIsCreateOpen(false);
            setCreateForm({ username: '', email: '', password: '', enabled: true, roleNames: [] });
        },
        onError: (error: Error) => {
            logger.error('[UsersPage] createMutation error:', error);
            alert(`Failed to create user: ${error.message}`);
        },
    });

    const updateMutation = useMutation({
        mutationFn: async ({ id, data }: { id: number; data: UpdateAdminUserRequest }) => {
            const response = await adminUsersApi.update(id, data);
            return response.data;
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['adminUsers'] });
            setEditingUser(null);
            setEditForm({ email: '', enabled: true, roleNames: [] });
        },
        onError: (error: Error) => {
            logger.error('[UsersPage] updateMutation error:', error);
            alert(`Failed to update user: ${error.message}`);
        },
    });

    const deleteMutation = useMutation({
        mutationFn: async (id: number) => {
            await adminUsersApi.delete(id);
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['adminUsers'] });
        },
        onError: (error: Error) => {
            logger.error('[UsersPage] deleteMutation error:', error);
            alert(`Failed to delete user: ${error.message}`);
        },
    });

    if (!isAdmin) {
        return (
            <div className="rounded-md bg-gray-100 p-4">
                <p className="text-gray-800">Not authorized.</p>
            </div>
        );
    }

    if (usersLoading || rolesLoading) {
        return <div className="text-center py-12">Loading...</div>;
    }

    if (usersError || rolesError) {
        return (
            <div className="rounded-md bg-red-50 border border-red-200 p-4">
                <p className="text-red-800">Failed to load users.</p>
            </div>
        );
    }

    return (
        <div>
            <div className="sm:flex sm:items-center sm:justify-between">
                <div>
                    <h1 className="text-2xl font-semibold text-gray-900">User Management</h1>
                    <p className="mt-2 text-sm text-gray-700">Manage users and their roles.</p>
                </div>
                <div className="mt-4 sm:mt-0">
                    <button
                        type="button"
                        onClick={() => setIsCreateOpen(true)}
                        className="inline-flex items-center rounded-md bg-blue-600 px-3 py-2 text-sm font-semibold text-white shadow-sm hover:bg-blue-500 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-600"
                    >
                        Add user
                    </button>
                </div>
            </div>

            <div className="mt-6 bg-white shadow rounded-lg overflow-hidden">
                <div className="px-4 py-3 border-b flex items-center justify-between">
                    <span className="text-sm font-medium text-gray-700">
                        Users ({users.length})
                    </span>
                </div>
                <div className="overflow-x-auto">
                    <table className="min-w-full divide-y divide-gray-200">
                        <thead className="bg-gray-50">
                            <tr>
                                <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">
                                    Username
                                </th>
                                <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">
                                    Email
                                </th>
                                <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">
                                    Enabled
                                </th>
                                <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">
                                    Roles
                                </th>
                                <th className="px-4 py-2 text-right text-xs font-medium text-gray-500 uppercase">
                                    Actions
                                </th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-200">
                            {users.map((user) => (
                                <tr key={user.id} className="hover:bg-gray-50">
                                    <td className="px-4 py-3">
                                        <span className="font-medium text-gray-900">
                                            {user.username}
                                        </span>
                                    </td>
                                    <td className="px-4 py-3">
                                        <span className="text-gray-700">{user.email}</span>
                                    </td>
                                    <td className="px-4 py-3">
                                        {user.enabled ? (
                                            <span className="inline-flex items-center rounded bg-green-100 px-2 py-1 text-xs font-semibold text-green-800">
                                                Yes
                                            </span>
                                        ) : (
                                            <span className="inline-flex items-center rounded bg-gray-100 px-2 py-1 text-xs font-semibold text-gray-700">
                                                No
                                            </span>
                                        )}
                                    </td>
                                    <td className="px-4 py-3">
                                        {user.roles.length > 0 ? (
                                            <div className="flex flex-wrap gap-1">
                                                {user.roles.map((r) => (
                                                    <span
                                                        key={r}
                                                        className="inline-flex items-center rounded bg-gray-100 px-2 py-1 text-xs font-medium text-gray-700"
                                                    >
                                                        {r}
                                                    </span>
                                                ))}
                                            </div>
                                        ) : (
                                            <span className="text-gray-500">—</span>
                                        )}
                                    </td>
                                    <td className="px-4 py-3 text-right">
                                        <div className="inline-flex items-center gap-2">
                                            <button
                                                type="button"
                                                onClick={() => {
                                                    setEditingUser(user);
                                                    setEditForm({
                                                        email: user.email,
                                                        enabled: user.enabled,
                                                        roleNames: user.roles,
                                                    });
                                                }}
                                                className="text-sm text-blue-600 hover:text-blue-800 underline"
                                            >
                                                Edit
                                            </button>
                                            <button
                                                type="button"
                                                onClick={() => {
                                                    // eslint-disable-next-line no-alert
                                                    if (
                                                        confirm(`Delete user '${user.username}'?`)
                                                    ) {
                                                        deleteMutation.mutate(user.id);
                                                    }
                                                }}
                                                className="text-sm text-red-600 hover:text-red-800 underline"
                                            >
                                                Delete
                                            </button>
                                        </div>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            </div>

            <Modal
                isOpen={isCreateOpen}
                onClose={() => setIsCreateOpen(false)}
                title="Create user"
                size="lg"
                actions={
                    <div className="flex gap-2 w-full justify-end">
                        <button
                            type="button"
                            onClick={() => setIsCreateOpen(false)}
                            className="px-3 py-2 rounded-md text-sm font-semibold whitespace-nowrap bg-gray-100 text-gray-700 hover:bg-gray-200"
                        >
                            Cancel
                        </button>
                        <button
                            type="button"
                            onClick={() => createMutation.mutate(createForm)}
                            className="px-3 py-2 rounded-md text-sm font-semibold whitespace-nowrap bg-blue-600 text-white hover:bg-blue-700"
                        >
                            Create
                        </button>
                    </div>
                }
            >
                <div className="space-y-4">
                    <div>
                        <label className="block text-sm font-medium text-gray-700">Username</label>
                        <input
                            type="text"
                            value={createForm.username}
                            onChange={(e) =>
                                setCreateForm((prev) => ({ ...prev, username: e.target.value }))
                            }
                            className="mt-1 w-full rounded border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm border p-2"
                        />
                    </div>
                    <div>
                        <label className="block text-sm font-medium text-gray-700">Email</label>
                        <input
                            type="email"
                            value={createForm.email}
                            onChange={(e) =>
                                setCreateForm((prev) => ({ ...prev, email: e.target.value }))
                            }
                            className="mt-1 w-full rounded border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm border p-2"
                        />
                    </div>
                    <div>
                        <label className="block text-sm font-medium text-gray-700">Password</label>
                        <input
                            type="password"
                            value={createForm.password}
                            onChange={(e) =>
                                setCreateForm((prev) => ({ ...prev, password: e.target.value }))
                            }
                            className="mt-1 w-full rounded border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm border p-2"
                        />
                    </div>

                    <div className="flex items-center gap-3">
                        <input
                            type="checkbox"
                            checked={createForm.enabled}
                            onChange={(e) =>
                                setCreateForm((prev) => ({ ...prev, enabled: e.target.checked }))
                            }
                            className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                        />
                        <span className="text-sm text-gray-700">Enabled</span>
                    </div>

                    <div>
                        <div className="text-sm font-medium text-gray-700 mb-2">Roles</div>
                        {enabledRoles.length === 0 ? (
                            <p className="text-sm text-gray-500">No roles available.</p>
                        ) : (
                            <div className="space-y-2">
                                {enabledRoles.map((role) => (
                                    <label key={role.id} className="flex items-center gap-2">
                                        <input
                                            type="checkbox"
                                            checked={createForm.roleNames.includes(role.name)}
                                            onChange={() =>
                                                setCreateForm((prev) => ({
                                                    ...prev,
                                                    roleNames: toggleRoleNames(
                                                        prev.roleNames,
                                                        role.name
                                                    ),
                                                }))
                                            }
                                            className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                                        />
                                        <span className="text-sm text-gray-700">{role.name}</span>
                                    </label>
                                ))}
                            </div>
                        )}
                    </div>
                </div>
            </Modal>

            <Modal
                isOpen={editingUser !== null}
                onClose={() => setEditingUser(null)}
                title="Edit user"
                size="lg"
                actions={
                    <div className="flex gap-2 w-full justify-end">
                        <button
                            type="button"
                            onClick={() => setEditingUser(null)}
                            className="px-3 py-2 rounded-md text-sm font-semibold whitespace-nowrap bg-gray-100 text-gray-700 hover:bg-gray-200"
                        >
                            Cancel
                        </button>
                        <button
                            type="button"
                            onClick={() => {
                                if (!editingUser) return;
                                updateMutation.mutate({ id: editingUser.id, data: editForm });
                            }}
                            className="px-3 py-2 rounded-md text-sm font-semibold whitespace-nowrap bg-blue-600 text-white hover:bg-blue-700"
                        >
                            Save
                        </button>
                    </div>
                }
            >
                <div className="space-y-4">
                    <div>
                        <div className="text-sm font-medium text-gray-700 mb-1">Username</div>
                        <div className="rounded border border-gray-200 bg-gray-50 p-2 text-sm text-gray-800">
                            {editingUser?.username}
                        </div>
                    </div>
                    <div>
                        <label className="block text-sm font-medium text-gray-700">Email</label>
                        <input
                            type="email"
                            value={editForm.email}
                            onChange={(e) =>
                                setEditForm((prev) => ({ ...prev, email: e.target.value }))
                            }
                            className="mt-1 w-full rounded border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm border p-2"
                        />
                    </div>

                    <div className="flex items-center gap-3">
                        <input
                            type="checkbox"
                            checked={editForm.enabled}
                            onChange={(e) =>
                                setEditForm((prev) => ({ ...prev, enabled: e.target.checked }))
                            }
                            className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                        />
                        <span className="text-sm text-gray-700">Enabled</span>
                    </div>

                    <div>
                        <div className="text-sm font-medium text-gray-700 mb-2">Roles</div>
                        {enabledRoles.length === 0 ? (
                            <p className="text-sm text-gray-500">No roles available.</p>
                        ) : (
                            <div className="space-y-2">
                                {enabledRoles.map((role) => (
                                    <label key={role.id} className="flex items-center gap-2">
                                        <input
                                            type="checkbox"
                                            checked={editForm.roleNames.includes(role.name)}
                                            onChange={() =>
                                                setEditForm((prev) => ({
                                                    ...prev,
                                                    roleNames: toggleRoleNames(
                                                        prev.roleNames,
                                                        role.name
                                                    ),
                                                }))
                                            }
                                            className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                                        />
                                        <span className="text-sm text-gray-700">{role.name}</span>
                                    </label>
                                ))}
                            </div>
                        )}
                    </div>
                </div>
            </Modal>
        </div>
    );
}
