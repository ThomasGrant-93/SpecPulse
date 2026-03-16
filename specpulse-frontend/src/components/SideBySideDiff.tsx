import {useMemo, useState} from 'react';

interface SideBySideDiffProps {
    oldSpec: string;
    newSpec: string;
}

interface DiffLine {
    type: 'unchanged' | 'added' | 'removed' | 'section-header';
    oldLineNumber?: number;
    newLineNumber?: number;
    oldContent?: string;
    newContent?: string;
    path?: string;
}

export default function SideBySideDiff({oldSpec, newSpec}: SideBySideDiffProps) {
    const [collapsedSections, setCollapsedSections] = useState<Set<number>>(new Set());

    // Parse diff into lines
    const diffLines = useMemo(() => {
        return parseDiff(oldSpec, newSpec);
    }, [oldSpec, newSpec]);

    // Group lines into collapsible sections
    const sections = useMemo(() => {
        const result: { lines: DiffLine[]; hasChanges: boolean; startIdx: number }[] = [];
        let currentSection: DiffLine[] = [];
        let hasChanges = false;
        let sectionStartIdx = 0;

        diffLines.forEach((line, idx) => {
            if (line.type === 'section-header') {
                if (currentSection.length > 0) {
                    result.push({lines: currentSection, hasChanges, startIdx: sectionStartIdx});
                }
                currentSection = [line];
                hasChanges = false;
                sectionStartIdx = idx;
            } else {
                currentSection.push(line);
                if (line.type === 'added' || line.type === 'removed') {
                    hasChanges = true;
                }
            }
        });

        if (currentSection.length > 0) {
            result.push({lines: currentSection, hasChanges, startIdx: sectionStartIdx});
        }

        return result;
    }, [diffLines]);

    const toggleCollapse = (sectionIdx: number) => {
        setCollapsedSections((prev) => {
            const next = new Set(prev);
            if (next.has(sectionIdx)) {
                next.delete(sectionIdx);
            } else {
                next.add(sectionIdx);
            }
            return next;
        });
    };

    const collapseAll = () => {
        const toCollapse = new Set<number>();
        sections.forEach((section, idx) => {
            if (!section.hasChanges) {
                toCollapse.add(idx);
            }
        });
        setCollapsedSections(toCollapse);
    };

    const expandAll = () => {
        setCollapsedSections(new Set());
    };

    return (
            <div className="border rounded-lg overflow-hidden">
                {/* Toolbar */}
                <div className="bg-gray-50 border-b px-4 py-2 flex items-center justify-between">
                    <div className="flex items-center gap-4">
                        <span className="text-sm font-medium text-gray-700">Side-by-Side Diff</span>
                        <div className="flex items-center gap-2">
                            <span className="w-3 h-3 bg-red-100 border border-red-300 rounded"></span>
                            <span className="text-xs text-gray-600">Removed</span>
                            <span className="w-3 h-3 bg-green-100 border border-green-300 rounded"></span>
                            <span className="text-xs text-gray-600">Added</span>
                        </div>
                    </div>
                    <div className="flex gap-2">
                        <button
                                onClick={collapseAll}
                                className="text-xs px-2 py-1 bg-white border border-gray-300 rounded hover:bg-gray-50"
                        >
                            Collapse Unchanged
                        </button>
                        <button
                                onClick={expandAll}
                                className="text-xs px-2 py-1 bg-white border border-gray-300 rounded hover:bg-gray-50"
                        >
                            Expand All
                        </button>
                    </div>
                </div>

                {/* Diff Content */}
                <div className="overflow-x-auto">
                    <table className="w-full text-sm font-mono">
                        <thead>
                        <tr className="bg-gray-50 border-b">
                            <th className="w-12 text-right pr-2 py-2 text-gray-500 font-normal border-r">
                                Old
                            </th>
                            <th className="w-12 text-right pr-2 py-2 text-gray-500 font-normal border-r">
                                New
                            </th>
                            <th className="w-1/2 text-left pl-2 py-2 text-gray-700">Before</th>
                            <th className="w-1/2 text-left pl-2 py-2 text-gray-700">After</th>
                        </tr>
                        </thead>
                        <tbody>
                        {sections.map((section, sectionIdx) => {
                            const isCollapsed = collapsedSections.has(sectionIdx);

                            if (isCollapsed && !section.hasChanges) {
                                return (
                                        <tr
                                                key={sectionIdx}
                                                onClick={() => toggleCollapse(sectionIdx)}
                                                className="cursor-pointer hover:bg-gray-50"
                                        >
                                            <td
                                                    colSpan={4}
                                                    className="py-2 px-4 text-center text-gray-500"
                                            >
                                                <div className="flex items-center justify-center gap-2">
                                                    <svg
                                                            className="w-4 h-4"
                                                            fill="none"
                                                            stroke="currentColor"
                                                            viewBox="0 0 24 24"
                                                    >
                                                        <path
                                                                strokeLinecap="round"
                                                                strokeLinejoin="round"
                                                                strokeWidth={2}
                                                                d="M19 9l-7 7-7-7"
                                                        />
                                                    </svg>
                                                    <span>{section.lines.length} unchanged lines</span>
                                                    <span className="text-xs bg-gray-200 px-2 py-0.5 rounded">
                                                    Lines {section.lines[0]?.oldLineNumber}–
                                                        {
                                                            section.lines[section.lines.length - 1]
                                                                    ?.oldLineNumber
                                                        }
                                                </span>
                                                </div>
                                            </td>
                                        </tr>
                                );
                            }

                            return section.lines.map((line, lineIdx) => {
                                if (line.type === 'section-header') {
                                    return (
                                            <tr
                                                    key={`${sectionIdx}-${lineIdx}`}
                                                    className="bg-gray-100"
                                            >
                                                <td
                                                        colSpan={4}
                                                        className="py-2 px-4 font-semibold text-gray-700 border-y"
                                                >
                                                    <div className="flex items-center gap-2">
                                                        {section.hasChanges && (
                                                                <button
                                                                        onClick={(e) => {
                                                                            e.stopPropagation();
                                                                            toggleCollapse(sectionIdx);
                                                                        }}
                                                                        className="text-gray-400 hover:text-gray-600"
                                                                >
                                                                    <svg
                                                                            className="w-4 h-4"
                                                                            fill="none"
                                                                            stroke="currentColor"
                                                                            viewBox="0 0 24 24"
                                                                    >
                                                                        <path
                                                                                strokeLinecap="round"
                                                                                strokeLinejoin="round"
                                                                                strokeWidth={2}
                                                                                d={
                                                                                    isCollapsed
                                                                                            ? 'M19 9l-7 7-7-7'
                                                                                            : 'M9 5l7 7-7 7'
                                                                                }
                                                                        />
                                                                    </svg>
                                                                </button>
                                                        )}
                                                        <span className="text-blue-600">
                                                        {line.path}
                                                    </span>
                                                    </div>
                                                </td>
                                            </tr>
                                    );
                                }

                                return (
                                        <tr
                                                key={`${sectionIdx}-${lineIdx}`}
                                                className={`${
                                                        line.type === 'removed'
                                                                ? 'bg-red-50'
                                                                : line.type === 'added'
                                                                        ? 'bg-green-50'
                                                                        : 'bg-white'
                                                } hover:bg-opacity-75`}
                                        >
                                            <td className="text-right pr-2 py-1 text-gray-400 border-r select-none">
                                                {line.oldLineNumber || ''}
                                            </td>
                                            <td className="text-right pr-2 py-1 text-gray-400 border-r select-none">
                                                {line.newLineNumber || ''}
                                            </td>
                                            <td className="pl-2 py-1 whitespace-pre-wrap break-all border-r">
                                                {line.type === 'removed' && (
                                                        <span className="text-red-700">
                                                    <span className="text-red-400 mr-2">−</span>
                                                            {line.oldContent}
                                                </span>
                                                )}
                                                {line.type === 'unchanged' && (
                                                        <span className="text-gray-700">
                                                    <span className="text-gray-300 mr-2"> </span>
                                                            {line.oldContent}
                                                </span>
                                                )}
                                                {line.type === 'added' && (
                                                        <span className="text-gray-400 italic">
                                                    <span className="mr-2"> </span>
                                                            {line.oldContent}
                                                </span>
                                                )}
                                            </td>
                                            <td className="pl-2 py-1 whitespace-pre-wrap break-all">
                                                {line.type === 'added' && (
                                                        <span className="text-green-700">
                                                    <span className="text-green-400 mr-2">+</span>
                                                            {line.newContent}
                                                </span>
                                                )}
                                                {line.type === 'unchanged' && (
                                                        <span className="text-gray-700">
                                                    <span className="text-gray-300 mr-2"> </span>
                                                            {line.newContent}
                                                </span>
                                                )}
                                                {line.type === 'removed' && (
                                                        <span className="text-gray-400 italic">
                                                    <span className="mr-2"> </span>
                                                            {line.newContent}
                                                </span>
                                                )}
                                            </td>
                                        </tr>
                                );
                            });
                        })}
                        </tbody>
                    </table>
                </div>
            </div>
    );
}

