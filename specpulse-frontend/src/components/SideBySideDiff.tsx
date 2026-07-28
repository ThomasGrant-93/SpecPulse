import { useMemo, useState } from 'react';
import type { ReactNode } from 'react';

import { formatJsonForDiff } from '@/utils/formatJsonForDiff';

interface SideBySideDiffProps {
    oldSpec: string;
    newSpec: string;
}

type DiffRowType = 'unchanged' | 'added' | 'removed' | 'modified';

interface InlineRange {
    oldMidStart: number;
    oldMidEndExclusive: number;
    newMidStart: number;
    newMidEndExclusive: number;
}

interface DiffRow {
    type: DiffRowType;
    oldLineNumber?: number;
    newLineNumber?: number;
    oldContent?: string;
    newContent?: string;

    // JSON folding state
    foldSectionPath: number[];
    isSectionHeader: boolean;
    sectionId?: number;
    sectionLabel?: string;

    // Inline diff for modified lines
    inlineRange?: InlineRange;
    renamed?: { from: string; to: string };
}

const syntaxHighlightCache = new Map<string, ReactNode>();

function countIndentSpaces(line: string): number {
    const m = line.match(/^\s*/);
    return m ? m[0].length : 0;
}

function extractKeyAtLineStart(line: string): string | null {
    const m = line.match(/^\s*"([^"]+)"\s*:/);
    return m ? m[1] : null;
}

function tryGetFoldHeader(line: string): { label: string } | null {
    // Matches nested object/array under a key, e.g. "paths": { ... } / "tags": [ ...
    const m = line.match(/^\s*"([^"]+)"\s*:\s*(\{|\[)/);
    return m ? { label: m[1] } : null;
}

function computeInlineRange(oldLine: string, newLine: string): InlineRange {
    const oldLen = oldLine.length;
    const newLen = newLine.length;
    const minLen = Math.min(oldLen, newLen);

    let prefix = 0;
    while (prefix < minLen && oldLine[prefix] === newLine[prefix]) {
        prefix++;
    }

    let suffix = 0;
    while (suffix < minLen - prefix && oldLine[oldLen - 1 - suffix] === newLine[newLen - 1 - suffix]) {
        suffix++;
    }

    return {
        oldMidStart: prefix,
        oldMidEndExclusive: oldLen - suffix,
        newMidStart: prefix,
        newMidEndExclusive: newLen - suffix,
    };
}

function syntaxHighlightJsonSegment(segment: string): ReactNode {
    if (!segment) return null;
    const cached = syntaxHighlightCache.get(segment);
    if (cached) return cached;

    const tokenRegex = /"(?:\\.|[^"\\])*"|-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?\b|true|false|null|[{}\[\]:,]|\s+/g;

    const parts: ReactNode[] = [];
    let lastIndex = 0;

    const matches = segment.matchAll(tokenRegex);
    for (const match of matches) {
        const token = match[0];
        const index = match.index ?? 0;

        if (index > lastIndex) {
            const rest = segment.slice(lastIndex, index);
            parts.push(<span key={`r-${lastIndex}`}>{rest}</span>);
        }

        let className = 'text-gray-700';
        if (token.startsWith('"')) {
            const after = segment.slice(index + token.length);
            const isKey = /^\s*:/.test(after);
            className = isKey ? 'text-blue-700 font-medium' : 'text-purple-700';
        } else if (token === 'true' || token === 'false') {
            className = 'text-teal-700 font-semibold';
        } else if (token === 'null') {
            className = 'text-teal-700';
        } else if (token.trim() !== '' && !Number.isNaN(Number(token)) && /^-?\d/.test(token)) {
            className = 'text-orange-600';
        } else if ('{}[]:,'.includes(token.trim())) {
            className = 'text-gray-500';
        }

        parts.push(<span key={index} className={className}>{token}</span>);
        lastIndex = index + token.length;
    }

    if (lastIndex < segment.length) {
        parts.push(<span key={`tail-${lastIndex}`}>{segment.slice(lastIndex)}</span>);
    }

    const node = <>{parts}</>;
    syntaxHighlightCache.set(segment, node);

    // Prevent unbounded growth when users compare many specs in a single session.
    if (syntaxHighlightCache.size > 5000) {
        syntaxHighlightCache.clear();
    }
    return node;
}

