import React from 'react';
import { money, date, time, initials, localDateTime } from '../../format.ts';
import { Icon, Logo, Button, IconButton, Badge, Avatar, Empty, Field, finance, PageHeading } from '../../components/ui.jsx';

function Metrics({ state: s, store }) {
    const t = (k, v) => store.t(k, v);
    return <div className="metrics">
<article className="metric featured">
<div className="metric-top">
<span>{t('openValue')}</span>
<span className="metric-icon">
<Icon name="euro"/>
</span>
</div>
<strong>{money(s.summary.open_value, s.numberLocale)}</strong>
<div className="metric-foot">
<span className="tiny-dot"/>{t('workspaceTotal', { count: s.summary.total })}</div>
<span className="metric-decoration"/>
</article>
<article className="metric">
<div className="metric-top">
<span>{t('inReview')}</span>
<Icon name="file"/>
</div>
<strong>{s.summary.review.toString().padStart(2, '0')}</strong>
<div className="metric-foot">
<span className="tiny-dot amber"/>{t('overdueCount', { count: s.summary.overdue })}</div>
</article>
<article className="metric">
<div className="metric-top">
<span>{t('awaiting')}</span>
<Icon name="clock"/>
</div>
<strong>{s.summary.awaiting.toString().padStart(2, '0')}</strong>
<div className="metric-foot">
<span className="tiny-dot violet"/>{t('evidenceFirst')}</div>
</article>
<article className="metric">
<div className="metric-top">
<span>{t('readyExport')}</span>
<Icon name="shield"/>
</div>
<strong>{money(s.summary.approved_value, s.numberLocale)}</strong>
<div className="metric-foot">
<span className="tiny-dot green"/>{t('readyCount', { count: s.summary.approved })}</div>
</article>
</div>;
}

function InvoiceQueue({ state: s, store }) {
    const t = (k, v) => store.t(k, v);
    return <section className={'invoice-queue mobile-' + (s.mobilePane === 'queue' ? 'visible' : 'hidden')} aria-label={t('queue')}>
<div className="queue-head">
<div className="section-heading">
<h2>{t('allInvoices')}</h2>
<span className="counter">{s.total}</span>
<IconButton icon="refresh" label={t('retry')} onClick={() => store.refresh()}/>
</div>
<div className="input-icon">
<Icon name="search"/>
<input id="invoice-search" type="search" value={s.query} onChange={e => store.search(e.target.value)} placeholder={t('search')} aria-label={t('search')}/>
</div>
<select className="status-filter" value={s.stage} aria-label={t('filter')} onChange={e => store.filter(e.target.value)}>
<option value="">{t('allInvoices')}</option>{['RECEIVED', 'REVIEWED', 'CLARIFICATION', 'READY_FOR_APPROVAL', 'APPROVED', 'REJECTED', 'EXPORTED'].map(v => <option key={v} value={v}>{t(v)}</option>)}</select>
</div>
<div className={'queue-list ' + (s.loading ? 'is-loading' : '')}>
 {s.invoices.map((invoice, index) => <button type="button" className={'invoice-row ' + (s.selectedId === invoice.id ? 'selected' : '')} key={invoice.id} aria-pressed={s.selectedId === invoice.id} onClick={() => store.selectInvoice(invoice.id)}>
<div className="row-top">
<Avatar name={invoice.supplier_name} tone={index % 5}/>
<div className="row-identity">
<strong>{invoice.supplier_name}</strong>
<span>{invoice.invoice_number}</span>
</div>
</div>
<div className="row-mid">
<span>{invoice.project_code ?? t('notAssigned')}</span>
<strong>{money(invoice.gross, s.numberLocale, invoice.currency)}</strong>
</div>
<div className="row-bottom">
<Badge stage={invoice.stage} t={t}/>{invoice.duplicate_warning && !invoice.duplicate_cleared ? <span className="row-warning" title={t('duplicateTitle')}>
<Icon name="alert" size={14}/>
</span> : <span className="row-due">{date(invoice.due_date, s.numberLocale).replace(/,?\s20\d\d/, '')}</span>}</div>
</button>)}
 {!s.invoices.length && !s.loading && <Empty title={t('noInvoices')} body={t('noInvoicesSub')}/>}</div>
<div className="queue-footer">
<span>{t('page', { number: s.page + 1 })}</span>
<IconButton icon="chevron" className="icon-button previous" label={t('previous')} disabled={s.page === 0} onClick={() => store.page(-1)}/>
<IconButton icon="chevron" label={t('next')} disabled={(s.page + 1) * 50 >= s.total} onClick={() => store.page(1)}/>
</div>
</section>;
}

