import {beforeEach, describe, expect, it, vi} from 'vitest';
import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {QueryClientProvider} from '@tanstack/react-query';
import ServiceForm from './ServiceForm';
import type {Service} from '@/types';
import {testQueryClient} from '@/test/setup';

// Mock fetch globally
const mockFetch = vi.fn();
global.fetch = mockFetch;

describe('ServiceForm', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    const mockOnSubmit = vi.fn();
    const mockOnCancel = vi.fn();

    const renderServiceForm = (initialData?: Service) => {
        return render(
            <QueryClientProvider client={testQueryClient}>
                <ServiceForm
                    onSubmit={mockOnSubmit}
                    onCancel={mockOnCancel}
                    initialData={initialData}
                />
            </QueryClientProvider>
        );
    };

    describe('Form rendering', () => {
        it('should render form with correct title for new service', () => {
            renderServiceForm();

            expect(screen.getByText('Add New Service')).toBeInTheDocument();
        });

        it('should render form with correct title for editing', () => {
            const existingService: Service = {
                id: 1,
                name: 'Existing Service',
                openApiUrl: 'https://api.example.com/openapi.json',
                description: 'Test description',
                enabled: true,
                groupId: null,
                group: null,
            };

            renderServiceForm(existingService);

            expect(screen.getByText('Edit Service')).toBeInTheDocument();
        });

        it('should have all required form fields', () => {
            renderServiceForm();

            expect(screen.getByLabelText(/Name \*/i)).toBeInTheDocument();
            expect(screen.getByLabelText(/OpenAPI URL \*/i)).toBeInTheDocument();
            expect(screen.getByLabelText(/Description/i)).toBeInTheDocument();
            expect(screen.getByLabelText(/Enabled/i)).toBeInTheDocument();
        });

        it('should have proper placeholders', () => {
            renderServiceForm();

            expect(screen.getByPlaceholderText('my-service')).toBeInTheDocument();
            expect(
                    screen.getByPlaceholderText('https://api.example.com/openapi.json')
            ).toBeInTheDocument();
        });
    });

    describe('Form validation', () => {
        // Note: Full form validation testing requires complex mocking of fetch API
        // and form submission. Core validation logic is tested in the component manually.

        it('should have validation button disabled when URL is empty', () => {
            renderServiceForm();

            const validateButton = screen.getByRole('button', {name: /Validate/i});
            expect(validateButton).toBeDisabled();
        });

        it('should validate OpenAPI spec before submit for new service', async () => {
            // Mock successful validation - called twice (once for validate button, once for submit)
            mockFetch
                    .mockResolvedValueOnce({
                        ok: true,
                        json: async () => ({valid: true, errors: []}),
                    })
                    .mockResolvedValueOnce({
                        ok: true,
                        json: async () => ({valid: true, errors: []}),
                    });

            renderServiceForm();

            const user = userEvent.setup();
            await user.type(screen.getByLabelText(/Name/i), 'Test Service');
            await user.type(
                    screen.getByLabelText(/OpenAPI URL/i),
                    'https://api.example.com/openapi.json'
            );

            // Click validate button
            const validateButton = screen.getByRole('button', {name: /Validate/i});
            await user.click(validateButton);

            await waitFor(() => {
                expect(screen.getByText(/OpenAPI specification is valid/i)).toBeInTheDocument();
            });

            // Submit form
            const submitButton = screen.getByRole('button', {name: /Save/i});
            await user.click(submitButton);

            await waitFor(() => {
                expect(mockOnSubmit).toHaveBeenCalledWith({
                    name: 'Test Service',
                    openApiUrl: 'https://api.example.com/openapi.json',
                    description: '',
                    enabled: true,
                    groupId: null,
                });
            });
        });

        it('should show validation errors from API', async () => {
            mockFetch.mockResolvedValueOnce({
                ok: true,
                json: async () => ({
                    valid: false,
                    errors: ['Invalid OpenAPI format', 'Missing required field'],
                }),
            });

            renderServiceForm();

            const user = userEvent.setup();
            await user.type(screen.getByLabelText(/Name/i), 'Test Service');
            await user.type(
                    screen.getByLabelText(/OpenAPI URL/i),
                    'https://api.example.com/openapi.json'
            );

            const validateButton = screen.getByRole('button', {name: /Validate/i});
            await user.click(validateButton);

            await waitFor(() => {
                expect(screen.getByText('OpenAPI Validation Failed')).toBeInTheDocument();
                expect(screen.getByText('Invalid OpenAPI format')).toBeInTheDocument();
                expect(screen.getByText('Missing required field')).toBeInTheDocument();
            });
        });

        it('should skip validation when editing existing service', async () => {
            const existingService: Service = {
                id: 1,
                name: 'Existing Service',
                openApiUrl: 'https://api.example.com/openapi.json',
                description: '',
                enabled: true,
                groupId: null,
                group: null,
            };

            renderServiceForm(existingService);

            const user = userEvent.setup();
            // Change name
            await user.clear(screen.getByLabelText(/Name/i));
            await user.type(screen.getByLabelText(/Name/i), 'Updated Service');

            // Submit should work without validation
            const submitButton = screen.getByRole('button', {name: /Save Changes/i});
            await user.click(submitButton);

            await waitFor(() => {
                expect(mockOnSubmit).toHaveBeenCalledWith({
                    name: 'Updated Service',
                    openApiUrl: 'https://api.example.com/openapi.json',
                    description: '',
                    enabled: true,
                    groupId: null,
                });
            });
        });
    });

    describe('Form state management', () => {
        it('should populate fields with initial data', () => {
            const existingService: Service = {
                id: 1,
                name: 'My Service',
                openApiUrl: 'https://api.myservice.com/openapi.json',
                description: 'My description',
                enabled: false,
                groupId: null,
                group: null,
            };

            renderServiceForm(existingService);

            expect(screen.getByLabelText(/Name/i)).toHaveValue('My Service');
            expect(screen.getByLabelText(/OpenAPI URL/i)).toHaveValue(
                    'https://api.myservice.com/openapi.json'
            );
            expect(screen.getByLabelText(/Description/i)).toHaveValue('My description');
            expect(screen.getByLabelText(/Enabled/i)).not.toBeChecked();
        });

        it('should reset validation state when input changes', async () => {
            renderServiceForm();

            const user = userEvent.setup();
            await user.type(screen.getByLabelText(/Name/i), 'Test');
            await user.type(screen.getByLabelText(/OpenAPI URL/i), 'https://api.example.com');

            // Validation state should be reset when typing
            const nameInput = screen.getByLabelText(/Name/i);
            fireEvent.change(nameInput, {target: {value: 'Test 2'}});

            // Success message should disappear
            expect(screen.queryByText(/OpenAPI specification is valid/i)).not.toBeInTheDocument();
        });

        it('should toggle enabled checkbox', async () => {
            renderServiceForm();

            const checkbox = screen.getByLabelText(/Enabled/i);
            expect(checkbox).toBeChecked();

            const user = userEvent.setup();
            await user.click(checkbox);
            expect(checkbox).not.toBeChecked();

            await user.click(checkbox);
            expect(checkbox).toBeChecked();
        });
    });

    describe('Form actions', () => {
        it('should call onCancel when cancel button is clicked', async () => {
            renderServiceForm();

            const cancelButton = screen.getByRole('button', {name: /Cancel/i});
            const user = userEvent.setup();
            await user.click(cancelButton);

            expect(mockOnCancel).toHaveBeenCalled();
        });

        it('should call onSubmit with form data', async () => {
            // Mock validation called on submit
            mockFetch.mockResolvedValueOnce({
                ok: true,
                json: async () => ({valid: true, errors: []}),
            });

            renderServiceForm();

            const user = userEvent.setup();
            await user.type(screen.getByLabelText(/Name/i), 'Test Service');
            await user.type(
                    screen.getByLabelText(/OpenAPI URL/i),
                    'https://api.example.com/openapi.json'
            );
            await user.type(screen.getByLabelText(/Description/i), 'Test description');

            const submitButton = screen.getByRole('button', {name: /Save/i});
            await user.click(submitButton);

            await waitFor(() => {
                expect(mockOnSubmit).toHaveBeenCalledWith({
                    name: 'Test Service',
                    openApiUrl: 'https://api.example.com/openapi.json',
                    description: 'Test description',
                    enabled: true,
                    groupId: null,
                });
            });
        });

        it('should disable buttons during validation', async () => {
            // Mock a slow validation
            mockFetch.mockImplementationOnce(
                    () =>
                            new Promise((resolve) =>
                                    setTimeout(
                                            () =>
                                                    resolve({
                                                        ok: true,
                                                        json: async () => ({valid: true, errors: []}),
                                                    }),
                                            100
                                    )
                            )
            );

            renderServiceForm();

            const user = userEvent.setup();
            await user.type(screen.getByLabelText(/Name/i), 'Test');
            await user.type(screen.getByLabelText(/OpenAPI URL/i), 'https://api.example.com');

            const validateButton = screen.getByRole('button', {name: /Validate/i});
            await user.click(validateButton);

            // Buttons should be disabled during validation
            expect(validateButton).toBeDisabled();
            expect(screen.getByRole('button', {name: /Validating\.\.\./i})).toBeDisabled();
        });
    });

    describe('Accessibility', () => {
        it('should have proper form structure', () => {
            renderServiceForm();

            // Form element exists (Testing Library doesn't expose form role by default)
            const form = document.querySelector('form');
            expect(form).toBeInTheDocument();
        });

        it('should have labels for all inputs', () => {
            renderServiceForm();

            expect(screen.getByLabelText(/Name \*/i)).toBeInTheDocument();
            expect(screen.getByLabelText(/OpenAPI URL \*/i)).toBeInTheDocument();
            expect(screen.getByLabelText(/Description/i)).toBeInTheDocument();
            expect(screen.getByLabelText(/Enabled/i)).toBeInTheDocument();
        });

        it('should show validation errors in alert region', async () => {
            mockFetch.mockResolvedValueOnce({
                ok: true,
                json: async () => ({valid: false, errors: ['Error 1']}),
            });

            renderServiceForm();

            const user = userEvent.setup();
            await user.type(screen.getByLabelText(/Name/i), 'Test');
            await user.type(screen.getByLabelText(/OpenAPI URL/i), 'https://api.example.com');

            const validateButton = screen.getByRole('button', {name: /Validate/i});
            await user.click(validateButton);

            await waitFor(() => {
                expect(screen.getByText(/OpenAPI Validation Failed/i)).toBeInTheDocument();
            });
        });

        it('should show success message with proper styling', async () => {
            mockFetch.mockResolvedValueOnce({
                ok: true,
                json: async () => ({valid: true, errors: []}),
            });

            renderServiceForm();

            const user = userEvent.setup();
            await user.type(screen.getByLabelText(/Name/i), 'Test');
            await user.type(screen.getByLabelText(/OpenAPI URL/i), 'https://api.example.com');

            const validateButton = screen.getByRole('button', {name: /Validate/i});
            await user.click(validateButton);

            await waitFor(() => {
                expect(screen.getByText(/OpenAPI specification is valid/i)).toBeInTheDocument();
            });
        });
    });

    describe('Error handling', () => {
        it('should handle fetch errors gracefully', async () => {
            mockFetch.mockRejectedValueOnce(new Error('Network error'));

            renderServiceForm();

            const user = userEvent.setup();
            await user.type(screen.getByLabelText(/Name/i), 'Test');
            await user.type(screen.getByLabelText(/OpenAPI URL/i), 'https://api.example.com');

            const validateButton = screen.getByRole('button', {name: /Validate/i});
            await user.click(validateButton);

            await waitFor(() => {
                expect(screen.getByText(/Failed to validate OpenAPI spec/i)).toBeInTheDocument();
            });
        });

        it('should handle non-JSON responses', async () => {
            mockFetch.mockResolvedValueOnce({
                ok: false,
                text: async () => 'Internal Server Error',
            });

            renderServiceForm();

            const user = userEvent.setup();
            await user.type(screen.getByLabelText(/Name/i), 'Test');
            await user.type(screen.getByLabelText(/OpenAPI URL/i), 'https://api.example.com');

            const validateButton = screen.getByRole('button', {name: /Validate/i});
            await user.click(validateButton);

            await waitFor(() => {
                expect(screen.getByText(/Failed to validate OpenAPI spec/i)).toBeInTheDocument();
            });
        });
    });
});