function renderJsonLineWithInlineDiff(content: string, inlineRange: InlineRange, side: 'old' | 'new'): ReactNode {
    const midStart = side === 'old' ? inlineRange.oldMidStart : inlineRange.newMidStart;
    const midEndExclusive = side === 'old' ? inlineRange.oldMidEndExclusive : inlineRange.newMidEndExclusive;

    const prefix = content.slice(0, midStart);
    const mid = content.slice(midStart, midEndExclusive);
    const suffix = content.slice(midEndExclusive);

    const midClass = side === 'old' ? 'bg-red-100 text-red-800' : 'bg-green-100 text-green-800';

    return (
        <>
            {syntaxHighlightJsonSegment(prefix)}
            {mid ? (
                <span className={`inline-block px-px ${midClass}`}>{syntaxHighlightJsonSegment(mid)}</span>
            ) : null}
            {syntaxHighlightJsonSegment(suffix)}
        </>
    );
}

function parseDiffRows(oldSpec: string, newSpec: string): DiffRow[] {
    const oldLines = oldSpec.split('\n');
    const newLines = newSpec.split('\n');

    // Remaining-line frequency checks to decide add/remove vs modification.
    // This is still heuristic (not a full sequence diff), but it preserves multiplicity better
    // than Set-based membership and reduces misclassification when lines repeat.
    const oldCounts = new Map<string, number>();
    const newCounts = new Map<string, number>();
    for (const line of oldLines) {
        oldCounts.set(line, (oldCounts.get(line) ?? 0) + 1);
    }
    for (const line of newLines) {
        newCounts.set(line, (newCounts.get(line) ?? 0) + 1);
    }

    const dec = (m: Map<string, number>, k: string) => {
        const next = (m.get(k) ?? 0) - 1;
        if (next <= 0) m.delete(k);
        else m.set(k, next);
    };

    type PreRow = {
        type: 'unchanged' | 'added' | 'removed';
        oldLineNumber?: number;
        newLineNumber?: number;
        oldContent?: string;
        newContent?: string;
    };

    const prelim: PreRow[] = [];
    let oldIdx = 0;
    let newIdx = 0;
    let oldLineNum = 1;
    let newLineNum = 1;

    while (oldIdx < oldLines.length || newIdx < newLines.length) {
        const oldLine = oldIdx < oldLines.length ? oldLines[oldIdx] : undefined;
        const newLine = newIdx < newLines.length ? newLines[newIdx] : undefined;

        if (oldLine !== undefined && newLine !== undefined && oldLine === newLine) {
            prelim.push({
                type: 'unchanged',
                oldLineNumber: oldLineNum,
                newLineNumber: newLineNum,
                oldContent: oldLine,
                newContent: newLine,
            });

            dec(oldCounts, oldLine);
            dec(newCounts, newLine);
            oldIdx++;
            newIdx++;
            oldLineNum++;
            newLineNum++;
            continue;
        }

        if (oldLine !== undefined && (newLine === undefined || (newCounts.get(oldLine) ?? 0) === 0)) {
            prelim.push({
                type: 'removed',
                oldLineNumber: oldLineNum,
                oldContent: oldLine,
                newContent: '',
            });

            dec(oldCounts, oldLine);
            oldIdx++;
            oldLineNum++;
            continue;
        }

        if (newLine !== undefined && (oldLine === undefined || (oldCounts.get(newLine) ?? 0) === 0)) {
            prelim.push({
                type: 'added',
                newLineNumber: newLineNum,
                oldContent: '',
                newContent: newLine,
            });

            dec(newCounts, newLine);
            newIdx++;
            newLineNum++;
            continue;
        }

        // Modified line: emit remove + add.
        if (oldLine !== undefined) {
            prelim.push({
                type: 'removed',
                oldLineNumber: oldLineNum,
                oldContent: oldLine,
                newContent: '',
            });

            dec(oldCounts, oldLine);
            oldIdx++;
            oldLineNum++;
        }

        if (newLine !== undefined) {
            prelim.push({
                type: 'added',
                newLineNumber: newLineNum,
                oldContent: '',
                newContent: newLine,
            });

            dec(newCounts, newLine);
            newIdx++;
            newLineNum++;
        }
    }

    const paired: DiffRow[] = [];
    for (let i = 0; i < prelim.length; i++) {
        const a = prelim[i];
        const b = prelim[i + 1];

        if (a.type === 'removed' && b?.type === 'added') {
            const oldContent = a.oldContent ?? '';
            const newContent = b.newContent ?? '';
            const renamedFrom = extractKeyAtLineStart(oldContent);
            const renamedTo = extractKeyAtLineStart(newContent);

            paired.push({
                type: 'modified',
                oldLineNumber: a.oldLineNumber,
                newLineNumber: b.newLineNumber,
                oldContent,
                newContent,
                foldSectionPath: [],
                isSectionHeader: false,
                inlineRange: computeInlineRange(oldContent, newContent),
                renamed:
                    renamedFrom && renamedTo && renamedFrom !== renamedTo
                        ? { from: renamedFrom, to: renamedTo }
                        : undefined,
            });
            i++;
        } else if (a.type === 'unchanged') {
            paired.push({
                type: 'unchanged',
                oldLineNumber: a.oldLineNumber,
                newLineNumber: a.newLineNumber,
                oldContent: a.oldContent,
                newContent: a.newContent,
                foldSectionPath: [],
                isSectionHeader: false,
            });
        } else if (a.type === 'removed') {
            paired.push({
                type: 'removed',
                oldLineNumber: a.oldLineNumber,
                newLineNumber: undefined,
                oldContent: a.oldContent,
                newContent: undefined,
                foldSectionPath: [],
                isSectionHeader: false,
            });
        } else {
            paired.push({
                type: 'added',
                oldLineNumber: undefined,
                newLineNumber: a.newLineNumber,
                oldContent: undefined,
                newContent: a.newContent,
                foldSectionPath: [],
                isSectionHeader: false,
            });
        }
    }

    // Apply indentation-based folding.
    let nextSectionId = 1;
    const stack: { id: number; indent: number; label: string }[] = [];

    for (const row of paired) {
        const chosenLine =
            row.type === 'added'
                ? row.newContent
                : row.type === 'removed'
                  ? row.oldContent
                  : row.newContent ?? row.oldContent;

        const line = chosenLine ?? '';
        const indent = countIndentSpaces(line);

        while (stack.length > 0 && indent <= stack[stack.length - 1]!.indent) {
            stack.pop();
        }

        const fold = tryGetFoldHeader(line);
        if (fold) {
            const sectionId = nextSectionId++;
            stack.push({ id: sectionId, indent, label: fold.label });
            row.isSectionHeader = true;
            row.sectionId = sectionId;
            row.sectionLabel = fold.label;
        }

        row.foldSectionPath = stack.map((s) => s.id);
    }

    return paired;
}