function AllocationStrip({ state: s, store }) {
    const t = (k, v) => store.t(k, v), i = s.detail;
    return <div className="allocation-strip">
<div>
<span>
<Icon name="folder" size={13}/>{t('project')}</span>
<strong>{i.project_name ?? t('notAssigned')}</strong>
</div>
<div>
<span>
<Icon name="grid" size={13}/>{t('costCenter')}</span>
<strong>{i.cost_center_code ?? t('notAssigned')}</strong>
</div>{i.can_edit && <IconButton icon="edit" label={t('editAllocation')} onClick={() => store.openDialog('allocation')}/>}</div>;
}

function DocumentSheet({ state: s, store }) {
    const i = s.detail, t = (k, v) => store.t(k, v), doc = i.documents.find(d => d.kind === 'ORIGINAL');
    return <div className="document-view">
<p className="document-disclaimer">
<Icon name="info" size={14}/>{t('structuredNotice')}</p>
<article className="invoice-paper">
<div className="paper-brand">
<span className="supplier-monogram">{initials(i.supplier_name)}</span>
<div>
<strong>{i.supplier_name}</strong>
<span>{i.supplier_email}</span>
</div>
<span className="paper-format">{i.source_format}</span>
</div>
<div className="paper-title">
<h2>{t('invoiceLabel')}</h2>
<span>{i.invoice_number}</span>
</div>
<div className="paper-addresses">
<div>
<span className="paper-label">{t('billedTo')}</span>
<strong>{s.me.organization}</strong>
<span>{i.project_name}</span>
</div>
<div>
<span className="paper-label">{t('invoiceDetails')}</span>
<dl>
<dt>{t('issueDate')}</dt>
<dd>{date(i.issue_date, s.numberLocale)}</dd>
<dt>{t('dueDate')}</dt>
<dd>{date(i.due_date, s.numberLocale)}</dd>
</dl>
</div>
</div>
<div className="paper-lines">
<div className="paper-line-head">
<span>{t('summaryDescription')}</span>
<span>{t('net')}</span>
</div>
<div className="paper-line">
<div>
<strong>{i.description || i.invoice_number}</strong>
<span>{i.project_code}</span>
</div>
<span>{money(i.net, s.numberLocale, i.currency)}</span>
</div>
</div>
<div className="paper-totals">
<div>
<span>{t('net')}</span>
<strong>{money(i.net, s.numberLocale, i.currency)}</strong>
</div>
<div>
<span>{t('tax')}</span>
<strong>{money(i.tax, s.numberLocale, i.currency)}</strong>
</div>
<div className="paper-grand">
<span>{t('gross')}</span>
<strong>{money(i.gross, s.numberLocale, i.currency)}</strong>
</div>
</div>
<div className="paper-bottom">
<div>
<Icon name="shield" size={15}/>
<span>{t('originalPreserved')}</span>
</div>
<span>SHA-256 {doc?.sha256.slice(0, 12)}...</span>
</div>
</article>
<div className={'validation-strip ' + (i.basic_valid ? 'valid' : 'invalid')}>
<span>
<Icon name={i.basic_valid ? 'check' : 'alert'} size={15}/>{t('basicChecks')}</span>
<strong>{i.basic_valid ? t('complete') : t('attention')}</strong>
</div>
<p className="small-print">{t('checksNotice')}</p>
</div>;
}

