import {ReactNode, useEffect, useRef} from 'react';

interface ModalProps {
    isOpen: boolean;
    onClose: () => void;
    title: string;
    children: ReactNode;
    actions?: ReactNode;
    size?: 'sm' | 'md' | 'lg';
}

/**
 * Reusable Modal component with accessibility support
 * - Focus trap inside modal
 * - Escape key to close
 * - Click outside to close
 * - Proper ARIA attributes
 * - Prevents body scroll when open
 */
export function Modal({
                          isOpen,
                          onClose,
                          title,
                          children,
                          actions,
                          size = 'md',
                      }: ModalProps) {
    const modalRef = useRef<HTMLDivElement>(null);
    const previousActiveElement = useRef<HTMLElement | null>(null);

    // Store previous focus and focus modal on open
    useEffect(() => {
        if (isOpen) {
            previousActiveElement.current = document.activeElement as HTMLElement;
            modalRef.current?.focus();
        }
    }, [isOpen]);

    // Restore focus on close
    useEffect(() => {
        return () => {
            if (previousActiveElement.current) {
                previousActiveElement.current.focus();
            }
        };
    }, []);

    // Handle escape key and focus trap
    useEffect(() => {
        if (!isOpen) return;

        const handleKeyDown = (e: KeyboardEvent) => {
            if (e.key === 'Escape') {
                onClose();
                return;
            }

            // Focus trap
            if (e.key === 'Tab') {
                const focusableElements = modalRef.current?.querySelectorAll<HTMLElement>(
                        'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
                );

                if (focusableElements && focusableElements.length > 0) {
                    const firstElement = focusableElements[0];
                    const lastElement = focusableElements[focusableElements.length - 1];

                    if (e.shiftKey && document.activeElement === firstElement) {
                        e.preventDefault();
                        lastElement.focus();
                    } else if (!e.shiftKey && document.activeElement === lastElement) {
                        e.preventDefault();
                        firstElement.focus();
                    }
                }
            }
        };

        document.addEventListener('keydown', handleKeyDown);
        document.body.style.overflow = 'hidden';

        return () => {
            document.removeEventListener('keydown', handleKeyDown);
            document.body.style.overflow = 'unset';
        };
    }, [isOpen, onClose]);

    if (!isOpen) return null;

    const sizeClasses = {
        sm: 'sm:max-w-md',
        md: 'sm:max-w-lg',
        lg: 'sm:max-w-2xl',
    };

    return (
            <div
                    className="fixed inset-0 bg-gray-500 bg-opacity-75 transition-opacity z-50 flex items-center justify-center p-4"
                    role="dialog"
                    aria-modal="true"
                    aria-labelledby="modal-title"
                    onClick={onClose}
            >
                <div
                        ref={modalRef}
                        className={`relative transform overflow-hidden rounded-lg bg-white shadow-xl transition-all sm:w-full ${sizeClasses[size]}`}
                        role="document"
                        onClick={(e) => e.stopPropagation()}
                        tabIndex={-1}
                >
                    <div className="bg-white px-4 pb-4 pt-5 sm:p-6">
                        <div className="sm:flex sm:items-start">
                            <div className="w-full">
                                <h3
                                        id="modal-title"
                                        className="text-lg font-semibold leading-6 text-gray-900"
                                >
                                    {title}
                                </h3>
                                <div className="mt-4">{children}</div>
                            </div>
                        </div>
                    </div>
                    {actions && (
                            <div className="bg-gray-50 px-4 py-3 sm:flex sm:flex-row-reverse sm:px-6 border-t border-gray-200">
                                {actions}
                            </div>
                    )}
                </div>
            </div>
    );
}

export default Modal;
