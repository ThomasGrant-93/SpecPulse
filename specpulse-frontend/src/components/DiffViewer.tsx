import type {SpecDiff} from '@/types';

interface DiffViewerProps {
    diffs: SpecDiff[];
}

export default function DiffViewer({diffs}: DiffViewerProps) {
    if (diffs.length === 0) {
        return (
                <div className="text-center py-12 bg-white rounded-lg">
                    <svg
                            className="mx-auto h-12 w-12 text-gray-400"
                            fill="none"
                            stroke="currentColor"
                            viewBox="0 0 24 24"
                    >
                        <path
                                strokeLinecap="round"
                                strokeLinejoin="round"
                                strokeWidth={2}
                                d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
                        />
                    </svg>
                    <p className="mt-4 text-gray-500">No comparisons yet</p>
                    <p className="mt-2 text-sm text-gray-400">
                        Comparisons will appear here when new versions are pulled
                    </p>
                </div>
        );
    }

    return (
            <div className="divide-y">
                {diffs.map((diff) => (
                        <div
                                key={diff.id}
                                className={`p-4 ${diff.hasBreakingChanges ? 'bg-red-50' : 'bg-green-50'}`}
                        >
                            <div className="flex items-center justify-between mb-3">
                                <div className="flex items-center gap-3">
                            <span
                                    className={`text-lg ${diff.hasBreakingChanges ? 'text-red-600' : 'text-green-600'}`}
                            >
                                {diff.hasBreakingChanges ? '⚠️' : '✓'}
                            </span>
                                    <div>
                                        <h3 className="text-sm font-semibold">
                                            {diff.hasBreakingChanges ? (
                                                    <span className="text-red-800">
                                            Breaking Changes Detected
                                        </span>
                                            ) : (
                                                    <span className="text-green-800">Compatible Changes</span>
                                            )}
                                        </h3>
                                        <p className="text-xs text-gray-600 mt-0.5">
                                            {new Date(diff.createdAt).toLocaleString()}
                                        </p>
                                    </div>
                                </div>
                                {diff.breakingChangesCount > 0 && (
                                        <span className="inline-flex items-center rounded-md bg-red-100 px-2.5 py-1 text-xs font-medium text-red-800">
                                {diff.breakingChangesCount} breaking
                            </span>
                                )}
                            </div>
                            {diff.diffContent && (
                                    <details className="mt-3">
                                        <summary
                                                className="cursor-pointer text-sm text-blue-600 hover:text-blue-800 font-medium">
                                            View diff details
                                        </summary>
                                        <pre className="mt-3 overflow-x-auto rounded bg-gray-900 p-3 text-xs text-gray-100 max-h-96 whitespace-pre-wrap break-all">
                                {diff.diffContent}
                            </pre>
                                    </details>
                            )}
                        </div>
                ))}
            </div>
    );
}
