import {useQuery} from '@tanstack/react-query';
import {groupsApi} from '@/services/api';
import type {ServiceGroup} from '@/types';
import {logger} from '@/utils/logger';

interface GroupSelectorProps {
    value?: number | null;
    onChange: (groupId: number | null) => void;
    disabled?: boolean;
    label?: string;
}

interface GroupOptionProps {
    group: ServiceGroup;
    level: number;
    onSelect: (groupId: number | null) => void;
}

function GroupOption({group, level, onSelect}: GroupOptionProps) {
    const hasChildren = group.childGroups && group.childGroups.length > 0;

    return (
            <>
                <option
                        key={`group-${group.id}`}
                        value={group.id}
                        style={{paddingLeft: `${level * 16}px`}}
                >
                    {'  '.repeat(level)}
                    {group.icon && `${group.icon} `}
                    {group.name}
                    {group.serviceCount !== undefined && group.serviceCount > 0
                            ? ` (${group.serviceCount})`
                            : ''}
                </option>
                {hasChildren &&
                        group.childGroups!.map((child) => (
                                <GroupOption
                                        key={child.id}
                                        group={child}
                                        level={level + 1}
                                        onSelect={onSelect}
                                />
                        ))}
            </>
    );
}

export default function GroupSelector({
                                          value,
                                          onChange,
                                          disabled = false,
                                          label = 'Group',
                                      }: GroupSelectorProps) {
    const {data: groups = [], isLoading} = useQuery({
        queryKey: ['groups'],
        queryFn: async () => {
            const response = await groupsApi.getAll();
            return response.data;
        },
    });

    if (isLoading) {
        return (
                <div className="text-sm text-gray-500 italic">Loading groups...</div>
        );
    }

    if (groups.length === 0) {
        return (
                <div className="text-sm text-gray-500">
                    No groups available.{' '}
                    <a href="/groups" className="text-blue-600 hover:underline">
                        Create a group
                    </a>
                </div>
        );
    }

    return (
            <div>
                <label htmlFor="group-selector" className="block text-sm font-medium text-gray-700">
                    {label}
                </label>
                <select
                        id="group-selector"
                        value={value || ''}
                        onChange={(e) => {
                            const selectedValue = e.target.value ? Number(e.target.value) : null;
                            logger.debug('[GroupSelector] Group changed to:', selectedValue);
                            onChange(selectedValue);
                        }}
                        disabled={disabled}
                        className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm border p-2 disabled:bg-gray-100 disabled:cursor-not-allowed"
                >
                    <option value="">No group</option>
                    {groups.map((group) => (
                            <GroupOption
                                    key={group.id}
                                    group={group}
                                    level={0}
                                    onSelect={onChange}
                            />
                    ))}
                </select>
            </div>
    );
}
