import { createContext, PropsWithChildren, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import type { LoginRequest, LoginResponse, MeResponse } from '@/types';
import { authService } from './authApi';
import {
    clearTokens,
    getAccessToken,
    getRefreshToken,
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
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
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
        const a = getAccessToken();
        const r = getRefreshToken();
        if (!a || !r) {
            setUser(null);
            return;
        }

        authService
            .me()
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
                setTokens({ accessToken: tokens.accessToken, refreshToken: tokens.refreshToken });
                const meRes = await authService.me();
                setUser(meRes.data);
                navigate('/');
                return tokens;
            },
            logout: () => {
                clearTokens();
                setUser(null);
                navigate('/login');
            },
        };
    }, [navigate, user]);

    useEffect(() => {
        const handler = () => {
            clearTokens();
            setUser(null);
            navigate('/login');
        };
        window.addEventListener('specpulse:auth:logout', handler);
        return () => window.removeEventListener('specpulse:auth:logout', handler);
    }, [navigate]);

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