function rowIsChanged(row: DiffRow): boolean {
    return row.type === 'added' || row.type === 'removed' || row.type === 'modified';
}

export default function SideBySideDiff({ oldSpec, newSpec }: SideBySideDiffProps) {
    const [showUnchangedRoot, setShowUnchangedRoot] = useState(false);
    const [expandedSections, setExpandedSections] = useState<Set<number>>(new Set());
    const [collapsedByUser, setCollapsedByUser] = useState<Set<number>>(new Set());

    const formattedOldSpec = useMemo(() => formatJsonForDiff(oldSpec), [oldSpec]);
    const formattedNewSpec = useMemo(() => formatJsonForDiff(newSpec), [newSpec]);

    const diffRows = useMemo(() => parseDiffRows(formattedOldSpec, formattedNewSpec), [formattedOldSpec, formattedNewSpec]);

    const sectionMeta = useMemo(() => {
        const sectionIds = new Set<number>();
        for (const row of diffRows) {
            if (row.isSectionHeader && row.sectionId !== undefined) {
                sectionIds.add(row.sectionId);
            }
        }

        const hasChanges = new Map<number, boolean>();
        for (const id of sectionIds) hasChanges.set(id, false);

        for (const row of diffRows) {
            if (!rowIsChanged(row)) continue;
            for (const id of row.foldSectionPath) {
                hasChanges.set(id, true);
            }
        }

        return { sectionIds: Array.from(sectionIds.values()), hasChanges };
    }, [diffRows]);

    const defaultCollapsed = useMemo(() => {
        const next = new Set<number>();
        for (const id of sectionMeta.sectionIds) {
            if (!sectionMeta.hasChanges.get(id)) next.add(id);
        }
        return next;
    }, [sectionMeta]);

    const isSectionCollapsed = (sectionId: number): boolean => {
        const collapsedByDefault = defaultCollapsed.has(sectionId);
        const collapsedByUserLocal = collapsedByUser.has(sectionId);
        const expandedByUser = expandedSections.has(sectionId);
        return (collapsedByDefault || collapsedByUserLocal) && !expandedByUser;
    };

    const toggleSection = (sectionId: number) => {
        const currentlyCollapsed = isSectionCollapsed(sectionId);
        if (currentlyCollapsed) {
            // Expand the section.
            setCollapsedByUser((prev) => {
                const next = new Set(prev);
                next.delete(sectionId);
                return next;
            });
            setExpandedSections((prev) => {
                const next = new Set(prev);
                next.add(sectionId);
                return next;
            });
        } else {
            // Collapse the section.
            if (defaultCollapsed.has(sectionId)) {
                setExpandedSections((prev) => {
                    const next = new Set(prev);
                    next.delete(sectionId);
                    return next;
                });
            } else {
                setCollapsedByUser((prev) => {
                    const next = new Set(prev);
                    next.add(sectionId);
                    return next;
                });
            }
        }
    };

    const collapseAllUnchanged = () => {
        setExpandedSections(new Set());
        setCollapsedByUser(new Set());
        setShowUnchangedRoot(false);
    };

    const expandAll = () => {
        setCollapsedByUser(new Set());
        setExpandedSections(new Set(sectionMeta.sectionIds));
        setShowUnchangedRoot(true);
    };

    const isVisible = (row: DiffRow): boolean => {
        if (row.foldSectionPath.length === 0) {
            return rowIsChanged(row) || showUnchangedRoot;
        }
        const path = row.foldSectionPath;

        if (row.isSectionHeader && row.sectionId !== undefined) {
            // Keep the header visible even when it is collapsed.
            const ancestors = path.slice(0, -1);
            return !ancestors.some((id) => isSectionCollapsed(id));
        }

        return !path.some((id) => isSectionCollapsed(id));
    };

    const hasVisibleRows = diffRows.some(isVisible);

    return (
        <div className="border rounded-lg overflow-hidden">
            <div className="bg-gray-50 border-b px-4 py-2 flex items-center justify-between">
                <div className="flex items-center gap-4">
                    <span className="text-sm font-medium text-gray-700">Side-by-Side Diff</span>
                    <div className="flex items-center gap-2">
                        <span className="w-3 h-3 bg-red-100 border border-red-300 rounded" />
                        <span className="text-xs text-gray-600">Removed</span>
                        <span className="w-3 h-3 bg-green-100 border border-green-300 rounded" />
                        <span className="text-xs text-gray-600">Added</span>
                    </div>
                </div>
                <div className="flex gap-2">
                    <button
                        onClick={collapseAllUnchanged}
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

            <div className="overflow-x-auto">
                <table className="w-full text-sm font-mono">
                    <thead>
                        <tr className="bg-gray-50 border-b">
                            <th className="w-12 text-right pr-2 py-2 text-gray-500 font-normal border-r">Old</th>
                            <th className="w-12 text-right pr-2 py-2 text-gray-500 font-normal border-r">New</th>
                            <th className="w-1/2 text-left pl-2 py-2 text-gray-700">Before</th>
                            <th className="w-1/2 text-left pl-2 py-2 text-gray-700">After</th>
                        </tr>
                    </thead>
                    <tbody>
                        {!hasVisibleRows ? (
                            <tr>
                                <td colSpan={4} className="py-6 text-center text-sm text-gray-500">
                                    No changes detected
                                </td>
                            </tr>
                        ) : null}

                        {diffRows.map((row, idx) => {
                            if (!isVisible(row)) return null;

                            const bgClass =
                                row.type === 'removed'
                                    ? 'bg-red-50'
                                    : row.type === 'added'
                                      ? 'bg-green-50'
                                      : row.type === 'modified'
                                        ? 'bg-amber-50'
                                        : 'bg-white';

                            const isCollapsed = row.isSectionHeader && row.sectionId !== undefined && isSectionCollapsed(row.sectionId);

                            const beforeCell = (() => {
                                if (row.type === 'added') return null;
                                const content = row.oldContent ?? '';
                                const title = content;

                                if (row.type === 'unchanged') {
                                    return (
                                        <span title={title} className="text-gray-700">
                                            {syntaxHighlightJsonSegment(content)}
                                        </span>
                                    );
                                }

                                if (row.type === 'removed') {
                                    return (
                                        <span title={title} className="text-red-700">
                                            <span className="text-red-400 mr-2">−</span>
                                            {syntaxHighlightJsonSegment(content)}
                                        </span>
                                    );
                                }

                                if (row.type === 'modified') {
                                    return (
                                        <span title={title} className="text-gray-700">
                                            <span className="text-red-400 mr-2">−</span>
                                            {row.inlineRange ? renderJsonLineWithInlineDiff(content, row.inlineRange, 'old') : syntaxHighlightJsonSegment(content)}
                                        </span>
                                    );
                                }

                                return null;
                            })();

                            const afterCell = (() => {
                                if (row.type === 'removed') return null;
                                const content = row.newContent ?? '';
                                const title = content;

                                if (row.type === 'unchanged') {
                                    return (
                                        <span title={title} className="text-gray-700">
                                            {syntaxHighlightJsonSegment(content)}
                                        </span>
                                    );
                                }

                                if (row.type === 'added') {
                                    return (
                                        <span title={title} className="text-green-700">
                                            <span className="text-green-400 mr-2">+</span>
                                            {syntaxHighlightJsonSegment(content)}
                                        </span>
                                    );
                                }

                                if (row.type === 'modified') {
                                    return (
                                        <span title={title} className="text-gray-700">
                                            <span className="text-green-400 mr-2">+</span>
                                            {row.inlineRange ? renderJsonLineWithInlineDiff(content, row.inlineRange, 'new') : syntaxHighlightJsonSegment(content)}
                                        </span>
                                    );
                                }

                                return null;
                            })();

                            const renameBadge =
                                row.renamed && row.type === 'modified' ? (
                                    <span
                                        className="ml-2 inline-flex items-center rounded bg-amber-100 text-amber-800 px-2 py-0.5 text-[11px]"
                                        title={`Renamed key: ${row.renamed.from} → ${row.renamed.to}`}
                                    >
                                        ↔ Renamed
                                    </span>
                                ) : null;

                            return (
                                <tr key={idx} className={`${bgClass} hover:bg-opacity-75`}>
                                    <td className="text-right pr-2 py-1 text-gray-400 border-r select-none whitespace-nowrap">
                                        {row.isSectionHeader && row.sectionId !== undefined ? (
                                            <button
                                                type="button"
                                                onClick={() => toggleSection(row.sectionId!)}
                                                className="inline-flex items-center justify-center mr-2 text-gray-400 hover:text-gray-600"
                                                aria-label={isCollapsed ? 'Expand section' : 'Collapse section'}
                                                title={isCollapsed ? 'Expand' : 'Collapse'}
                                            >
                                                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                    <path
                                                        strokeLinecap="round"
                                                        strokeLinejoin="round"
                                                        strokeWidth={2}
                                                        d={isCollapsed ? 'M19 9l-7 7-7-7' : 'M9 5l7 7-7 7'}
                                                    />
                                                </svg>
                                            </button>
                                        ) : null}
                                        {row.oldLineNumber ?? ''}
                                    </td>
                                    <td className="text-right pr-2 py-1 text-gray-400 border-r select-none whitespace-nowrap">
                                        {row.newLineNumber ?? ''}
                                    </td>
                                    <td className="pl-2 py-1 whitespace-pre-wrap break-all border-r">
                                        {row.isSectionHeader && row.sectionLabel ? (
                                            <span className="text-blue-600 font-medium mr-2" title={row.sectionLabel}>
                                                {row.sectionLabel}
                                            </span>
                                        ) : null}
                                        {beforeCell}
                                        {renameBadge}
                                    </td>
                                    <td className="pl-2 py-1 whitespace-pre-wrap break-all">
                                        {afterCell}
                                    </td>
                                </tr>
                            );
                        })}
                    </tbody>
                </table>
            </div>
        </div>
    );
}
