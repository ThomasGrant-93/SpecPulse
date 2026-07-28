export function formatJsonForDiff(content: string): string {
    const trimmed = content.trim();
    if (!trimmed) {
        return content;
    }

    try {
        const parsed: unknown = JSON.parse(trimmed);

        // Backward compatibility: sometimes JSON may come as a quoted JSON string.
        if (typeof parsed === 'string') {
            const inner = parsed.trim();
            if (inner.startsWith('{') || inner.startsWith('[')) {
                const reparsed: unknown = JSON.parse(inner);
                return JSON.stringify(reparsed, null, 2);
            }
            return content;
        }

        return JSON.stringify(parsed, null, 2);
    } catch {
        // Not valid JSON: keep as-is to avoid rendering/crashing.
        return content;
    }
}