// Simple diff parser - compares lines between old and new spec
function parseDiff(oldSpec: string, newSpec: string): DiffLine[] {
    const oldLines = oldSpec.split('\n');
    const newLines = newSpec.split('\n');
    const result: DiffLine[] = [];

    let currentPath = '';
    let oldIdx = 0;
    let newIdx = 0;
    let oldLineNum = 1;
    let newLineNum = 1;

    while (oldIdx < oldLines.length || newIdx < newLines.length) {
        const oldLine = oldLines[oldIdx];
        const newLine = newLines[newIdx];

        // Detect path/section from JSON structure
        if (oldLine?.includes('"')) {
            const match = oldLine.match(/"([^"]+)":/);
            if (match) {
                currentPath = match[1];
                result.push({
                    type: 'section-header',
                    path: currentPath,
                });
            }
        }

        if (oldLine === newLine) {
            // Unchanged line
            result.push({
                type: 'unchanged',
                oldLineNumber: oldLineNum,
                newLineNumber: newLineNum,
                oldContent: oldLine,
                newContent: newLine,
            });
            oldIdx++;
            newIdx++;
            oldLineNum++;
            newLineNum++;
        } else if (oldLine && !newLines.includes(oldLine)) {
            // Removed line
            result.push({
                type: 'removed',
                oldLineNumber: oldLineNum,
                oldContent: oldLine,
                newContent: '',
            });
            oldIdx++;
            oldLineNum++;
        } else if (newLine && !oldLines.includes(newLine)) {
            // Added line
            result.push({
                type: 'added',
                newLineNumber: newLineNum,
                oldContent: '',
                newContent: newLine,
            });
            newIdx++;
            newLineNum++;
        } else {
            // Line modified - show as remove + add
            if (oldLine) {
                result.push({
                    type: 'removed',
                    oldLineNumber: oldLineNum,
                    oldContent: oldLine,
                    newContent: '',
                });
                oldIdx++;
                oldLineNum++;
            }
            if (newLine) {
                result.push({
                    type: 'added',
                    newLineNumber: newLineNum,
                    oldContent: '',
                    newContent: newLine,
                });
                newIdx++;
                newLineNum++;
            }
        }
    }

    return result;
}
