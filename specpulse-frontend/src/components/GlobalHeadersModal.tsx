import { useMemo, useState } from 'react';
import { Modal } from '@/components/Modal';
import { useHeadersSession } from '@/features/spec/HeadersProvider';
import { isSensitiveHeaderName, isValidHeaderName, normalizeHeaderName } from '@/utils/headerUtils';

type GlobalHeadersModalProps = {
    isOpen: boolean;
    onClose: () => void;
};

export function GlobalHeadersModal({ isOpen, onClose }: GlobalHeadersModalProps) {
    const { state, upsertGlobalHeader, removeGlobalHeader } = useHeadersSession();
    const [newName, setNewName] = useState('');
    const [newValue, setNewValue] = useState('');
    const [enabled, setEnabled] = useState(true);
    const [formError, setFormError] = useState<string | null>(null);

    const globalHeadersList = useMemo(() => {
        return Object.values(state.global).sort((a, b) => a.name.localeCompare(b.name));
    }, [state.global]);

    const handleAdd = () => {
        setFormError(null);
        const name = newName.trim();
        if (!name) {
            setFormError('Header name is required');
            return;
        }
        if (!isValidHeaderName(name)) {
            setFormError('Invalid header name. Use letters/numbers and dashes only.');
            return;
        }

        upsertGlobalHeader({
            name,
            value: newValue,
            enabled,
            required: false,
        });

        setNewName('');
        setNewValue('');
        setEnabled(true);
    };

    const handleRemove = (name: string) => {
        removeGlobalHeader(name);
    };

    return (
        <Modal isOpen={isOpen} onClose={onClose} title="Global Request Headers" size="lg">
            <div className="space-y-4">
                {formError && <div className="text-sm text-red-600">{formError}</div>}

                {globalHeadersList.length === 0 ? (
                    <div className="text-sm text-gray-600">No global headers configured.</div>
                ) : (
                    <div className="space-y-3">
                        {globalHeadersList.map((h) => (
                            <div key={normalizeHeaderName(h.name)} className="flex items-start gap-3">
                                <div className="min-w-0 flex-1">
                                    <div className="flex items-center justify-between gap-3">
                                        <div className="font-mono text-sm text-gray-900 truncate">{h.name}</div>
                                        <label className="flex items-center gap-2 text-xs text-gray-600">
                                            <input
                                                type="checkbox"
                                                checked={h.enabled}
                                                onChange={(e) => {
                                                    upsertGlobalHeader({
                                                        name: h.name,
                                                        value: h.value,
                                                        enabled: e.target.checked,
                                                        required: false,
                                                    });
                                                }}
                                            />
                                            Enabled
                                        </label>
                                    </div>
                                    <input
                                        type={isSensitiveHeaderName(h.name) ? 'password' : 'text'}
                                        value={h.value}
                                        onChange={(e) => {
                                            upsertGlobalHeader({
                                                name: h.name,
                                                value: e.target.value,
                                                enabled: h.enabled,
                                                required: false,
                                            });
                                        }}
                                        className="mt-2 w-full rounded border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm border p-2 font-mono"
                                        placeholder={isSensitiveHeaderName(h.name) ? '********' : 'Enter value'}
                                        aria-label={`Value for header ${h.name}`}
                                    />
                                    {isSensitiveHeaderName(h.name) && (
                                        <div className="mt-1 text-xs text-gray-500">Sensitive value (masked)</div>
                                    )}
                                </div>
                                <button
                                    type="button"
                                    onClick={() => handleRemove(h.name)}
                                    className="rounded-md bg-white px-3 py-1.5 text-sm font-semibold text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 hover:bg-gray-50"
                                >
                                    Remove
                                </button>
                            </div>
                        ))}
                    </div>
                )}

                <div className="pt-3 border-t border-gray-200">
                    <h4 className="text-sm font-semibold text-gray-900">Add Header</h4>
                    <div className="mt-3 grid grid-cols-1 gap-3 sm:grid-cols-3">
                        <div className="sm:col-span-1">
                            <label className="block text-xs font-medium text-gray-700">Header Name</label>
                            <input
                                value={newName}
                                onChange={(e) => setNewName(e.target.value)}
                                className="mt-1 w-full rounded border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm border p-2 font-mono"
                                placeholder="Authorization"
                            />
                        </div>
                        <div className="sm:col-span-2">
                            <label className="block text-xs font-medium text-gray-700">Header Value</label>
                            <input
                                type={newName && isSensitiveHeaderName(newName) ? 'password' : 'text'}
                                value={newValue}
                                onChange={(e) => setNewValue(e.target.value)}
                                className="mt-1 w-full rounded border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm border p-2 font-mono"
                                placeholder={newName && isSensitiveHeaderName(newName) ? '********' : 'Enter value'}
                            />
                            {newName && isSensitiveHeaderName(newName) && (
                                <div className="mt-1 text-xs text-gray-500">Sensitive value (masked)</div>
                            )}
                        </div>
                    </div>

                    <div className="mt-3 flex items-center justify-between gap-3">
                        <label className="flex items-center gap-2 text-sm text-gray-700">
                            <input type="checkbox" checked={enabled} onChange={(e) => setEnabled(e.target.checked)} />
                            Enabled
                        </label>
                        <button
                            type="button"
                            onClick={handleAdd}
                            className="rounded-md bg-blue-600 px-3 py-2 text-sm font-semibold text-white shadow-sm hover:bg-blue-500 disabled:opacity-50"
                        >
                            Add
                        </button>
                    </div>

                    <div className="mt-2 text-xs text-gray-500">
                        Authorization convenience: if you add an <span className="font-mono">Authorization</span> header and the value doesn’t start with <span className="font-mono">Bearer </span>, we send <span className="font-mono">Bearer {'<token>'}</span>.
                    </div>
                </div>
            </div>
        </Modal>
    );
}
