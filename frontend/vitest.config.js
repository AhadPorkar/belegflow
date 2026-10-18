import { defineConfig } from 'vitest/config';

// Shared core tests run in Node; React component tests use a real jsdom + React DOM renderer.
// Coverage includes executable production files even when a test never imports them.
export default defineConfig({
  test: {
    projects: [
      { test: { name: 'core', environment: 'node', include: ['tests/unit/**/*.test.mjs'] } },
      { test: { name: 'components', environment: 'jsdom', include: ['tests/components/**/*.test.jsx'],
          setupFiles: ['tests/components/setup.js'], clearMocks: true, restoreMocks: true } },
    ],
    reporters: ['default', 'junit'],
    outputFile: { junit: 'test-results/unit-and-components.xml' },
    coverage: {
      provider: 'v8', include: ['src/**/*.{ts,jsx}'],
      // Type declarations do not execute. The browser entry point is checked by real-app Playwright.
      exclude: ['src/models.ts', 'src/main.jsx'],
      reporter: ['text', 'html', 'json-summary', 'json', 'lcov'], reportsDirectory: 'coverage',
      reportOnFailure: true,
      ...(process.env.BF_STRICT_COVERAGE === 'true'
        ? { thresholds: { lines: 80, functions: 80, branches: 70, statements: 80 } } : {}),
    },
  },
});
