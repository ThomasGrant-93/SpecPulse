import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import {describe, expect, it, vi, beforeEach} from 'vitest';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import GroupSelector from './GroupSelector';

// Mock the groupsApi module
vi.mock('@/services/api', () => ({
    groupsApi: {
        getAll: vi.fn(),
    },
}));

// Import mocked api after mocking
import {groupsApi} from '@/services/api';

const createQueryClient = () =>
        new QueryClient({
            defaultOptions: {
                queries: {
                    retry: false,
                },
            },
        });

beforeEach(() => {
    vi.clearAllMocks();
});

const mockGroups = [
    {
        id: 1,
        name: 'Production',
        description: 'Production environment',
        color: '#22c55e',
        icon: '🌍',
        sortOrder: 1,
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
        serviceCount: 5,
    },
    {
        id: 2,
        name: 'Development',
        description: 'Development environment',
        color: '#3b82f6',
        icon: '💻',
        sortOrder: 2,
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
        serviceCount: 10,
        childGroups: [
            {
                id: 3,
                name: 'Frontend',
                parentGroupId: 2,
                parentGroupName: 'Development',
                color: '#f59e0b',
                icon: '🎨',
                sortOrder: 1,
                createdAt: '2024-01-01T00:00:00Z',
                updatedAt: '2024-01-01T00:00:00Z',
                serviceCount: 4,
            },
        ],
    },
];

const renderWithProviders = (component: React.ReactElement, queryClient = createQueryClient()) => {
    return {
        ...render(
                <QueryClientProvider client={queryClient}>
                    {component}
                </QueryClientProvider>
        ),
        queryClient,
    };
};

describe('GroupSelector', () => {
    it('should show loading state initially', () => {
        (groupsApi.getAll as any).mockResolvedValue({data: []});

        const queryClient = createQueryClient();

        renderWithProviders(
                <GroupSelector value={null} onChange={vi.fn()}/>,
                queryClient
        );

        expect(screen.getByText(/loading groups/i)).toBeInTheDocument();
    });

    it('should show message when no groups available', async () => {
        (groupsApi.getAll as any).mockResolvedValue({data: []});

        const queryClient = createQueryClient();

        renderWithProviders(
                <GroupSelector value={null} onChange={vi.fn()}/>,
                queryClient
        );

        await waitFor(() => {
            expect(screen.getByText(/no groups available/i)).toBeInTheDocument();
        });

        expect(screen.getByText(/create a group/i)).toBeInTheDocument();
    });

    it('should render groups as options', async () => {
        (groupsApi.getAll as any).mockResolvedValue({data: mockGroups});

        const queryClient = createQueryClient();

        renderWithProviders(
                <GroupSelector value={null} onChange={vi.fn()}/>,
                queryClient
        );

        await waitFor(() => {
            expect(screen.getByLabelText('Group')).toBeInTheDocument();
        });

        const select = screen.getByLabelText('Group') as HTMLSelectElement;
        expect(select.options).toHaveLength(4); // No group + 2 groups + 1 child group
        expect(select.options[1].text).toContain('Production');
        expect(select.options[2].text).toContain('Development');
    });

    it('should show "No group" as first option', async () => {
        (groupsApi.getAll as any).mockResolvedValue({data: mockGroups});

        const queryClient = createQueryClient();

        renderWithProviders(
                <GroupSelector value={null} onChange={vi.fn()}/>,
                queryClient
        );

        await waitFor(() => {
            const select = screen.getByLabelText('Group') as HTMLSelectElement;
            expect(select.options[0].text).toBe('No group');
        });
    });

    it('should call onChange when group is selected', async () => {
        (groupsApi.getAll as any).mockResolvedValue({data: mockGroups});

        const queryClient = createQueryClient();
        const onChange = vi.fn();

        renderWithProviders(
                <GroupSelector value={null} onChange={onChange}/>,
                queryClient
        );

        await waitFor(() => {
            const select = screen.getByLabelText('Group') as HTMLSelectElement;
            fireEvent.change(select, {target: {value: '1'}});
        });

        expect(onChange).toHaveBeenCalledWith(1);
    });

    it('should call onChange with null when "No group" is selected', async () => {
        (groupsApi.getAll as any).mockResolvedValue({data: mockGroups});

        const queryClient = createQueryClient();
        const onChange = vi.fn();

        renderWithProviders(
                <GroupSelector value={1} onChange={onChange}/>,
                queryClient
        );

        await waitFor(() => {
            const select = screen.getByLabelText('Group') as HTMLSelectElement;
            fireEvent.change(select, {target: {value: ''}});
        });

        expect(onChange).toHaveBeenCalledWith(null);
    });

    it('should respect disabled prop', async () => {
        (groupsApi.getAll as any).mockResolvedValue({data: mockGroups});

        const queryClient = createQueryClient();

        renderWithProviders(
                <GroupSelector value={null} onChange={vi.fn()} disabled/>,
                queryClient
        );

        await waitFor(() => {
            const select = screen.getByLabelText('Group') as HTMLSelectElement;
            expect(select).toBeDisabled();
        });
    });

    it('should use custom label', async () => {
        (groupsApi.getAll as any).mockResolvedValue({data: mockGroups});

        const queryClient = createQueryClient();

        renderWithProviders(
                <GroupSelector value={null} onChange={vi.fn()} label="Custom Label"/>,
                queryClient
        );

        await waitFor(() => {
            expect(screen.getByLabelText('Custom Label')).toBeInTheDocument();
        });
    });

    it('should display service count in option text', async () => {
        (groupsApi.getAll as any).mockResolvedValue({data: mockGroups});

        const queryClient = createQueryClient();

        renderWithProviders(
                <GroupSelector value={null} onChange={vi.fn()}/>,
                queryClient
        );

        await waitFor(() => {
            const select = screen.getByLabelText('Group') as HTMLSelectElement;
            expect(select.options[1].text).toContain('(5)');
            expect(select.options[2].text).toContain('(10)');
        });
    });

    it('should display icon in option text', async () => {
        (groupsApi.getAll as any).mockResolvedValue({data: mockGroups});

        const queryClient = createQueryClient();

        renderWithProviders(
                <GroupSelector value={null} onChange={vi.fn()}/>,
                queryClient
        );

        await waitFor(() => {
            const select = screen.getByLabelText('Group') as HTMLSelectElement;
            expect(select.options[1].text).toContain('🌍');
            expect(select.options[2].text).toContain('💻');
        });
    });

    it('should indent child group options', async () => {
        (groupsApi.getAll as any).mockResolvedValue({data: mockGroups});

        const queryClient = createQueryClient();

        renderWithProviders(
                <GroupSelector value={null} onChange={vi.fn()}/>,
                queryClient
        );

        await waitFor(() => {
            const select = screen.getByLabelText('Group') as HTMLSelectElement;
            // Frontend is a child group, should have indentation
            const frontendOption = Array.from(select.options).find((opt) =>
                    opt.text.includes('Frontend')
            );
            expect(frontendOption?.style.paddingLeft).toBe('16px');
        });
    });

    it('should select correct value when value prop is provided', async () => {
        (groupsApi.getAll as any).mockResolvedValue({data: mockGroups});

        const queryClient = createQueryClient();

        renderWithProviders(
                <GroupSelector value={2} onChange={vi.fn()}/>,
                queryClient
        );

        await waitFor(() => {
            const select = screen.getByLabelText('Group') as HTMLSelectElement;
            expect(select.value).toBe('2');
        });
    });
});
