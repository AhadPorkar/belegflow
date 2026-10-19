import { expect } from '@playwright/test';

export const demoPassword = 'BelegFlow-Demo-2026!';

/** Submit only after the previous logout has completed; never reload a pending logout. */
export async function loginOnScreen(page, email = 'finance@belegflow.demo', password = demoPassword) {
  const form = page.locator('.login-box');
  await expect(form).toBeVisible();
  await expect(form.locator('button[type=submit]')).toBeEnabled();
  await form.locator('input[name=email]').fill(email);
  await form.locator('input[name=password]').fill(password);
  await form.locator('button[type=submit]').click();
  await expect(page.locator('.workspace-grid')).toBeVisible();
  await expect.poll(async () => (await api(page, '/session')).data?.email).toBe(email);
  await page.getByRole('button', { name: 'EN', exact: true }).click();
  await expect(page.locator('html')).toHaveAttribute('lang', 'en');
}

export async function login(page, email = 'finance@belegflow.demo', password = demoPassword) {
  await page.goto('/');
  await loginOnScreen(page, email, password);
}

export async function signOut(page, label = 'Sign out') {
  const completed = page.waitForResponse(response =>
    new URL(response.url()).pathname === '/api/logout' && response.request().method() === 'POST');
  // Ordinary, actionable click: no force, dispatchEvent, or test-only sign-out endpoint.
  await page.getByRole('button', { name: label, exact: true }).click();
  const response = await completed;
  expect(response.status()).toBeGreaterThanOrEqual(200);
  expect(response.status()).toBeLessThan(300);
  await expect(page.locator('.login-box button[type=submit]')).toBeEnabled();
  expect((await api(page, '/session')).status).toBe(401);
}

export async function api(page, path, method = 'GET', body) {
  return page.evaluate(async ({ path, method, body }) => {
    const csrf = await fetch('/api/csrf').then(response => response.json());
    const response = await fetch('/api' + path, {
      method,
      headers: { 'Content-Type': 'application/json', 'X-CSRF-TOKEN': csrf.token },
      body: body === undefined ? undefined : JSON.stringify(body),
    });
    return { status: response.status, data: await response.json().catch(() => null) };
  }, { path, method, body });
}
