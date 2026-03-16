import axios, {type AxiosError} from 'axios';
import type {
    ApplicationSetting,
    AuditLog,
    CreateGroupRequest,
    CreateServiceRequest,
    PullResult,
    Service,
    ServiceGroup,
    SettingsCategory,
    SettingValue,
    SpecDiff,
    SpecVersion,
    UpdateGroupRequest,
    UpdateServiceRequest,
} from '@/types';

const API_BASE = '/api/v1';

const api = axios.create({
    baseURL: API_BASE,
    headers: {
        'Content-Type': 'application/json',
    },
});

// Request interceptor - logging in development
api.interceptors.request.use(
    (config) => {
        if (import.meta.env.DEV) {
            console.warn('[API Request]', config.method?.toUpperCase(), config.url, {
                params: config.params,
                data: config.data,
            });
        }
        return config;
    },
    (error) => {
        if (import.meta.env.DEV) {
            console.error('[API Request Error]', error.message);
        }
        return Promise.reject(error);
    }
);

// Response interceptor - error handling
api.interceptors.response.use(
    (response) => response,
    (error: AxiosError) => {
        if (import.meta.env.DEV) {
            console.error('[API Response Error]', {
                status: error.response?.status,
                statusText: error.response?.statusText,
                data: error.response?.data,
                message: error.message,
            });
        }

        // Handle 401 Unauthorized - could redirect to login in future
        if (error.response?.status === 401) {
            console.warn('[API] Unauthorized access - user should be redirected to login');
            // TODO: Implement auth flow
            // window.location.href = '/login';
        }

        // Handle 403 Forbidden
        if (error.response?.status === 403) {
            console.warn('[API] Forbidden access');
        }

        // Handle 500 Internal Server Error
        if (error.response?.status === 500) {
            console.error('[API] Internal server error');
        }

        return Promise.reject(error);
    }
);

// Registry API
export const registryApi = {
    getAll: () => api.get<Service[]>('/registry'),
    getEnabled: () => api.get<Service[]>('/registry/enabled'),
    getById: (id: number) => api.get<Service>(`/registry/${id}`),
    create: (data: CreateServiceRequest) => api.post<Service>('/registry', data),
    update: (id: number, data: UpdateServiceRequest) => api.put<Service>(`/registry/${id}`, data),
    delete: (id: number) => api.delete(`/registry/${id}`),
};

// Versions API
export const versionsApi = {
    getByService: (serviceId: number) => api.get<SpecVersion[]>(`/versions/service/${serviceId}`),
    getLatest: (serviceId: number) => api.get<SpecVersion>(`/versions/service/${serviceId}/latest`),
    getById: (id: number) => api.get<SpecVersion>(`/versions/${id}`),
};

// Diffs API
export const diffsApi = {
    getByService: (serviceId: number) => api.get<SpecDiff[]>(`/diffs/service/${serviceId}`),
    getById: (id: number) => api.get<SpecDiff>(`/diffs/${id}`),
    compare: (oldSpec: string, newSpec: string) => api.post('/diffs/compare', {oldSpec, newSpec}),
};

// Audit API
export const auditApi = {
    getByService: (serviceId: number) => api.get<AuditLog[]>(`/audit/service/${serviceId}`),
    getRecent: (limit: number = 50) => api.get<AuditLog[]>(`/audit/recent?limit=${limit}`),
};

// Pull API
export const pullApi = {
    triggerService: (serviceId: number) => api.post<PullResult>(`/pull/service/${serviceId}`),
    triggerAll: () => api.post('/pull/all'),
};

// Groups API
export const groupsApi = {
    getAll: () => api.get<ServiceGroup[]>('/groups'),
    getRoot: () => api.get<ServiceGroup[]>('/groups/root'),
    getById: (id: number, includeServices = false) =>
        api.get<ServiceGroup>(`/groups/${id}?includeServices=${includeServices}`),
    create: (data: CreateGroupRequest) => api.post<ServiceGroup>('/groups', data),
    update: (id: number, data: UpdateGroupRequest) =>
        api.put<ServiceGroup>(`/groups/${id}`, data),
    delete: (id: number) => api.delete(`/groups/${id}`),
    addServices: (groupId: number, serviceIds: number[]) =>
        api.post<ServiceGroup>(`/groups/${groupId}/services`, serviceIds),
    removeService: (groupId: number, serviceId: number) =>
        api.delete(`/groups/${groupId}/services/${serviceId}`),
    getServiceGroups: (serviceId: number) =>
        api.get<ServiceGroup[]>(`/groups/services/${serviceId}`),
};

// Settings API
export const settingsApi = {
    getAll: () => api.get<SettingsCategory[]>('/settings'),
    getPublic: () => api.get<SettingsCategory[]>('/settings/public'),
    getByCategory: (category: string) =>
        api.get<SettingsCategory>(`/settings/${category}`),
    getSetting: (category: string, key: string) =>
        api.get<ApplicationSetting>(`/settings/${category}/${key}`),
    update: (category: string, key: string, value: SettingValue) =>
        api.put<ApplicationSetting>(`/settings/${category}/${key}`, {value}),
    updateBulk: (updates: Record<string, SettingValue>) =>
        api.post<ApplicationSetting[]>('/settings', updates),
    getCategories: () => api.get<string[]>('/settings/categories'),
};

export default api;
