import { defineConfig } from '@playwright/test';
export default defineConfig({
 testDir: './tests/e2e', fullyParallel: false, workers: 1, retries: 0,
 reporter: [['list'], ['html', {open:'never'}]],
 use: {baseURL: process.env.BF_BASE_URL || 'http://localhost:8080', browserName:'chromium', trace:'retain-on-failure', screenshot:'only-on-failure'},
});
