import {memo, useState} from 'react';
import type {ServiceGroup} from '@/types';

interface GroupTreeItemProps {
    group: ServiceGroup;
    selectedGroupId?: number | null;
    onSelectGroup: (group: ServiceGroup | null) => void;
    onEditGroup: (group: ServiceGroup) => void;
    onDeleteGroup: (group: ServiceGroup) => void;
    level?: number;
}

interface GroupTreeProps {
    groups: ServiceGroup[];
    selectedGroupId?: number | null;
    onSelectGroup: (group: ServiceGroup | null) => void;
    onEditGroup: (group: ServiceGroup) => void;
    onDeleteGroup: (group: ServiceGroup) => void;
}

const GroupTreeItem = memo(function GroupTreeItem({
                                                      group,
                                                      selectedGroupId,
                                                      onSelectGroup,
                                                      onEditGroup,
                                                      onDeleteGroup,
                                                      level = 0,
                                                  }: GroupTreeItemProps) {
    const [isExpanded, setIsExpanded] = useState(true);
    const hasChildren = group.childGroups && group.childGroups.length > 0;

    const handleDelete = (e: React.MouseEvent): void => {
        e.stopPropagation();
        if (confirm(`Delete group "${group.name}"?`)) {
            onDeleteGroup(group);
        }
    };

    const handleEdit = (e: React.MouseEvent): void => {
        e.stopPropagation();
        onEditGroup(group);
    };

    return (
            <div>
                <div
                        className={`flex items-center justify-between px-3 py-2 cursor-pointer rounded-md transition-colors ${
                                selectedGroupId === group.id
                                        ? 'bg-blue-100 text-blue-900'
                                        : 'hover:bg-gray-100'
                        }`}
                        style={{paddingLeft: `${level * 16 + 12}px`}}
                        onClick={() => onSelectGroup(group)}
                >
                    <div className="flex items-center gap-2 flex-1 min-w-0">
                        {hasChildren ? (
                                <button
                                        onClick={(e) => {
                                            e.stopPropagation();
                                            setIsExpanded(!isExpanded);
                                        }}
                                        className="p-1 hover:bg-gray-200 rounded"
                                >
                                    <svg
                                            className={`w-4 h-4 transition-transform ${
                                                    isExpanded ? 'rotate-90' : ''
                                            }`}
                                            fill="none"
                                            stroke="currentColor"
                                            viewBox="0 0 24 24"
                                    >
                                        <path
                                                strokeLinecap="round"
                                                strokeLinejoin="round"
                                                strokeWidth={2}
                                                d="M9 5l7 7-7 7"
                                        />
                                    </svg>
                                </button>
                        ) : (
                                <span className="w-4"/>
                        )}

                        {group.icon && (
                                <span className="text-lg" role="img" aria-label={group.name}>
                            {group.icon}
                        </span>
                        )}

                        {group.color && (
                                <span
                                        className="w-3 h-3 rounded-full flex-shrink-0"
                                        style={{backgroundColor: group.color}}
                                />
                        )}

                        <span className="font-medium truncate">{group.name}</span>

                        {group.serviceCount !== undefined && group.serviceCount > 0 && (
                                <span className="text-xs text-gray-500 bg-gray-200 px-2 py-0.5 rounded-full">
                            {group.serviceCount}
                        </span>
                        )}
                    </div>

                    <div className="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                        <button
                                onClick={handleEdit}
                                className="p-1 text-gray-600 hover:text-blue-600 hover:bg-blue-50 rounded"
                                title="Edit group"
                        >
                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path
                                        strokeLinecap="round"
                                        strokeLinejoin="round"
                                        strokeWidth={2}
                                        d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"
                                />
                            </svg>
                        </button>
                        <button
                                onClick={handleDelete}
                                className="p-1 text-gray-600 hover:text-red-600 hover:bg-red-50 rounded"
                                title="Delete group"
                        >
                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path
                                        strokeLinecap="round"
                                        strokeLinejoin="round"
                                        strokeWidth={2}
                                        d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"
                                />
                            </svg>
                        </button>
                    </div>
                </div>

                {hasChildren && isExpanded && (
                        <div>
                            {group.childGroups!.map((child: ServiceGroup) => (
                                    <GroupTreeItem
                                            key={child.id}
                                            group={child}
                                            selectedGroupId={selectedGroupId}
                                            onSelectGroup={onSelectGroup}
                                            onEditGroup={onEditGroup}
                                            onDeleteGroup={onDeleteGroup}
                                            level={level + 1}
                                    />
                            ))}
                        </div>
                )}
            </div>
    );
});

export default function GroupTree({
                                      groups,
                                      selectedGroupId,
                                      onSelectGroup,
                                      onEditGroup,
                                      onDeleteGroup,
                                  }: GroupTreeProps) {
    return (
            <div className="space-y-1">
                {groups.map((group) => (
                        <GroupTreeItem
                                key={group.id}
                                group={group}
                                selectedGroupId={selectedGroupId}
                                onSelectGroup={onSelectGroup}
                                onEditGroup={onEditGroup}
                                onDeleteGroup={onDeleteGroup}
                        />
                ))}
            </div>
    );
}
