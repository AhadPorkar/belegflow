import React from 'react';
import { vi } from 'vitest';
import { render } from '@testing-library/react';
import { WorkspaceStore } from '../../src/store.ts';
import { translate } from '../../src/i18n.ts';
import { FixtureApi, me, catalog, detail, summary } from '../support/fixtures.mjs';

/** Each React test receives independent synthetic data and action spies, not a fake DOM renderer. */
export function props(overrides = {}) {
  const core = new WorkspaceStore(new FixtureApi());
  const state = {...core.getSnapshot(),ready:true,me:structuredClone(me),catalog:structuredClone(catalog),
    summary:structuredClone(summary),invoices:[structuredClone(detail)],total:1,
    detail:structuredClone(detail),selectedId:detail.id,...overrides};
  core.dispose();
  const store={t:(key,vars)=>translate(state.locale,key,vars)};
  for(const method of ['navigate','patch','openDialog','closeDialog','login','loginDemo','logout','setLocale',
    'setNumberLocale','toggleTheme','search','filter','page','selectInvoice','openInvoice','refresh','command',
    'comment','submitDialog','setDialogFile','saveRules','revoke','runReminders','markRead','earlierAudit',
    'download','exportApproved']) store[method]=vi.fn();
  store.comment.mockResolvedValue(true);
  return {state,store};
}
export function show(Component, overrides={}) { const p=props(overrides); return {...render(<Component {...p}/>),...p}; }
export function input(container,name) { const el=container.querySelector(`[name="${name}"]`); if(!el)throw Error(`Missing input ${name}`);return el; }
