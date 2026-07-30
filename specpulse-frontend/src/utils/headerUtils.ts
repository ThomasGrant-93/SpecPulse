export function normalizeHeaderName(name: string): string {
    return name.trim().toLowerCase();
}

export function isSensitiveHeaderName(name: string): boolean {
    const n = name.trim().toLowerCase();
    if (n.includes('authorization')) return true;
    if (/(api[-_]?key|apikey)/.test(n)) return true;
    if (n.includes('secret')) return true;
    if (n.includes('token')) return true;
    if (n.includes('password')) return true;
    if (n.includes('credential')) return true;
    return false;
}

export function formatHeaderValueForSend(headerName: string, value: string): string {
    const trimmed = value.trim();
    const normalizedName = normalizeHeaderName(headerName);

    // Swagger UI / OpenAPI bearer formatting convenience.
    if (normalizedName === 'authorization') {
        if (!trimmed) return trimmed;
        if (trimmed.toLowerCase().startsWith('bearer ')) return trimmed;
        return `Bearer ${trimmed}`;
    }

    return trimmed;
}

export function maskSensitiveValue(value: string): string {
    if (!value) return '';
    return '*'.repeat(Math.min(12, Math.max(6, value.length >= 6 ? 8 : value.length)));
}

export function isValidHeaderName(name: string): boolean {
    // Must match the backend proxy sanitizer: "^[A-Za-z0-9-]+$".
    return /^[A-Za-z0-9-]+$/.test(name.trim());
}
