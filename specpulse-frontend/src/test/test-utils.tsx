import {render, type RenderOptions} from '@testing-library/react';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {MemoryRouter} from 'react-router-dom';
import type {ReactElement, ReactNode} from 'react';

// Create a test query client
const createTestQueryClient = () =>
        new QueryClient({
            defaultOptions: {
                queries: {
                    retry: false,
                    staleTime: 0,
                },
                mutations: {
                    retry: false,
                },
            },
        });

interface AllProvidersProps {
    children: ReactNode;
    initialEntries?: string[];
}

function AllProviders({children, initialEntries = ['/']}: AllProvidersProps) {
    const queryClient = createTestQueryClient();

    return (
            <MemoryRouter initialEntries={initialEntries}>
                <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
            </MemoryRouter>
    );
}

interface CustomRenderOptions extends Omit<RenderOptions, 'wrapper'> {
    initialEntries?: string[];
}

function customRender(ui: ReactElement, {initialEntries, ...options}: CustomRenderOptions = {}) {
    return render(ui, {
        wrapper: (props) => <AllProviders initialEntries={initialEntries} {...props} />,
        ...options,
    });
}

// Re-export everything
export * from '@testing-library/react';
export {customRender as render};
export {AllProviders};
