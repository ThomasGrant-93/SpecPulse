import {useState} from 'react';
import {useMutation, useQueryClient} from '@tanstack/react-query';
import GroupSelector from './GroupSelector';

interface BulkGroupAssignmentProps {
    serviceIds: number[];
    onSuccess?: () => void;
    onCancel: () => void;
}

export default function BulkGroupAssignment({
                                                serviceIds,
                                                onSuccess,
                                                onCancel,
                                            }: BulkGroupAssignmentProps) {
    const [selectedGroupId, setSelectedGroupId] = useState<number | null>(null);
    const queryClient = useQueryClient();

    const assignMutation = useMutation({
        mutationFn: async (groupId: number) => {
            const promises = serviceIds.map((serviceId) =>
                    fetch(`/api/v1/registry/${serviceId}`, {
                        method: 'PUT',
                        headers: {'Content-Type': 'application/json'},
                        body: JSON.stringify({groupId}),
                    }).then((res) => {
                        if (!res.ok) throw new Error(`Failed to update service ${serviceId}`);
                        return res.json();
                    })
            );
            await Promise.all(promises);
        },
        onSuccess: () => {
            // Invalidate all service queries including searches
            queryClient.invalidateQueries({queryKey: ['services'], exact: false});
            queryClient.invalidateQueries({queryKey: ['groups'], exact: false});
            onSuccess?.();
        },
    });

    const handleAssign = () => {
        if (!selectedGroupId) {
            alert('Please select a group');
            return;
        }
        assignMutation.mutate(selectedGroupId);
    };

    return (
            <div className="fixed inset-0 bg-gray-500 bg-opacity-75 transition-opacity z-50">
                <div className="fixed inset-0 z-10 overflow-y-auto">
                    <div className="flex min-h-full items-end justify-center p-4 text-center sm:items-center sm:p-0">
                        <div className="relative transform overflow-hidden rounded-lg bg-white px-4 pb-4 pt-5 text-left shadow-xl transition-all sm:my-8 sm:w-full sm:max-w-lg sm:p-6">
                            <div>
                                <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-blue-100">
                                    <svg
                                            className="h-6 w-6 text-blue-600"
                                            fill="none"
                                            stroke="currentColor"
                                            viewBox="0 0 24 24"
                                    >
                                        <path
                                                strokeLinecap="round"
                                                strokeLinejoin="round"
                                                strokeWidth={2}
                                                d="M17 14v6m-3-3h6M6 10h2a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v2a2 2 0 002 2zm10 0h2a2 2 0 002-2V6a2 2 0 00-2-2h-2a2 2 0 00-2 2v2a2 2 0 002 2zM6 20h2a2 2 0 002-2v-2a2 2 0 00-2-2H6a2 2 0 00-2 2v2a2 2 0 002 2z"
                                        />
                                    </svg>
                                </div>
                                <div className="mt-3 text-center sm:mt-5">
                                    <h3 className="text-lg font-semibold leading-6 text-gray-900">
                                        Assign Services to Group
                                    </h3>
                                    <div className="mt-2">
                                        <p className="text-sm text-gray-500">
                                            Selected {serviceIds.length} service
                                            {serviceIds.length !== 1 ? 's' : ''}. Choose a group to
                                            assign them to:
                                        </p>
                                    </div>
                                    <div className="mt-4">
                                        <GroupSelector
                                                value={selectedGroupId}
                                                onChange={setSelectedGroupId}
                                                label="Target Group"
                                        />
                                    </div>
                                </div>
                            </div>
                            <div className="mt-5 sm:mt-6 sm:grid sm:grid-flow-row-dense sm:grid-cols-2 sm:gap-3">
                                <button
                                        type="button"
                                        className="inline-flex w-full justify-center rounded-md bg-blue-600 px-3 py-2 text-sm font-semibold text-white shadow-sm hover:bg-blue-500 sm:col-start-2"
                                        onClick={handleAssign}
                                        disabled={assignMutation.isPending || !selectedGroupId}
                                >
                                    {assignMutation.isPending ? 'Assigning...' : 'Assign'}
                                </button>
                                <button
                                        type="button"
                                        className="mt-3 inline-flex w-full justify-center rounded-md bg-white px-3 py-2 text-sm font-semibold text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 hover:bg-gray-50 sm:col-start-1 sm:mt-0"
                                        onClick={onCancel}
                                        disabled={assignMutation.isPending}
                                >
                                    Cancel
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
    );
}
