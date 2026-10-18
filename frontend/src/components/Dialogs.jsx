import React from 'react';
import { money, date, time, initials, localDateTime } from '../format.ts';
import { Icon, Logo, Button, IconButton, Badge, Avatar, Empty, Field, finance, PageHeading } from './ui.jsx';

function AssignmentFields({ state: s, store, required = false }) {
    const t = (k, v) => store.t(k, v), i = required ? s.detail : null;
    return <div className="form-grid">
<Field label={t('project')}>
<select name="project_id" defaultValue={i?.project_id ?? ''} required={required} onChange={e => {
            const p = s.catalog.projects.find(p => p.id === e.target.value);
            const select = e.target.form?.elements.namedItem('approver_id');
            if (p && select)
                select.value = p.manager_id;
        }}>
<option value="">{t('notAssigned')}</option>{s.catalog.projects.filter(p => p.active).map(p => <option key={p.id} value={p.id}>{p.code} &middot; {p.name}</option>)}</select>
</Field>
<Field label={t('costCenter')}>
<select name="cost_center_id" defaultValue={i?.cost_center_id ?? ''} required={required}>
<option value="">{t('notAssigned')}</option>{s.catalog.cost_centers.filter(c => c.active).map(c => <option key={c.id} value={c.id}>{c.code} &middot; {c.name}</option>)}</select>
</Field>
<Field label={t('approver')}>
<select name="approver_id" defaultValue={i?.approver_id ?? ''} required={required}>
<option value="">{t('notAssigned')}</option>{s.catalog.users.filter(u => u.active && ['ADMIN', 'APPROVER'].includes(u.role)).map(u => <option key={u.id} value={u.id}>{u.display_name}</option>)}</select>
</Field>{required && <Field label={t('dueDate')}>
<input name="due_date" type="date" required defaultValue={i?.due_date ?? ''} min={i?.issue_date}/>
</Field>}</div>;
}

function FileDrop({ state: s, store, evidence = false }) {
    const t = (k, v) => store.t(k, v);
    return <div className={'file-drop ' + (s.dialogFile ? 'has-file' : '')} onDragOver={e => e.preventDefault()} onDrop={e => {
        e.preventDefault();
        store.setDialogFile(e.dataTransfer.files[0]);
    }}>
<span className="upload-mark">
<Icon name={s.dialogFile ? 'file' : 'upload'} size={26}/>
</span>
<strong>{s.dialogFile?.name ?? t(evidence ? 'uploadEvidence' : 'dropFile')}</strong>
<span>{s.dialogFile ? Math.ceil(s.dialogFile.size / 1024) + ' KB' : t('orBrowse')}</span>
<input type="file" name="file" aria-label={t('file')} accept={evidence ? '.pdf,.png,.jpg,.jpeg,.txt' : '.xml,.pdf'} required={!s.dialogFile} onChange={e => store.setDialogFile(e.target.files?.[0])}/>
<small>{t(evidence ? 'evidenceFormats' : 'supportedFiles')}</small>
</div>;
}

