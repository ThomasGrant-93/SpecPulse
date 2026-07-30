import { AxiosError, AxiosResponse } from 'axios';

import { api } from './api';

export interface TestRequest {
    url: string;
    method: string;
    parameters?: Record<string, string>;
    headers?: Record<string, string>;
    body?: unknown | Record<string, unknown>;
}

export interface TestResponse {
    status: number;
    statusText: string;
    headers: Record<string, string>;
    body: unknown;
    duration: number;
    error?: string;
}

export const apiTester = {
    async execute(request: TestRequest): Promise<TestResponse> {
        const startTime = Date.now();

        // Build URL with query parameters
        let url = request.url;
        if (request.parameters) {
            const queryParams = new URLSearchParams();
            Object.entries(request.parameters).forEach(([key, value]) => {
                if (value) {
                    queryParams.append(key, value);
                }
            });
            if (queryParams.toString()) {
                url += (url.includes('?') ? '&' : '?') + queryParams.toString();
            }
        }

        try {
            const response: AxiosResponse = await api.request({
                method: 'post',
                url: '/tests/proxy',
                headers: {
                    'Content-Type': 'application/json',
                },
                data: {
                    url,
                    method: request.method,
                    headers: request.headers,
                    body: request.body,
                },
                validateStatus: () => true, // Accept all status codes
            });

            const duration = Date.now() - startTime;
            const proxyResult = response.data as Omit<TestResponse, 'duration'>;

            return {
                status: proxyResult.status ?? 0,
                statusText: proxyResult.statusText ?? '',
                headers: proxyResult.headers ?? {},
                body: proxyResult.body,
                duration,
                error: proxyResult.error,
            };
        } catch (error) {
            const duration = Date.now() - startTime;
            const axiosError = error as AxiosError;

            return {
                status: axiosError.response?.status || 0,
                statusText: axiosError.message,
                headers: {},
                body: axiosError.response?.data || { error: axiosError.message },
                duration,
                error: axiosError.message,
            };
        }
    },
};
