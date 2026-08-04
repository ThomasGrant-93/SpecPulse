const ACCESS_TOKEN_KEY = 'specpulse.accessToken';

let accessToken: string | null = null;

export function hydrateTokensFromStorage(): void {
    accessToken = window.localStorage.getItem(ACCESS_TOKEN_KEY);
}

export function setTokens(next: { accessToken: string }): void {
    accessToken = next.accessToken;
    window.localStorage.setItem(ACCESS_TOKEN_KEY, next.accessToken);
}

export function clearTokens(): void {
    accessToken = null;
    window.localStorage.removeItem(ACCESS_TOKEN_KEY);
}

export function getAccessToken(): string | null {
    return accessToken;
}
