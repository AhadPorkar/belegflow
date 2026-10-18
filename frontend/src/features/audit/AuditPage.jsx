import React from 'react';
import { money, date, time, initials, localDateTime } from '../../format.ts';
import { Icon, Logo, Button, IconButton, Badge, Avatar, Empty, Field, finance, PageHeading } from '../../components/ui.jsx';

export function AuditPage({ state: s, store }) {
    const t = (k, v) => store.t(k, v);
    return <div className="standard-page">
<PageHeading title={t('auditTitle')} subtitle={t('auditSub')}/>
<div className="notice subtle">
<Icon name="shield"/>
<p>{t('auditNotice')}</p>
</div>
<div className="table-card">
<table>
<thead>
<tr>{['event', 'actor', 'invoiceNumber', 'createdAt'].map(k => <th scope="col" key={k}>{t(k)}</th>)}</tr>
</thead>
<tbody>{s.audit.map(a => <tr key={a.id}>
<td>
<strong>{t('event_' + a.event_type)}</strong>
<small className="audit-detail">{a.detail}</small>
</td>
<td>{a.actor_name}</td>
<td>{a.invoice_id ? <button type="button" className="text-button" onClick={() => store.openInvoice(a.invoice_id)}>{a.invoice_number ?? a.invoice_id.slice(0, 8)}<Icon name="arrow" size={13}/>
</button> : '\u2014'}</td>
<td className="nowrap">{date(a.created_at, s.numberLocale)}<small>{time(a.created_at, s.numberLocale)}</small>
</td>
</tr>)}</tbody>
</table>
</div>{s.auditNext > 0 && <Button className="load-more" icon="clock" onClick={() => store.earlierAudit()}>{t('loadEarlier')}</Button>}</div>;
}

