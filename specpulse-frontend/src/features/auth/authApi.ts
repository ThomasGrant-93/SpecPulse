import axios from 'axios';
import type {
    LoginRequest,
    LoginResponse,
    MeResponse,
    RefreshResponse,
} from '@/types';
import { getAccessToken } from './tokenStore';

const API_BASE = '/api/v1';

// Separate client: do NOT use the global API client (so refresh doesn't recurse into itself).
const authApi = axios.create({
    baseURL: API_BASE,
    headers: {
        'Content-Type': 'application/json',
    },
    withCredentials: true,
});

authApi.interceptors.request.use((config) => {
    const access = getAccessToken();
    if (!access) return config;

    const currentHeaders = config.headers ?? {};
    const hasAuthHeader =
        typeof (currentHeaders as any).Authorization === 'string' ||
        typeof (currentHeaders as any).authorization === 'string';

    if (!hasAuthHeader) {
        config.headers = {
            ...(currentHeaders as any),
            Authorization: `Bearer ${access}`,
        } as any;
    }
    return config;
});

export const authService = {
    login: (request: LoginRequest) => authApi.post<LoginResponse>('/auth/login', request),
    refresh: () => authApi.post<RefreshResponse>('/auth/refresh'),
    me: () => authApi.get<MeResponse>('/auth/me'),
    logout: () => authApi.post<void>('/auth/logout'),
};
