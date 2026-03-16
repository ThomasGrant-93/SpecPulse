/**
 * Setting value types matching backend valueType enum
 */
export type SettingValue = string | number | boolean | string[];

export interface Service {
    id: number;
    name: string;
    openApiUrl: string;
    description?: string;
    enabled: boolean;
    groupId?: number;
    group?: {
        id: number;
        name: string;
        icon?: string;
        color?: string;
    };
    latestVersion?: {
        id: number;
        versionHash: string;
        specVersion?: string;
        specTitle?: string;
        pulledAt: string;
    };
}

export interface ServiceGroup {
    id: number;
    name: string;
    description?: string;
    parentGroupId?: number;
    parentGroupName?: string;
    color?: string;
    icon?: string;
    sortOrder: number;
    createdAt: string;
    updatedAt: string;
    childGroups?: ServiceGroup[];
    serviceCount?: number;
    services?: GroupService[];
}

export interface GroupService {
    id: number;
    name: string;
    openApiUrl: string;
    description?: string;
    enabled: boolean;
    addedAt: string;
}

export interface CreateGroupRequest {
    name: string;
    description?: string;
    parentGroupId?: number;
    color?: string;
    icon?: string;
    sortOrder?: number;
}

export interface UpdateGroupRequest {
    name?: string;
    description?: string;
    parentGroupId?: number;
    color?: string;
    icon?: string;
    sortOrder?: number;
}

export interface ApplicationSetting {
    id: number;
    category: string;
    key: string;
    value: SettingValue;
    valueType: 'string' | 'integer' | 'boolean' | 'array';
    description?: string;
    isPublic: boolean;
    isEditable: boolean;
    createdAt: string;
    updatedAt: string;
}

export interface SettingsCategory {
    name: string;
    displayName: string;
    description?: string;
    settings: ApplicationSetting[];
}

export interface SpecVersion {
    id: number;
    serviceId: number;
    versionHash: string;
    specVersion?: string;
    specTitle?: string;
    fileSizeBytes?: number;
    pulledAt: string;
    specContent?: string;
}

export interface SpecDiff {
    id: number;
    serviceId: number;
    fromVersionId: number;
    toVersionId: number;
    diffContent: string;
    hasBreakingChanges: boolean;
    breakingChangesCount: number;
    createdAt: string;
}

export interface AuditLog {
    id: number;
    serviceId?: number;
    specVersionId?: number;
    eventType: string;
    eventDetails?: string;
    createdAt: string;
}

export interface CreateServiceRequest {
    name: string;
    openApiUrl: string;
    description?: string;
    enabled?: boolean;
    groupId?: number | null;
}

export interface UpdateServiceRequest {
    name?: string;
    openApiUrl?: string;
    description?: string;
    enabled?: boolean;
}

export interface PullResult {
    success: boolean;
    hasChanges: boolean;
    newVersionId?: number;
    previousVersionId?: number;
    versionHash?: string;
    error?: string;
}
