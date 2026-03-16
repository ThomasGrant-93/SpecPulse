import axios, {AxiosError, AxiosResponse} from 'axios';

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
            const response: AxiosResponse = await axios({
                method: request.method.toLowerCase(),
                url,
                headers: request.headers,
                data: request.body,
                validateStatus: () => true, // Accept all status codes
            });

            const duration = Date.now() - startTime;

            return {
                status: response.status,
                statusText: response.statusText,
                headers: response.headers as Record<string, string>,
                body: response.data,
                duration,
            };
        } catch (error) {
            const duration = Date.now() - startTime;
            const axiosError = error as AxiosError;

            return {
                status: axiosError.response?.status || 0,
                statusText: axiosError.message,
                headers: {},
                body: axiosError.response?.data || {error: axiosError.message},
                duration,
                error: axiosError.message,
            };
        }
    },
};
