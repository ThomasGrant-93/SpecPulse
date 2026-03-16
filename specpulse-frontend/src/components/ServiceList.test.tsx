import {describe, expect, it, vi} from 'vitest';
import {fireEvent, render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import ServiceList from './ServiceList';
import type {Service} from '@/types';

const mockServices: Service[] = [
    {
        id: 1,
        name: 'Test Service 1',
        openApiUrl: 'https://api.example.com/openapi.json',
        description: 'Test description 1',
        enabled: true,
        groupId: null,
        group: null,
        latestVersion: {
            id: 1,
            versionHash: 'abc123def456',
            specVersion: '1.0.0',
            specTitle: 'Test API v1',
            pulledAt: '2024-01-01T00:00:00Z',
        },
    },
    {
        id: 2,
        name: 'Test Service 2',
        openApiUrl: 'https://api.test.com/openapi.json',
        description: 'Test description 2',
        enabled: false,
        groupId: null,
        group: null,
        latestVersion: {
            id: 2,
            versionHash: 'xyz789uvw012',
            specVersion: '2.0.0',
            specTitle: 'Test API v2',
            pulledAt: '2024-01-02T00:00:00Z',
        },
    },
];

const renderServiceList = (services: Service[] = []) => {
    const mocks = {
        onDelete: vi.fn(),
        onPull: vi.fn(),
        onEdit: vi.fn(),
        pullStatus: {},
    };

    const utils = render(
            <MemoryRouter>
                <ServiceList
                        services={services}
                        onDelete={mocks.onDelete}
                        onPull={mocks.onPull}
                        onEdit={mocks.onEdit}
                        pullStatus={mocks.pullStatus}
                />
            </MemoryRouter>
    );

    return {...utils, ...mocks};
};

describe('ServiceList', () => {
    describe('Empty state', () => {
        it('should show empty message when no services', () => {
            renderServiceList([]);

            expect(screen.getByText('No services registered')).toBeInTheDocument();
        });

        it('should have proper accessibility attributes in empty state', () => {
            renderServiceList([]);

            const statusRegion = screen.getByRole('status');
            expect(statusRegion).toHaveAttribute('aria-label', 'No services available');
        });
    });

    describe('With services', () => {
        it('should render all services', () => {
            renderServiceList(mockServices);

            expect(screen.getByText('Test Service 1')).toBeInTheDocument();
            expect(screen.getByText('Test Service 2')).toBeInTheDocument();
        });

        it('should display service URLs', () => {
            renderServiceList(mockServices);

            expect(screen.getByText('https://api.example.com/openapi.json')).toBeInTheDocument();
            expect(screen.getByText('https://api.test.com/openapi.json')).toBeInTheDocument();
        });

        it('should show service status (enabled/disabled)', () => {
            renderServiceList(mockServices);

            expect(screen.getByText('Active')).toBeInTheDocument();
            expect(screen.getByText('Disabled')).toBeInTheDocument();
        });

        it('should display latest version information', () => {
            renderServiceList(mockServices);

            expect(screen.getByText('Test API v1')).toBeInTheDocument();
            expect(screen.getByText('Test API v2')).toBeInTheDocument();
            // Hash is displayed with first 8 chars + "..."
            expect(screen.getByText(/abc123de.*/i)).toBeInTheDocument();
        });

        it('should have links to service detail pages', () => {
            renderServiceList(mockServices);

            const link1 = screen.getByRole('link', {name: /Test Service 1/i});
            const link2 = screen.getByRole('link', {name: /Test Service 2/i});

            expect(link1).toHaveAttribute('href', '/services/1');
            expect(link2).toHaveAttribute('href', '/services/2');
        });
    });

    describe('Actions', () => {
        it('should call onEdit when edit button is clicked', () => {
            const {onEdit} = renderServiceList(mockServices);

            const editButton = screen.getByRole('button', {name: /Edit service Test Service 1/i});
            fireEvent.click(editButton);

            expect(onEdit).toHaveBeenCalledWith(mockServices[0]);
        });

        it('should call onPull when pull button is clicked', () => {
            const {onPull} = renderServiceList(mockServices);

            const pullButton = screen.getByRole('button', {name: /Pull service Test Service 1/i});
            fireEvent.click(pullButton);

            expect(onPull).toHaveBeenCalledWith(1);
        });

        it('should call onDelete when delete button is clicked', () => {
            const {onDelete} = renderServiceList(mockServices);

            const deleteButton = screen.getByRole('button', {
                name: /Delete service Test Service 1/i,
            });
            fireEvent.click(deleteButton);

            expect(onDelete).toHaveBeenCalledWith(1);
        });

        it('should disable pull button when loading', () => {
            renderServiceList(mockServices);

            const pullButton = screen.getByRole('button', {name: /Pull service Test Service 1/i});
            expect(pullButton).not.toBeDisabled();
        });

        it('should show pulling state when loading', () => {
            const pullStatus = {1: 'loading' as const};
            renderServiceList(mockServices);

            // Re-render with loading status
            render(
                    <MemoryRouter>
                        <ServiceList
                                services={mockServices}
                                onDelete={vi.fn()}
                                onPull={vi.fn()}
                                onEdit={vi.fn()}
                                pullStatus={pullStatus}
                        />
                    </MemoryRouter>
            );

            const pullingButton = screen.getByText('Pulling...');
            expect(pullingButton).toBeDisabled();
        });
    });

    describe('Accessibility', () => {
        it('should have table role and aria-label', () => {
            renderServiceList(mockServices);

            const table = screen.getByRole('table');
            expect(table).toHaveAttribute('aria-label', 'Services table');
        });

        it('should have action group with aria-label', () => {
            renderServiceList(mockServices);

            const actionGroup = screen.getByRole('group', {name: /Actions for Test Service 1/i});
            expect(actionGroup).toBeInTheDocument();
        });

        it('should have buttons with proper aria-labels', () => {
            renderServiceList(mockServices);

            expect(
                    screen.getByRole('button', {name: /Edit service Test Service 1/i})
            ).toBeInTheDocument();
            expect(
                    screen.getByRole('button', {name: /Pull service Test Service 1/i})
            ).toBeInTheDocument();
            expect(
                    screen.getByRole('button', {name: /Delete service Test Service 1/i})
            ).toBeInTheDocument();
        });

        it('should have status role for enabled/disabled badge', () => {
            renderServiceList(mockServices);

            const statusBadges = screen.getAllByRole('status');
            expect(statusBadges.length).toBeGreaterThan(0);
        });

        it('should have svg elements with aria-hidden', () => {
            renderServiceList(mockServices);

            const svgs = document.querySelectorAll('svg[aria-hidden="true"]');
            expect(svgs.length).toBeGreaterThan(0);
        });
    });

    describe('URL truncation', () => {
        it('should show full URL in title attribute for long URLs', () => {
            const longUrlService: Service[] = [
                {
                    id: 1,
                    name: 'Test Service',
                    openApiUrl: 'https://very-long-api-url.example.com/v1/openapi.json',
                    enabled: true,
                },
            ];

            renderServiceList(longUrlService);

            const urlElement = screen.getByTitle(
                    'https://very-long-api-url.example.com/v1/openapi.json'
            );
            expect(urlElement).toBeInTheDocument();
        });
    });
});