function EvidenceView({ state: s, store }) {
    const i = s.detail, t = (k, v) => store.t(k, v), docs = i.documents.filter(d => d.kind === 'EVIDENCE');
    return <div className="evidence-view">
<div className="evidence-heading">
<span className={'evidence-seal ' + (i.evidence_accepted_by ? 'accepted' : '')}>
<Icon name={i.evidence_accepted_by ? 'shield' : 'folder'} size={28}/>
</span>
<h2>{t('evidence')}</h2>
<p>{t('evidenceGateText')}</p>
</div>{i.evidence_accepted_by && <div className="notice success">
<Icon name="check"/>
<div>
<strong>{t('serviceAccepted')}</strong>
<p>{t('acceptedBy', { name: i.evidence_accepted_name ?? '' })}</p>
<p>{i.evidence_note}</p>
</div>
</div>}{!docs.length ? <Empty icon="paperclip" title={t('evidenceMissing')} body={t('evidenceMissingSub')}/> : <div className="document-files">{docs.map(d => <div className="document-file" key={d.id}>
<span className="file-icon">
<Icon name="file"/>
</span>
<div>
<strong>{d.filename}</strong>
<small>{Math.ceil(d.size_bytes / 1024)} KB &middot; {date(d.created_at, s.numberLocale)}</small>
</div>
<IconButton icon="download" label={t('download')} onClick={() => store.download('/documents/' + d.id, d.filename)}/>
</div>)}</div>}<div className="evidence-buttons">{i.can_upload_evidence && <Button icon="upload" onClick={() => store.openDialog('evidenceUpload')}>{t('uploadEvidence')}</Button>}{i.can_accept_evidence && !i.evidence_accepted_by && <Button tone="primary" icon="check" onClick={() => store.openDialog('acceptEvidence')}>{t('acceptEvidence')}</Button>}</div>{!i.can_accept_evidence && !i.evidence_accepted_by && <p className="small-print">{t('evidenceOwnerOnly')}</p>}<p className="small-print">{t('evidenceNotice')}</p>
</div>;
}

function InvoicePane({ state: s, store }) {
    const i = s.detail, t = (k, v) => store.t(k, v);
    return <section className={'document-pane mobile-' + (s.mobilePane === 'document' ? 'visible' : 'hidden')} aria-label={t('document')}>{!i ? <Empty icon="file" title={s.selectedId ? t('loading') : t('selectInvoice')}/> : <div className="document-pane-inner">
<div className="document-toolbar">
<div className="tabs" role="tablist" aria-label={t('document')}>
<button type="button" role="tab" aria-selected={s.documentTab === 'document'} onClick={() => store.patch({ documentTab: 'document' })}>
<Icon name="file" size={15}/>{t('document')}</button>
<button type="button" role="tab" aria-selected={s.documentTab === 'evidence'} onClick={() => store.patch({ documentTab: 'evidence' })}>
<Icon name="paperclip" size={15}/>{t('evidence')}{i.documents.filter(d => d.kind === 'EVIDENCE').length > 0 && <em>{i.documents.filter(d => d.kind === 'EVIDENCE').length}</em>}</button>
</div>
<IconButton icon="download" label={t('original')} onClick={() => {
                const d = i.documents.find(x => x.kind === 'ORIGINAL');
                if (d)
                    store.download('/documents/' + d.id, d.filename);
            }}/>
</div>
<AllocationStrip state={s} store={store}/>
<div className="document-scroll">{s.documentTab === 'document' ? <DocumentSheet state={s} store={store}/> : <EvidenceView state={s} store={store}/>}</div>
</div>}</section>;
}

function Readiness({ state: s, store }) {
    const i = s.detail, t = (k, v) => store.t(k, v), items = [['basicChecks', i.basic_valid], ['allocationCheck', Boolean(i.project_id && i.cost_center_id && i.approver_id)], ['duplicateCheck', !i.duplicate_warning || i.duplicate_cleared], ['serviceCheck', !i.evidence_required || Boolean(i.evidence_accepted_by)]];
    return <div className="readiness">
<h3>{t('readiness')}</h3>{items.map(([key, passed]) => <div key={key} className={'readiness-row ' + (passed ? 'passed' : 'pending')}>
<span className="check-circle">
<Icon name={passed ? 'check' : 'clock'} size={11}/>
</span>
<span>{t(key)}</span>
<small>{passed ? t('complete') : t('attention')}</small>
</div>)}</div>;
}

