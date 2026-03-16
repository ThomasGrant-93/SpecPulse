import {useState} from 'react';
import type {ApplicationSetting, SettingValue} from '@/types';

interface SettingFieldProps {
    setting: ApplicationSetting;
    value: SettingValue;
    onChange: (value: SettingValue) => void;
    onSave: () => void;
    onCancel: () => void;
    isSaving?: boolean;
}

export default function SettingField({
                                         setting,
                                         value,
                                         onChange,
                                         onSave,
                                         onCancel,
                                         isSaving = false,
                                     }: SettingFieldProps) {
    const [jsonError, setJsonError] = useState<string | null>(null);

    const renderInput = () => {
        switch (setting.valueType) {
            case 'boolean':
                return (
                        <input
                                type="checkbox"
                                checked={Boolean(value)}
                                onChange={(e) => onChange(e.target.checked)}
                                className="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                                disabled={!setting.isEditable || isSaving}
                        />
                );

            case 'integer':
                return (
                        <input
                                type="number"
                                value={typeof value === 'number' ? value : ''}
                                onChange={(e) => {
                                    const num = parseInt(e.target.value, 10);
                                    onChange(isNaN(num) ? '' : num);
                                }}
                                className="w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm border p-2"
                                disabled={!setting.isEditable || isSaving}
                        />
                );

            case 'array':
                return (
                        <div className="space-y-1">
                        <textarea
                                value={Array.isArray(value) ? JSON.stringify(value, null, 2) : '[]'}
                                onChange={(e) => {
                                    try {
                                        const parsed = JSON.parse(e.target.value);
                                        if (!Array.isArray(parsed)) {
                                            setJsonError('Value must be an array');
                                            return;
                                        }
                                        setJsonError(null);
                                        onChange(parsed);
                                    } catch (error) {
                                        setJsonError('Invalid JSON format');
                                    }
                                }}
                                rows={3}
                                className="w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm border p-2 font-mono"
                                disabled={!setting.isEditable || isSaving}
                                placeholder='["item1", "item2"]'
                        />
                            {jsonError && (
                                    <p className="text-sm text-red-600">{jsonError}</p>
                            )}
                        </div>
                );

            case 'string':
            default:
                // Check if it's a known enum
                if (setting.key.includes('theme')) {
                    return (
                            <select
                                    value={typeof value === 'string' ? value : ''}
                                    onChange={(e) => onChange(e.target.value)}
                                    className="w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm border p-2"
                                    disabled={!setting.isEditable || isSaving}
                            >
                                <option value="light">Light</option>
                                <option value="dark">Dark</option>
                                <option value="system">System</option>
                            </select>
                    );
                }
                if (setting.key.includes('provider')) {
                    return (
                            <select
                                    value={typeof value === 'string' ? value : ''}
                                    onChange={(e) => onChange(e.target.value)}
                                    className="w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm border p-2"
                                    disabled={!setting.isEditable || isSaving}
                            >
                                <option value="internal">Internal</option>
                                <option value="ldap">LDAP</option>
                                <option value="oauth2">OAuth2</option>
                                <option value="oidc">OpenID Connect</option>
                            </select>
                    );
                }
                if (setting.key.includes('cron')) {
                    return (
                            <div>
                                <input
                                        type="text"
                                        value={typeof value === 'string' ? value : ''}
                                        onChange={(e) => onChange(e.target.value)}
                                        className="w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm border p-2 font-mono"
                                        disabled={!setting.isEditable || isSaving}
                                        placeholder="0 0 */6 * * ?"
                                />
                                <p className="mt-1 text-xs text-gray-500">
                                    Cron expression (e.g., "0 0 */6 * * ?" for every 6 hours)
                                </p>
                            </div>
                    );
                }
                return (
                        <input
                                type="text"
                                value={typeof value === 'string' ? value : ''}
                                onChange={(e) => onChange(e.target.value)}
                                className="w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm border p-2"
                                disabled={!setting.isEditable || isSaving}
                        />
                );
        }
    };

    return (
            <div className="py-4 border-b border-gray-200 last:border-0">
                <div className="flex items-start justify-between gap-4">
                    <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-2">
                            <h4 className="text-sm font-medium text-gray-900">{setting.key}</h4>
                            {!setting.isEditable && (
                                    <span className="inline-flex items-center rounded-md bg-gray-100 px-2 py-1 text-xs font-medium text-gray-600">
                                System
                            </span>
                            )}
                            {setting.isPublic && (
                                    <span className="inline-flex items-center rounded-md bg-blue-50 px-2 py-1 text-xs font-medium text-blue-600">
                                Public
                            </span>
                            )}
                        </div>
                        {setting.description && (
                                <p className="mt-1 text-sm text-gray-500">{setting.description}</p>
                        )}
                        <p className="mt-1 text-xs text-gray-400">Type: {setting.valueType}</p>
                    </div>
                    <div className="flex items-center gap-2">
                        {renderInput()}
                    </div>
                </div>
                <div className="mt-3 flex justify-end gap-2">
                    <button
                            onClick={onCancel}
                            className="rounded-md bg-white px-3 py-1.5 text-sm font-semibold text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 hover:bg-gray-50"
                            disabled={isSaving}
                    >
                        Cancel
                    </button>
                    <button
                            onClick={onSave}
                            className="rounded-md bg-blue-600 px-3 py-1.5 text-sm font-semibold text-white shadow-sm hover:bg-blue-500 disabled:opacity-50"
                            disabled={!setting.isEditable || isSaving}
                    >
                        {isSaving ? 'Saving...' : 'Save'}
                    </button>
                </div>
            </div>
    );
}
