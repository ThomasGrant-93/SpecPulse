const typescriptParser = require('@typescript-eslint/parser');

module.exports = [
    {
        files: ['**/*.{ts,tsx}'],
        languageOptions: {
            ecmaVersion: 2020,
            sourceType: 'module',
            parser: typescriptParser,
            globals: {
                browser: true,
                es2020: true,
            },
        },
        ignores: ['dist', 'eslint.config.cjs', 'coverage'],
        rules: {
            // General code quality
            'no-console': ['warn', {allow: ['warn', 'error']}],
            'eqeqeq': ['error', 'always'],
            'curly': ['warn', 'multi-line'],
            'no-var': 'error',
            'prefer-const': 'warn',
        },
    },
];