function Timeline({ state: s, store }) {
    const t = (k, v) => store.t(k, v);
    return <div className="timeline">{s.detail.audit.map((event, index) => <div className="timeline-event" key={event.id}>
<span className={'timeline-dot ' + (index === 0 ? 'latest' : '')}>
<Icon name={event.event_type.includes('APPROVED') || event.event_type.includes('ACCEPTED') ? 'check' : event.event_type.includes('QUESTION') || event.event_type.includes('CLARIF') ? 'help' : 'file'} size={12}/>
</span>
<div>
<strong>{t('event_' + event.event_type)}</strong>
<span>{event.actor_name}</span>{event.detail && !/^[a-f\d-]{30,}|^[\w-]{30,} \/ /i.test(event.detail) && <p>{event.detail}</p>}<small>{date(event.created_at, s.numberLocale)} &middot; {time(event.created_at, s.numberLocale)}</small>
</div>
</div>)}</div>;
}

function Discussion({ state: s, store }) {
    const t = (k, v) => store.t(k, v);
    return <div className="discussion">
<div className="discussion-list">{!s.detail.discussion.length && <p className="muted">{t('noComments')}</p>}{s.detail.discussion.map(c => <article className={'comment ' + (c.is_question && !c.resolved_at ? 'question' : '')} key={c.id}>
<header>
<Avatar name={c.author_name} tone={2}/>
<div>
<strong>{c.author_name}</strong>
<small>{date(c.created_at, s.numberLocale)}</small>
</div>
</header>{c.is_question && <span className={'comment-state ' + (c.resolved_at ? 'resolved' : '')}>{t(c.resolved_at ? 'resolved' : 'openQuestion')}</span>}<p>{c.body}</p>{c.resolution && <div className="resolution">
<strong>{t('resolution')}</strong>
<p>{c.resolution}</p>
</div>}{c.is_question && !c.resolved_at && finance(s.me) && <Button icon="check" onClick={() => store.openDialog('resolveQuestion', undefined, undefined, c.id)}>{t('resolveQuestion')}</Button>}</article>)}</div>{s.me.role !== 'VIEWER' && <form className="comment-form" onSubmit={async (e) => {
                e.preventDefault();
                const form = e.currentTarget;
                const value = new FormData(form).get('body');
                if (await store.comment(String(value ?? '')))
                    form.reset();
            }}>
<textarea name="body" required maxLength={2000} rows={3} aria-label={t('commentPlaceholder')} placeholder={t('commentPlaceholder')}/>
<button className="button secondary" type="submit" disabled={s.busy}>
<Icon name="send" size={15}/>
<span>{t('sendComment')}</span>
</button>
</form>}</div>;
}

