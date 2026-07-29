const ACCESS_TOKEN_KEY = 'specpulse.accessToken';
const REFRESH_TOKEN_KEY = 'specpulse.refreshToken';

let accessToken: string | null = null;
let refreshToken: string | null = null;

export function hydrateTokensFromStorage(): void {
    accessToken = window.localStorage.getItem(ACCESS_TOKEN_KEY);
    refreshToken = window.localStorage.getItem(REFRESH_TOKEN_KEY);
}

export function setTokens(next: { accessToken: string; refreshToken: string }): void {
    accessToken = next.accessToken;
    refreshToken = next.refreshToken;
    window.localStorage.setItem(ACCESS_TOKEN_KEY, next.accessToken);
    window.localStorage.setItem(REFRESH_TOKEN_KEY, next.refreshToken);
}

export function clearTokens(): void {
    accessToken = null;
    refreshToken = null;
    window.localStorage.removeItem(ACCESS_TOKEN_KEY);
    window.localStorage.removeItem(REFRESH_TOKEN_KEY);
}

export function getAccessToken(): string | null {
    return accessToken;
}

export function getRefreshToken(): string | null {
    return refreshToken;
}
