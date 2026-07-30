import { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import type { LoginRequest } from '@/types';
import { useAuth } from '../useAuth';

export default function LoginPage() {
    const { isAuthenticated, login } = useAuth();
    const navigate = useNavigate();
    const location = useLocation();

    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState<string | null>(null);
    const [isLoading, setIsLoading] = useState(false);

    const from = (location.state as { from?: string } | null)?.from || '/';

    useEffect(() => {
        if (!isAuthenticated) return;
        navigate(from, { replace: true });
    }, [from, isAuthenticated, navigate]);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setIsLoading(true);
        setError(null);

        const request: LoginRequest = { username, password };
        try {
            await login(request);
        } catch {
            setError('Invalid username or password');
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="min-h-screen bg-gray-50 dark:bg-gray-900 flex items-center justify-center px-4 py-12">
            <div className="w-full max-w-md">
                <div className="bg-white shadow rounded-lg p-6 dark:bg-gray-800">
                    <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">Login</h1>
                    <p className="text-sm text-gray-600 dark:text-gray-300 mt-2">
                        Use an ADMIN account to manage services.
                    </p>

                    {error && (
                        <div
                            className="mt-4 rounded-md bg-red-50 text-red-800 dark:bg-red-900/20 dark:text-red-200 px-3 py-2 text-sm"
                            role="alert"
                        >
                            {error}
                        </div>
                    )}

                    <form onSubmit={handleSubmit} className="mt-6 space-y-4">
                        <div>
                            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                                Username
                            </label>
                            <input
                                value={username}
                                onChange={(e) => setUsername(e.target.value)}
                                className="mt-1 w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm dark:bg-gray-900 dark:text-gray-100 dark:border-gray-700"
                                placeholder="admin"
                                autoComplete="username"
                            />
                        </div>
                        <div>
                            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                                Password
                            </label>
                            <input
                                type="password"
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                className="mt-1 w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm dark:bg-gray-900 dark:text-gray-100 dark:border-gray-700"
                                placeholder="••••••••"
                                autoComplete="current-password"
                            />
                        </div>

                        <button
                            type="submit"
                            disabled={isLoading || !username || !password}
                            className="w-full rounded-md bg-blue-600 px-4 py-2 text-sm font-semibold text-white shadow-sm hover:bg-blue-500 disabled:opacity-50"
                        >
                            {isLoading ? 'Signing in...' : 'Sign in'}
                        </button>

                        <button
                            type="button"
                            onClick={() => navigate(from)}
                            className="w-full rounded-md bg-gray-100 px-4 py-2 text-sm font-semibold text-gray-800 shadow-sm hover:bg-gray-200 dark:bg-gray-700 dark:text-gray-100 dark:hover:bg-gray-600"
                        >
                            Continue as guest
                        </button>
                    </form>
                </div>
            </div>
        </div>
    );
}