function DecisionPane({ state: s, store }) {
    const i = s.detail, t = (k, v) => store.t(k, v);
    if (!i)
        return <section className={'decision-pane mobile-' + (s.mobilePane === 'decision' ? 'visible' : 'hidden')}/>;
    const allowed = i.allowed_actions, primary = ['REVIEW', 'RESOLVE', 'SUBMIT', 'APPROVE', 'EXPORT', 'REOPEN'].find(a => allowed.includes(a));
    const expected = { RECEIVED: 'REVIEW', REVIEWED: 'SUBMIT', CLARIFICATION: 'RESOLVE', READY_FOR_APPROVAL: 'APPROVE', APPROVED: 'EXPORT', REJECTED: 'REOPEN' }[i.stage];
    return <section className={'decision-pane mobile-' + (s.mobilePane === 'decision' ? 'visible' : 'hidden')} aria-label={t('decisionCenter')}>
<div className="decision-scroll">
<div className="decision-header">
<span className="eyebrow">{t('nextStep')}</span>
<h2>{t('decisionCenter')}</h2>
<Badge stage={i.stage} t={t}/>
</div>
<div className="workflow-rail">{[true, i.stage !== 'RECEIVED', !i.evidence_required || !!i.evidence_accepted_by, ['APPROVED', 'EXPORTED'].includes(i.stage), i.stage === 'EXPORTED'].map((done, n) => <span key={n} className={done ? 'done' : ''} title={t(['RECEIVED', 'REVIEWED', 'evidence', 'APPROVED', 'EXPORTED'][n])}>{done ? <Icon name="check" size={11}/> : n + 1}</span>)}</div>
<Readiness state={s} store={store}/>
 {i.duplicate_warning && !i.duplicate_cleared && <div className="attention-card duplicate-card">
<span className="attention-icon">
<Icon name="alert" size={18}/>
</span>
<strong>{t('duplicateTitle')}</strong>
<p>{t('duplicateText')}</p>{i.can_edit && <button className="text-button" type="button" onClick={() => store.openDialog('duplicate')}>{t('clearDuplicate')}<Icon name="arrow" size={14}/>
</button>}</div>}
 {i.evidence_required && !i.evidence_accepted_by && <div className="attention-card evidence-card">
<span className="attention-icon">
<Icon name="lock" size={18}/>
</span>
<strong>{t('evidenceGateTitle')}</strong>
<p>{t('evidenceGateText')}</p>
<button className="text-button" type="button" onClick={() => store.patch({ documentTab: 'evidence', mobilePane: 'document' })}>{t('reviewEvidence')}<Icon name="arrow" size={14}/>
</button>
</div>}
 <div className="assigned-owner">
<Avatar name={i.approver_name ?? '?'} tone={1}/>
<div>
<small>{t('approver')}</small>
<strong>{i.approver_name ?? t('notAssigned')}</strong>
</div>
<Icon name="shield" size={17}/>
</div>
 {i.second_required && <div className="second-approval">
<div>
<Icon name="users" size={17}/>
<strong>{t('secondRequired')}</strong>
</div>
<p>{t('secondText', { amount: money(s.catalog.threshold, s.numberLocale) })}</p>{i.first_approved_by && !i.second_approved_by && <span>{t('firstRecorded')} &middot; {t('waitingSecond')}</span>}</div>}
 <div className="activity-tabs tabs" role="tablist" aria-label={t('activity')}>
<button type="button" role="tab" aria-selected={s.activityTab === 'activity'} onClick={() => store.patch({ activityTab: 'activity' })}>{t('activity')}</button>
<button type="button" role="tab" aria-selected={s.activityTab === 'discussion'} onClick={() => store.patch({ activityTab: 'discussion' })}>{t('discussion')}<em>{i.discussion.length}</em>
</button>
</div>{s.activityTab === 'activity' ? <Timeline state={s} store={store}/> : <Discussion state={s} store={store}/>}</div>
 <div className="decision-actions">{s.me.role === 'VIEWER' ? <p className="small-print">{t('readOnly')}</p> : <div>{(primary || expected) && <Button tone="primary" className="full" icon={primary === 'EXPORT' ? 'download' : 'check'} disabled={!primary || s.busy} onClick={() => store.command(primary)}>{t(primary ?? expected)}</Button>}{!primary && expected && <p className="action-hint">{t(i.stage === 'READY_FOR_APPROVAL' ? 'waitingForApprover' : 'approvalBlocked')}</p>}<div className="secondary-actions">{allowed.includes('CLARIFY') && <Button icon="help" disabled={s.busy} onClick={() => store.command('CLARIFY')}>{t('CLARIFY')}</Button>}{allowed.includes('REJECT') && <button className="reject-button" type="button" disabled={s.busy} onClick={() => store.command('REJECT')} title={t('REJECT')}>
<Icon name="close" size={16}/>
</button>}</div>
</div>}<div className="decision-version">
<Icon name="lock" size={11}/>{t('currentVersion', { number: i.version })}</div>
</div>
</section>;
}

export function Workspace({ state: s, store }) {
    const t = (k, v) => store.t(k, v);
    return <div className="workspace-page">
<PageHeading title={t(s.view === 'approvals' ? 'approvalTitle' : 'inboxTitle')} subtitle={t(s.view === 'approvals' ? 'approvalSub' : 'inboxSub')}>{finance(s.me) && <Button tone="primary" icon="plus" onClick={() => store.openDialog('import')}>{t('import')}</Button>}</PageHeading>
<Metrics state={s} store={store}/>
<div className="mobile-tabs">{['queue', 'document', 'decision'].map(p => <button type="button" key={p} aria-pressed={s.mobilePane === p} onClick={() => store.patch({ mobilePane: p })}>{t(p)}</button>)}</div>
<div className="workspace-grid">
<InvoiceQueue state={s} store={store}/>
<InvoicePane state={s} store={store}/>
<DecisionPane state={s} store={store}/>
</div>
</div>;
}

