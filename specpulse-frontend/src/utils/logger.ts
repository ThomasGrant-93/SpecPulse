/**
 * Logging utility for SpecPulse frontend
 * Only logs debug/warn messages in development mode
 * Error logs are always shown for debugging production issues
 */

const isDev = import.meta.env.DEV;

export const logger = {
    /**
     * Debug logging - only shown in development
     */
    debug: (message: string, ...args: unknown[]): void => {
        if (isDev) {
            console.debug(`[SpecPulse DEBUG] ${message}`, ...args);
        }
    },

    /**
     * Warning logging - only shown in development
     */
    warn: (message: string, ...args: unknown[]): void => {
        if (isDev) {
            console.warn(`[SpecPulse WARN] ${message}`, ...args);
        }
    },

    /**
     * Error logging - always shown
     */
    error: (message: string, ...args: unknown[]): void => {
        console.error(`[SpecPulse ERROR] ${message}`, ...args);
    },

    /**
     * Info logging - only shown in development
     */
    info: (message: string, ...args: unknown[]): void => {
        if (isDev) {
            console.info(`[SpecPulse INFO] ${message}`, ...args);
        }
    },
};
