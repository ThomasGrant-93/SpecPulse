import { render, screen, fireEvent } from '@testing-library/react';

import SideBySideDiff from './SideBySideDiff';

function generateLargeFilteredSpecs(targetBytes: number) {
    const base = {
        paths: {
            '/big': {
                get: {
                    parameters: [] as Array<{
                        n: string;
                        r: boolean;
                        s: { t: string };
                    }>,
                },
            },
        },
    };

    const sampleParam = {
        n: 'param0',
        r: false,
        s: { t: 'string' },
    };

    const baseLen = JSON.stringify(base, null, 2).length;
    const sampleParamLen = JSON.stringify(sampleParam, null, 2).length;

    // Rough estimate: each additional param contributes roughly its own pretty-printed size plus some punctuation.
    let count = Math.max(1, Math.floor((targetBytes - baseLen) / (sampleParamLen + 4)));
    count = Math.min(count, 4000);

    const mkParam = (i: number) => ({
        n: `param${i}`,
        r: i % 2 === 0,
        s: { t: 'string' },
    });

    const mkNewParam = (i: number) => {
        if (i === Math.floor(count / 2)) {
            return {
                n: `param${i}`,
                r: i % 2 === 0,
                s: { t: 'integer' },
            };
        }
        return mkParam(i);
    };

    const oldObj = JSON.parse(JSON.stringify(base)) as typeof base;
    const newObj = JSON.parse(JSON.stringify(base)) as typeof base;
    oldObj.paths['/big'].get.parameters = [];
    newObj.paths['/big'].get.parameters = [];
    for (let i = 0; i < count; i++) {
        oldObj.paths['/big'].get.parameters.push(mkParam(i));
        newObj.paths['/big'].get.parameters.push(mkNewParam(i));
    }

    let oldSpec = JSON.stringify(oldObj, null, 2);
    let newSpec = JSON.stringify(newObj, null, 2);

    // Ensure we cross the target; adjust a few times to avoid huge over-allocation.
    let guard = 0;
    while (oldSpec.length < targetBytes && guard < 3) {
        guard++;
        const i = count;
        oldObj.paths['/big'].get.parameters.push(mkParam(i));
        newObj.paths['/big'].get.parameters.push(mkNewParam(i));
        count++;
        oldSpec = JSON.stringify(oldObj, null, 2);
        newSpec = JSON.stringify(newObj, null, 2);
    }

    return { oldSpec, newSpec, oldBytes: oldSpec.length, newBytes: newSpec.length };
}

describe('SideBySideDiff large spec smoke', () => {
    it(
        'renders large delta-first specs without crashing',
        () => {
        // jsdom + React rendering is memory heavy; keep the spec large but avoid heap OOM.
        // This still exercises the same code paths (large JSON parse + diff + folding).
        const targetBytes = 350 * 1024;
        const { oldSpec, newSpec, oldBytes } = generateLargeFilteredSpecs(targetBytes);

        // Sanity check: ensure the test is actually exercising the large-spec path.
        expect(oldBytes).toBeGreaterThanOrEqual(targetBytes);

        const start = Date.now();
        render(<SideBySideDiff oldSpec={oldSpec} newSpec={newSpec} />);
        const durationMs = Date.now() - start;

        // This is a smoke test: don’t be too strict to avoid CI flakiness.
        expect(durationMs).toBeLessThan(25000);
        },
        30000
    );
});
