import {fireEvent, render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';
import GroupTree from './GroupTree';
import type {ServiceGroup} from '@/types';

const mockGroups: ServiceGroup[] = [
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
                description: 'Frontend services',
                parentGroupId: 2,
                parentGroupName: 'Development',
                color: '#f59e0b',
                icon: '🎨',
                sortOrder: 1,
                createdAt: '2024-01-01T00:00:00Z',
                updatedAt: '2024-01-01T00:00:00Z',
                serviceCount: 4,
            },
            {
                id: 4,
                name: 'Backend',
                description: 'Backend services',
                parentGroupId: 2,
                parentGroupName: 'Development',
                color: '#ef4444',
                icon: '⚙️',
                sortOrder: 2,
                createdAt: '2024-01-01T00:00:00Z',
                updatedAt: '2024-01-01T00:00:00Z',
                serviceCount: 6,
            },
        ],
    },
];

describe('GroupTree', () => {
    const mockHandlers = {
        onSelectGroup: vi.fn(),
        onEditGroup: vi.fn(),
        onDeleteGroup: vi.fn(),
    };

    it('should render empty state when no groups', () => {
        render(
                <GroupTree
                        groups={[]}
                        onSelectGroup={mockHandlers.onSelectGroup}
                        onEditGroup={mockHandlers.onEditGroup}
                        onDeleteGroup={mockHandlers.onDeleteGroup}
                />
        );

        expect(screen.queryByText('Production')).not.toBeInTheDocument();
    });

    it('should render groups with icons and colors', () => {
        render(
                <GroupTree
                        groups={mockGroups}
                        onSelectGroup={mockHandlers.onSelectGroup}
                        onEditGroup={mockHandlers.onEditGroup}
                        onDeleteGroup={mockHandlers.onDeleteGroup}
                />
        );

        expect(screen.getByText('🌍')).toBeInTheDocument();
        expect(screen.getByText('Production')).toBeInTheDocument();
        expect(screen.getByText('💻')).toBeInTheDocument();
        expect(screen.getByText('Development')).toBeInTheDocument();
    });

    it('should display service count badge', () => {
        render(
                <GroupTree
                        groups={mockGroups}
                        onSelectGroup={mockHandlers.onSelectGroup}
                        onEditGroup={mockHandlers.onEditGroup}
                        onDeleteGroup={mockHandlers.onDeleteGroup}
                />
        );

        expect(screen.getByText('5')).toBeInTheDocument();
        expect(screen.getByText('10')).toBeInTheDocument();
    });

    it('should call onSelectGroup when group is clicked', () => {
        render(
                <GroupTree
                        groups={mockGroups}
                        onSelectGroup={mockHandlers.onSelectGroup}
                        onEditGroup={mockHandlers.onEditGroup}
                        onDeleteGroup={mockHandlers.onDeleteGroup}
                />
        );

        const productionGroup = screen.getByText('Production').closest('div');
        fireEvent.click(productionGroup!);

        expect(mockHandlers.onSelectGroup).toHaveBeenCalledWith(mockGroups[0]);
    });

    it('should highlight selected group', () => {
        render(
                <GroupTree
                        groups={mockGroups}
                        selectedGroupId={1}
                        onSelectGroup={mockHandlers.onSelectGroup}
                        onEditGroup={mockHandlers.onEditGroup}
                        onDeleteGroup={mockHandlers.onDeleteGroup}
                />
        );

        // Find the production group container div that has the highlight classes
        const productionElement = screen.getByText('Production').closest('[class*="bg-blue-100"]');
        expect(productionElement).toHaveClass('bg-blue-100', 'text-blue-900');
    });

    it('should expand/collapse child groups on chevron click', () => {
        render(
                <GroupTree
                        groups={mockGroups}
                        onSelectGroup={mockHandlers.onSelectGroup}
                        onEditGroup={mockHandlers.onEditGroup}
                        onDeleteGroup={mockHandlers.onDeleteGroup}
                />
        );

        // Child groups should be visible by default (isExpanded = true initially)
        expect(screen.getByText('Frontend')).toBeInTheDocument();
        expect(screen.getByText('Backend')).toBeInTheDocument();

        // Find and click the chevron button next to Development
        const developmentElement = screen.getByText('Development').closest('[class*="cursor-pointer"]');
        const chevronButton = developmentElement?.querySelector('button');
        if (chevronButton) {
            fireEvent.click(chevronButton);
        }

        // Child groups should be hidden after collapse
        expect(screen.queryByText('Frontend')).not.toBeInTheDocument();
        expect(screen.queryByText('Backend')).not.toBeInTheDocument();
    });

    it('should call onEditGroup when edit button is clicked', () => {
        render(
                <GroupTree
                        groups={mockGroups}
                        onSelectGroup={mockHandlers.onSelectGroup}
                        onEditGroup={mockHandlers.onEditGroup}
                        onDeleteGroup={mockHandlers.onDeleteGroup}
                />
        );

        // Hover to show edit button (we'll click it directly)
        const editButtons = screen.getAllByTitle('Edit group');
        fireEvent.click(editButtons[0]);

        expect(mockHandlers.onEditGroup).toHaveBeenCalledWith(mockGroups[0]);
    });

    it('should call onDeleteGroup when delete button is clicked and confirmed', () => {
        vi.spyOn(window, 'confirm').mockReturnValue(true);

        render(
                <GroupTree
                        groups={mockGroups}
                        onSelectGroup={mockHandlers.onSelectGroup}
                        onEditGroup={mockHandlers.onEditGroup}
                        onDeleteGroup={mockHandlers.onDeleteGroup}
                />
        );

        const deleteButtons = screen.getAllByTitle('Delete group');
        fireEvent.click(deleteButtons[0]);

        expect(window.confirm).toHaveBeenCalledWith('Delete group "Production"?');
        expect(mockHandlers.onDeleteGroup).toHaveBeenCalledWith(mockGroups[0]);

        vi.restoreAllMocks();
    });

    it('should not delete when confirmation is cancelled', () => {
        vi.spyOn(window, 'confirm').mockReturnValue(false);

        render(
                <GroupTree
                        groups={mockGroups}
                        onSelectGroup={mockHandlers.onSelectGroup}
                        onEditGroup={mockHandlers.onEditGroup}
                        onDeleteGroup={mockHandlers.onDeleteGroup}
                />
        );

        const deleteButtons = screen.getAllByTitle('Delete group');
        fireEvent.click(deleteButtons[0]);

        expect(mockHandlers.onDeleteGroup).not.toHaveBeenCalled();

        vi.restoreAllMocks();
    });

    it('should stop propagation when edit/delete buttons are clicked', () => {
        render(
                <GroupTree
                        groups={mockGroups}
                        onSelectGroup={mockHandlers.onSelectGroup}
                        onEditGroup={mockHandlers.onEditGroup}
                        onDeleteGroup={mockHandlers.onDeleteGroup}
                />
        );

        const editButton = screen.getAllByTitle('Edit group')[0];
        // Click the button - the component should stop propagation internally
        fireEvent.click(editButton);

        // Verify onSelectGroup was NOT called (propagation was stopped)
        expect(mockHandlers.onSelectGroup).not.toHaveBeenCalled();
        // Verify onEditGroup WAS called
        expect(mockHandlers.onEditGroup).toHaveBeenCalledWith(mockGroups[0]);
    });

    it('should render nested groups with proper indentation', () => {
        render(
                <GroupTree
                        groups={mockGroups}
                        onSelectGroup={mockHandlers.onSelectGroup}
                        onEditGroup={mockHandlers.onEditGroup}
                        onDeleteGroup={mockHandlers.onDeleteGroup}
                />
        );

        // Find the container divs with the padding style
        const frontendElement = screen.getByText('Frontend').closest('[style*="padding-left"]');
        const developmentElement = screen.getByText('Development').closest('[style*="padding-left"]');

        // Frontend should have more padding (deeper level)
        expect(frontendElement).toHaveStyle('padding-left: 28px');
        expect(developmentElement).toHaveStyle('padding-left: 12px');
    });

    it('should show chevron only for groups with children', () => {
        render(
                <GroupTree
                        groups={mockGroups}
                        onSelectGroup={mockHandlers.onSelectGroup}
                        onEditGroup={mockHandlers.onEditGroup}
                        onDeleteGroup={mockHandlers.onDeleteGroup}
                />
        );

        // Development has children, should have chevron button (the one with rotate-90 class when expanded)
        const developmentElement = screen.getByText('Development').closest('[class*="cursor-pointer"]');
        const developmentChevron = developmentElement?.querySelector('button[aria-label]') || developmentElement?.querySelector('button:has(svg[class*="rotate"])');
        expect(developmentElement?.querySelector('button')).toBeInTheDocument();

        // Production has no children, should not have chevron button (only edit/delete buttons)
        const productionElement = screen.getByText('Production').closest('[class*="cursor-pointer"]');
        // Production should only have edit/delete buttons, not chevron
        const buttons = productionElement?.querySelectorAll('button') || [];
        // Should have exactly 2 buttons (edit and delete), no chevron
        expect(buttons.length).toBe(2);
    });
});
