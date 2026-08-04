import { createContext, PropsWithChildren, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import type { LoginRequest, LoginResponse, MeResponse } from '@/types';
import { authService } from './authApi';
import { api } from '@/services/api';
import {
    clearTokens,
    hydrateTokensFromStorage,
    setTokens,
} from './tokenStore';

type AuthContextValue = {
    user: MeResponse | null;
    isAuthenticated: boolean;
    isAdmin: boolean;
    login: (request: LoginRequest) => Promise<LoginResponse>;
    logout: () => void;
};

export const AuthContext = createContext<AuthContextValue>({
    user: null,
    isAuthenticated: false,
    isAdmin: false,
    login: async (_req: LoginRequest) => {
        throw new Error('AuthProvider not mounted');
    },
    logout: () => {
        // no-op
    },
});

export default function AuthProvider({ children }: PropsWithChildren) {
    const navigate = useNavigate();
    const [user, setUser] = useState<MeResponse | null>(null);
    const [hydrated, setHydrated] = useState(false);

    useEffect(() => {
        hydrateTokensFromStorage();
        setHydrated(true);
    }, []);

    useEffect(() => {
        if (!hydrated) return;

        // Always call /auth/me and let the API client refresh access tokens using the refresh cookie when needed.
        api
            .get<MeResponse>('/auth/me')
            .then((res) => setUser(res.data))
            .catch(() => {
                clearTokens();
                setUser(null);
            });
    }, [hydrated]);

    const value = useMemo<AuthContextValue>(() => {
        const roles = user?.roles || [];
        const isAdmin = roles.includes('ADMIN');

        return {
            user,
            isAuthenticated: !!user && user.enabled,
            isAdmin,
            login: async (request: LoginRequest) => {
                const res = await authService.login(request);
                const tokens = res.data;
                setTokens({ accessToken: tokens.accessToken });
                const meRes = await api.get<MeResponse>('/auth/me');
                setUser(meRes.data);
                navigate('/');
                return tokens;
            },
            logout: () => {
                authService
                    .logout()
                    .catch(() => {
                        // Even if logout request fails, clear local tokens and rely on refresh cookie invalidation.
                        return;
                    })
                    .finally(() => {
                        clearTokens();
                        setUser(null);
                        navigate('/login');
                    });
            },
        };
    }, [navigate, user]);

    useEffect(() => {
        const handler = () => {
            authService
                .logout()
                .catch(() => {
                    return;
                })
                .finally(() => {
                    clearTokens();
                    setUser(null);
                    navigate('/login');
                });
        };
        window.addEventListener('specpulse:auth:logout', handler);
        return () => window.removeEventListener('specpulse:auth:logout', handler);
    }, [navigate]);

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
