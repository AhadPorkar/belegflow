import React from 'react';
import { describe,it,expect } from 'vitest';
import { screen,fireEvent } from '@testing-library/react';
import { Exports } from '../../src/features/accounting/Exports.jsx';
import { AuditPage } from '../../src/features/audit/AuditPage.jsx';
import { detail,summary } from '../support/fixtures.mjs';
import { show } from './helpers.jsx';

describe('Accounting handoff and audit',()=>{
 it.each(['en','de'])('explains the export boundary and empty state (%s)',locale=>{const {store}=show(Exports,{locale});expect(screen.getByText(store.t('exportBoundary'))).toBeTruthy();expect(screen.getByText(store.t('noExports'))).toBeTruthy();expect(screen.getByRole('button',{name:store.t('exportAll')}).disabled).toBe(true);});
 it('opens bulk export when approved invoices exist',()=>{const {store}=show(Exports,{summary:{...summary,approved:2}});fireEvent.click(screen.getByRole('button',{name:store.t('exportAll')}));expect(store.exportApproved).toHaveBeenCalledOnce();});
 it('disables bulk export during an in-flight command',()=>{const {store}=show(Exports,{busy:true,summary:{...summary,approved:2}});expect(screen.getByRole('button',{name:store.t('exportAll')}).disabled).toBe(true);});
 it('downloads a stored package using its immutable id',()=>{const e={id:'export-a',filename:'handoff.zip',sha256:'f'.repeat(64),invoice_count:2,created_by_name:'Nora',created_at:'2026-10-18T12:00:00Z'};const {store}=show(Exports,{exports:[e]});fireEvent.click(screen.getByRole('button',{name:store.t('download')}));expect(store.download).toHaveBeenCalledWith('/exports/export-a/download','handoff.zip');expect(screen.getByText(/SHA-256/).textContent).toContain('f'.repeat(18));});
 it.each(['en','de'])('renders audit history and links to the source invoice (%s)',locale=>{const {store}=show(AuditPage,{locale,audit:detail.audit,auditNext:3});fireEvent.click(screen.getByRole('button',{name:/TEST-2026-001/}));expect(store.openInvoice).toHaveBeenCalledWith('invoice-a');fireEvent.click(screen.getByRole('button',{name:store.t('loadEarlier')}));expect(store.earlierAudit).toHaveBeenCalledOnce();});
 it('does not offer a pagination request without a cursor',()=>{const {store}=show(AuditPage,{audit:[],auditNext:0});expect(screen.queryByRole('button',{name:store.t('loadEarlier')})).toBeNull();});
 it('renders non-invoice audit events without a fake link',()=>{const {container}=show(AuditPage,{audit:[{...detail.audit[0],invoice_id:null,invoice_number:null,detail:'Rule updated'}]});expect(screen.getByText('Rule updated')).toBeTruthy();expect(container.querySelector('tbody button')).toBeNull();});
});