function FormContent({ state: s, store }) {
    const t = (k, v) => store.t(k, v), d = s.dialog, r = d.record ?? {}, isPdf = s.dialogFile?.name.toLowerCase().endsWith('.pdf');
    if (d.kind === 'import')
        return <div>
<FileDrop state={s} store={store}/>
<p className="small-print">{t(isPdf ? 'pdfManual' : 'xmlAuto')}</p>
<Field label={t('supplierName')}>
<select name="supplier_id" required={isPdf} defaultValue="">
<option value="">{t(isPdf ? 'notAssigned' : 'autoSupplier')}</option>{s.catalog.suppliers.filter(u => u.active).map(u => <option value={u.id} key={u.id}>{u.name}</option>)}</select>
</Field>{isPdf && <div className="form-grid">
<Field label={t('invoiceNumber')}>
<input name="invoice_number" required maxLength={120}/>
</Field>
<Field label={t('description')}>
<input name="description" maxLength={2000}/>
</Field>
<Field label={t('issueDate')}>
<input type="date" name="issue_date" required/>
</Field>
<Field label={t('dueDate')}>
<input type="date" name="due_date" required/>
</Field>{['net', 'tax', 'gross'].map(n => <Field key={n} label={t(n)}>
<input type="number" name={n} required min="0" max="999999999999.99" step="0.01"/>
</Field>)}<Field label={t('currency')}>
<input name="currency" value="EUR" readOnly/>
</Field>
</div>}<h3 className="form-section-title">{t('allocation')}</h3>
<AssignmentFields state={s} store={store}/>
<p className="small-print">{t('checksNotice')}</p>
</div>;
    if (d.kind === 'evidenceUpload')
        return <FileDrop state={s} store={store} evidence/>;
    if (d.kind === 'allocation')
        return <AssignmentFields state={s} store={store} required/>;
    if (['command', 'acceptEvidence', 'duplicate', 'resolveQuestion'].includes(d.kind))
        return <div>
<p className="dialog-context">{d.kind === 'acceptEvidence' ? t('evidenceNotice') : d.kind === 'duplicate' ? t('duplicateText') : d.action === 'APPROVE' ? t('guide4') : s.detail?.invoice_number}</p>
<Field label={t(d.kind === 'resolveQuestion' ? 'resolution' : 'reason')}>
<textarea name="note" rows={5} maxLength={2000} required={d.action !== 'APPROVE'} autoFocus placeholder={t('reasonPlaceholder')}/>
</Field>
</div>;
    if (d.kind === 'supplier')
        return <div>
<Field label={t('supplierName')}>
<input name="name" required maxLength={200} defaultValue={r.name ?? ''} autoFocus/>
</Field>
<div className="form-grid">
<Field label={t('email')}>
<input name="email" type="email" maxLength={254} defaultValue={r.email ?? ''}/>
</Field>
<Field label={t('vatId')}>
<input name="vat_id" maxLength={40} defaultValue={r.vat_id ?? ''}/>
</Field>
</div>
<label className="check-label">
<input type="checkbox" name="active" defaultChecked={r.active ?? true}/>{t('active')}</label>
</div>;
    if (d.kind === 'project')
        return <div>
<div className="form-grid">
<Field label={t('code')}>
<input name="code" required maxLength={40} autoFocus/>
</Field>
<Field label={t('name')}>
<input name="name" required maxLength={160}/>
</Field>
</div>
<Field label={t('projectOwner')}>
<select name="manager_id" required>{s.catalog.users.filter(u => u.active && ['ADMIN', 'APPROVER'].includes(u.role)).map(u => <option key={u.id} value={u.id}>{u.display_name}</option>)}</select>
</Field>
<label className="check-label">
<input type="checkbox" name="evidence_required" defaultChecked/>{t('evidenceRequired')}</label>
</div>;
    if (d.kind === 'costCenter')
        return <div className="form-grid">
<Field label={t('code')}>
<input name="code" required maxLength={40} autoFocus/>
</Field>
<Field label={t('name')}>
<input name="name" required maxLength={160}/>
</Field>
</div>;
    if (d.kind === 'user')
        return <div className="form-grid">
<Field label={t('displayName')}>
<input name="display_name" required maxLength={120} defaultValue={r.display_name ?? ''} autoFocus/>
</Field>
<Field label={t('email')}>
<input name="email" type="email" required maxLength={254} defaultValue={r.email ?? ''}/>
</Field>
<Field label={t('role')}>
<select name="role" defaultValue={r.role ?? 'APPROVER'}>{['ADMIN', 'FINANCE', 'APPROVER', 'VIEWER'].map(role => <option key={role} value={role}>{t(role)}</option>)}</select>
</Field>
<Field label={t('password')} hint={t('passwordHelp')}>
<input name="password" type="password" minLength={14} maxLength={128} required={!r.id} autoComplete="new-password"/>
</Field>
<label className="check-label">
<input type="checkbox" name="active" defaultChecked={r.active ?? true}/>{t('active')}</label>
</div>;
    if (d.kind === 'delegation')
        return <div>
<p className="dialog-context">{t('delegationHelp')}</p>
<div className="form-grid">{['owner', 'delegate'].map((key, n) => <Field label={t(key)} key={key}>
<select name={key + '_id'} required defaultValue={s.catalog.users.filter(u => u.active && ['ADMIN', 'APPROVER'].includes(u.role))[n]?.id}>{s.catalog.users.filter(u => u.active && ['ADMIN', 'APPROVER'].includes(u.role)).map(u => <option key={u.id} value={u.id}>{u.display_name}</option>)}</select>
</Field>)}<Field label={t('from')}>
<input type="datetime-local" name="valid_from" required defaultValue={localDateTime()}/>
</Field>
<Field label={t('until')}>
<input type="datetime-local" name="valid_until" required defaultValue={localDateTime(new Date(Date.now() + 7 * 86400000).toISOString())}/>
</Field>
</div>
<p className="small-print">{t('browserTimezone')}</p>
</div>;
    if (d.kind === 'export')
        return <div className="export-confirm">
<span className="empty-icon">
<Icon name="download" size={28}/>
</span>
<strong>{r.invoices?.length} {t('invoiceCount')}</strong>
<p>{t('exportConfirmText')}</p>
<div className="notice">
<Icon name="info"/>
<p>{t('exportBoundary')}</p>
</div>
</div>;
    if (d.kind === 'reset')
        return <p>{t('resetText')}</p>;
    return null;
}

