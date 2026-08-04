import axios, { type AxiosError } from 'axios';
import { authService } from '@/features/auth/authApi';
import { clearTokens, getAccessToken, setTokens } from '@/features/auth/tokenStore';
import { isTestEnvironment } from '@/utils/testEnv';
import type {
    ApplicationSetting,
    AuditLog,
    AdminRole,
    AdminUser,
    CreateGroupRequest,
    CreateServiceRequest,
    CreateAdminUserRequest,
    PullResult,
    Service,
    ServiceGroup,
    SettingsCategory,
    SettingValue,
    SpecDiff,
    SpecVersion,
    UpdateAdminUserRequest,
    UpdateGroupRequest,
    UpdateServiceRequest,
} from '@/types';

const API_BASE = '/api/v1';

const isTestEnv = isTestEnvironment();

function decodeJwtPayload(token: string): any | null {
    try {
        const parts = token.split('.');
        if (parts.length < 2) return null;
        const payloadB64 = parts[1]
            .replace(/-/g, '+')
            .replace(/_/g, '/');
        const pad = '='.repeat((4 - (payloadB64.length % 4)) % 4);
        const b64 = payloadB64 + pad;

        // Node (tests) + browser support.
        if (typeof Buffer !== 'undefined') {
            return JSON.parse(Buffer.from(b64, 'base64').toString('utf8'));
        }

        // eslint-disable-next-line no-undef
        const decoded = atob(b64);
        const json = decodeURIComponent(
            Array.from(decoded)
                .map((c) => `%${c.charCodeAt(0).toString(16).padStart(2, '0')}`)
                .join('')
        );
        return JSON.parse(json);
    } catch {
        return null;
    }
}

function getJwtType(token: string): string | null {
    const payload = decodeJwtPayload(token);
    const typ = payload?.typ;
    return typeof typ === 'string' ? typ : null;
}

export const api = axios.create({
    baseURL: API_BASE,
    headers: {
        'Content-Type': 'application/json',
    },
});

// Request interceptor - logging in development
api.interceptors.request.use(
    (config) => {
        // Attach access token for backend JWT auth (don't override explicit Authorization).
        const access = getAccessToken();

        if (access) {
            const jwtType = getJwtType(access);
            // Never attach refresh tokens as Authorization bearer tokens.
            if (jwtType === 'refresh') {
                clearTokens();
            }
        }

        const effectiveAccess = getAccessToken();
        const currentHeaders = config.headers ?? {};
        const hasAuthHeader =
            (typeof (currentHeaders as any).Authorization === 'string' && (currentHeaders as any).Authorization.trim() !== '') ||
            (typeof (currentHeaders as any).authorization === 'string' && (currentHeaders as any).authorization.trim() !== '');

        if (effectiveAccess && !hasAuthHeader) {
            config.headers = {
                ...(currentHeaders as any),
                Authorization: `Bearer ${effectiveAccess}`,
            } as any;
        }

        if (import.meta.env.DEV && !isTestEnv) {
            console.warn('[API Request]', config.method?.toUpperCase(), config.url, {
                params: config.params,
                data: config.data,
            });
        }
        return config;
    },
    (error) => {
        if (import.meta.env.DEV && !isTestEnv) {
            console.error('[API Request Error]', error.message);
        }
        return Promise.reject(error);
    }
);

let refreshInFlight: Promise<string> | null = null;

type RetryAxiosRequestConfig = Parameters<typeof api.request>[0] & { __authRetry?: boolean };

// Response interceptor - error handling + refresh on 401
api.interceptors.response.use(
    (response) => response,
    (error: AxiosError) => {
        if (import.meta.env.DEV && !isTestEnv) {
            console.error('[API Response Error]', {
                status: error.response?.status,
                statusText: error.response?.statusText,
                data: error.response?.data,
                message: error.message,
            });
        }

        if (error.response?.status === 401) {
            const config = error.config as RetryAxiosRequestConfig;
            const url = config.url ?? '';
            const isAuthCall = url.includes('/auth/login') || url.includes('/auth/refresh');
            if (isAuthCall || config.__authRetry) {
                clearTokens();
                window.dispatchEvent(new Event('specpulse:auth:logout'));
                return Promise.reject(error);
            }

            config.__authRetry = true;

            if (!refreshInFlight) {
                refreshInFlight = authService
                    .refresh()
                    .then((res) => {
                        setTokens({ accessToken: res.data.accessToken });
                        return res.data.accessToken;
                    })
                    .catch((e) => {
                        clearTokens();
                        window.dispatchEvent(new Event('specpulse:auth:logout'));
                        throw e;
                    })
                    .finally(() => {
                        refreshInFlight = null;
                    });
            }

            return refreshInFlight.then((newAccess) => {
                const currentHeaders = config.headers ?? {};
                config.headers = {
                    ...(currentHeaders as any),
                    Authorization: `Bearer ${newAccess}`,
                } as any;
                return api.request(config);
            });
        }

        // Handle 403 Forbidden
        if (error.response?.status === 403) {
            if (!isTestEnv) console.warn('[API] Forbidden access');
        }

        // Handle 500 Internal Server Error
        if (error.response?.status === 500) {
            if (!isTestEnv) console.error('[API] Internal server error');
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
    getById: (
        id: number,
        params?: {
            diff_only?: boolean;
            exclude_unchanged?: boolean;
            compare_to?: number;
        }
    ) => {
        const url = `/versions/${id}`;
        return params ? api.get<SpecVersion>(url, { params }) : api.get<SpecVersion>(url);
    },
};

// Diffs API
export const diffsApi = {
    getByService: (serviceId: number) => api.get<SpecDiff[]>(`/diffs/service/${serviceId}`),
    getById: (id: number) => api.get<SpecDiff>(`/diffs/${id}`),
    compare: (oldSpec: string, newSpec: string) => api.post('/diffs/compare', { oldSpec, newSpec }),
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
    update: (id: number, data: UpdateGroupRequest) => api.put<ServiceGroup>(`/groups/${id}`, data),
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
    getByCategory: (category: string) => api.get<SettingsCategory>(`/settings/${category}`),
    getSetting: (category: string, key: string) =>
        api.get<ApplicationSetting>(`/settings/${category}/${key}`),
    update: (category: string, key: string, value: SettingValue) =>
        api.put<ApplicationSetting>(`/settings/${category}/${key}`, { value }),
    updateBulk: (updates: Record<string, SettingValue>) =>
        api.patch<ApplicationSetting[]>('/settings', updates),
    getCategories: () => api.get<string[]>('/settings/categories'),
};

// Admin API (user/role management)
export const adminUsersApi = {
    list: () => api.get<AdminUser[]>('/admin/users'),
    getById: (id: number) => api.get<AdminUser>(`/admin/users/${id}`),
    create: (data: CreateAdminUserRequest) => api.post<AdminUser>('/admin/users', data),
    update: (id: number, data: UpdateAdminUserRequest) =>
        api.put<AdminUser>(`/admin/users/${id}`, data),
    delete: (id: number) => api.delete(`/admin/users/${id}`),
};

export const adminRolesApi = {
    listEnabled: () => api.get<AdminRole[]>('/admin/roles'),
};

export default api;
