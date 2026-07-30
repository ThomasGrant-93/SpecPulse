export function isTestEnvironment(): boolean {
    // Vitest runs Vite in "test" mode.
    if (import.meta.env.MODE === 'test') return true;

    // Fallbacks for other runners/environments.
    if (typeof process !== 'undefined' && process.env.NODE_ENV === 'test') return true;

    return false;
}
