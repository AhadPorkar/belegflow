import { afterEach } from 'vitest';
import { cleanup } from '@testing-library/react';

afterEach(() => { cleanup(); localStorage.clear(); document.documentElement.removeAttribute('data-theme'); });
// jsdom has no top-layer rendering; dialog behavior is independently checked in real Chromium.
{
  HTMLDialogElement.prototype.showModal = function () { this.open = true; };
}
{
  HTMLDialogElement.prototype.close = function () { this.open = false; };
}
if (!globalThis.matchMedia) {
  globalThis.matchMedia = () => ({ matches: false, addEventListener() {}, removeEventListener() {} });
}
