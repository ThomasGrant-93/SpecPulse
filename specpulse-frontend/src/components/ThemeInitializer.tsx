import { useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import { settingsApi } from '@/features/settings/api';
import type { SettingsCategory, SettingValue } from '@/types';

type ThemeMode = 'light' | 'dark' | 'system';

function coerceThemeMode(value: SettingValue | undefined): ThemeMode {
    if (typeof value !== 'string') return 'light';
    if (value === 'dark' || value === 'light' || value === 'system') return value;
    return 'light';
}

function extractAppTheme(categories: SettingsCategory[] | undefined): ThemeMode {
    const fallback = categories
        ?.flatMap((c) => c.settings)
        .find((s) => s.key === 'app.theme')
        ?.value;

    // Backward/forward compatibility with potential future keys that include "theme".
    const looseFallback = fallback ??
        categories
            ?.flatMap((c) => c.settings)
            .find((s) => s.key.includes('theme'))
            ?.value;

    return coerceThemeMode(looseFallback);
}

export default function ThemeInitializer() {
    const { data: categories } = useQuery({
        queryKey: ['settings'],
        queryFn: async () => {
            const response = await settingsApi.getAll();
            return response.data;
        },
    });

    useEffect(() => {
        if (!categories) return;

        const themeMode = extractAppTheme(categories);
        const root = document.documentElement;

        const apply = (isDark: boolean) => {
            root.classList.toggle('dark', isDark);
        };

        if (themeMode === 'system') {
            const mq = window.matchMedia('(prefers-color-scheme: dark)');
            apply(mq.matches);

            const onChange = (e: MediaQueryListEvent) => {
                apply(e.matches);
            };

            mq.addEventListener('change', onChange);
            return () => mq.removeEventListener('change', onChange);
        }

        apply(themeMode === 'dark');
        return;
    }, [categories]);

    return null;
}