export function Modal({ state: s, store }) {
    const d = s.dialog;
    if (!d)
        return null;
    const t = (k, v) => store.t(k, v);
    const simple = ['help', 'commands', 'notifications'].includes(d.kind);
    const titles = { import: 'import', allocation: 'editAllocation', evidenceUpload: 'uploadEvidence', acceptEvidence: 'acceptEvidence', duplicate: 'clearDuplicate', resolveQuestion: 'resolveQuestion', supplier: d.record ? 'edit' : 'addSupplier', project: 'addProject', costCenter: 'addCostCenter', user: d.record ? 'edit' : 'addUser', delegation: 'addDelegation', export: 'exportConfirm', reset: 'resetConfirm', help: 'help', commands: 'commands', notifications: 'notifications', command: d.action };
    return <dialog className={'app-dialog ' + (['import', 'user'].includes(d.kind) ? 'wide' : '')} aria-labelledby="dialog-title" ref={node => {
            if (node && !node.open)
                node.showModal();
        }} onCancel={e => {
        e.preventDefault();
        store.closeDialog();
    }} onClick={e => {
            if (e.target === e.currentTarget)
                store.closeDialog();
        }}>
<div className="dialog-header">
<div>
<span className="eyebrow">BelegFlow</span>
<h2 id="dialog-title">{t(titles[d.kind] ?? 'continue')}</h2>
</div>
<IconButton icon="close" label={t('close')} onClick={() => store.closeDialog()}/>
</div>{simple ? <div className="dialog-body">{d.kind === 'help' && <div className="guide">
<p>{t('guideIntro')}</p>{['guide1', 'guide2', 'guide3', 'guide4', 'guide5'].map(k => <p className="guide-step" key={k}>{t(k)}</p>)}<p className="small-print">{t('guideDocs')}</p>{s.standalone && <p className="small-print">{t('previewRenderer')}</p>}</div>}{d.kind === 'commands' && <div className="command-list">{['inbox', 'approvals', 'suppliers', 'projects', ...(finance(s.me) ? ['exports'] : []), 'audit', 'settings'].map((view, n) => <button type="button" key={view} onClick={() => store.navigate(view)} autoFocus={n === 0}>
<Icon name={{ inbox: 'inbox', approvals: 'shield', suppliers: 'building', projects: 'folder', exports: 'download', audit: 'clock', settings: 'settings' }[view]}/>
<span>{t(view)}</span>
<Icon name="arrow" size={16}/>
</button>)}<p className="small-print">{t('keyboardHint')}</p>
</div>}{d.kind === 'notifications' && (s.notifications.length ? <div className="notifications-list">{s.notifications.map(n => <div key={n.id} className={'notification ' + (!n.is_read ? 'unread' : '')}>
<span className="notification-icon">
<Icon name="bell"/>
</span>
<div>
<button type="button" className="text-button" onClick={() => store.openInvoice(n.invoice_id)}>{t('reminderDue', { number: n.invoice_number })}</button>
<small>{date(n.created_at, s.numberLocale)}</small>
</div>{!n.is_read && <IconButton icon="check" label={t('markRead')} onClick={() => store.markRead(n.id)}/>}</div>)}</div> : <Empty icon="bell" title={t('noNotifications')}/>)}{s.error && <p className="error-message" role="alert">{t('error_' + s.error)}</p>}</div> : <form onSubmit={e => {
        e.preventDefault();
        store.submitDialog(e.currentTarget);
    }}>
<div className="dialog-body">
<FormContent state={s} store={store}/>{s.error && <div className="error-message" role="alert">
<Icon name="alert"/>{t('error_' + s.error)}</div>}</div>
<div className="dialog-footer">
<Button onClick={() => store.closeDialog()} disabled={s.busy}>{t('cancel')}</Button>
<button className={'button ' + (d.action === 'REJECT' ? 'danger' : 'primary')} type="submit" disabled={s.busy}>
<Icon name={d.kind === 'import' || d.kind === 'evidenceUpload' ? 'upload' : 'check'}/>
<span>{s.busy ? t('working') : t(d.kind === 'import' ? 'import' : d.kind === 'export' ? 'EXPORT' : d.kind === 'command' ? d.action : 'save')}</span>
</button>
</div>
</form>}</dialog>;
}

