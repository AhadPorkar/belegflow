import { defineConfig } from '@playwright/test';
export default defineConfig({
 testDir: './tests/e2e', fullyParallel: false, workers: 1, retries: 0,
 reporter: [['list'], ['html', {open:'never'}], ['junit',{outputFile:'test-results/e2e.xml'}]],
 use: {baseURL: process.env.BF_BASE_URL || 'http://localhost:8080', browserName:'chromium', locale:'de-DE', timezoneId:'Europe/Berlin', trace:'retain-on-failure', screenshot:'only-on-failure'},
});
