import { test, expect } from '@playwright/test';
import { login, loginOnScreen, signOut, api } from './helpers.js';

const viewports = [
  { name: 'desktop', width: 1440, height: 900 },
  { name: 'short desktop', width: 1280, height: 560 },
  { name: 'tablet rail', width: 1024, height: 768 },
  { name: 'mobile portrait', width: 390, height: 844 },
  { name: 'mobile landscape', width: 844, height: 390 },
];
for (const viewport of viewports) for (const locale of ['en', 'de']) {
  test(`account controls: ${viewport.name}, ${locale}, ordinary sign-out and fresh login`, async ({ page }) => {
    await page.setViewportSize({ width: viewport.width, height: viewport.height });
    await login(page, 'admin@belegflow.demo');
    await page.keyboard.press('Control+k');
    await page.getByRole('dialog').getByRole('button', { name: 'Workspace settings', exact: true }).click();
    await page.getByRole('button', { name: locale.toUpperCase(), exact: true }).click();
    await expect(page.locator('html')).toHaveAttribute('lang', locale);
    const label = locale === 'en' ? 'Number and date formatting' : 'Zahlen- und Datumsformat';
    const formatting = page.getByLabel(label, { exact: true });
    await expect(formatting).toHaveAccessibleName(label);
    await formatting.selectOption('de-DE');
    await expect(formatting).toHaveValue('de-DE');
    await page.keyboard.press('Tab'); // The next focus target must remain usable after a select.
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
    await signOut(page, locale === 'en' ? 'Sign out' : 'Abmelden');
    await loginOnScreen(page, 'viewer@belegflow.demo');
    expect((await api(page, '/session')).data.role).toBe('VIEWER');
    await expect(page.getByRole('button', { name: 'Import invoice', exact: true })).toHaveCount(0);
  });
}

test('session handoff: delayed logout disables login until the real server response', async ({ page }) => {
  await login(page, 'admin@belegflow.demo');
  let intercepted, release;
  const arrived = new Promise(resolve => { intercepted = resolve; });
  const gate = new Promise(resolve => { release = resolve; });
  await page.route('**/api/logout', async route => {
    intercepted();
    await gate;
    await route.continue(); // Still calls the real server; does not fake a successful logout.
  });
  const finished = page.waitForResponse(response => new URL(response.url()).pathname === '/api/logout');
  try {
    await page.getByRole('button', { name: 'Sign out', exact: true }).click();
    await arrived;
    await expect(page.locator('.app-shell')).toHaveCount(0);
    await expect(page.locator('.login-box form')).toHaveAttribute('aria-busy', 'true');
    await expect(page.locator('.login-box button[type=submit]')).toBeDisabled();
  } finally {
    release();
  }
  expect((await finished).ok()).toBe(true);
  await expect(page.locator('.login-box button[type=submit]')).toBeEnabled();
  expect((await api(page, '/session')).status).toBe(401);
  await loginOnScreen(page, 'viewer@belegflow.demo');
  expect((await api(page, '/session')).data.role).toBe('VIEWER');
});
