import React from 'react';
import { money, date, time, initials, localDateTime } from '../../format.ts';
import { Icon, Logo, Button, IconButton, Badge, Avatar, Empty, Field, finance, PageHeading } from '../../components/ui.jsx';

export function Exports({ state: s, store }) {
    const t = (k, v) => store.t(k, v);
    return <div className="standard-page">
<PageHeading title={t('exportTitle')} subtitle={t('exportSub')}>
<Button tone="primary" icon="download" disabled={!s.summary.approved || s.busy} onClick={() => store.exportApproved()}>{t('exportAll')}</Button>
</PageHeading>
<div className="notice">
<Icon name="info"/>
<p>{t('exportBoundary')}</p>
</div>{s.exports.length ? <div className="table-card">
<table>
<thead>
<tr>{['fileName', 'invoiceCount', 'createdBy', 'createdAt', 'download'].map(k => <th scope="col" key={k}>{t(k)}</th>)}</tr>
</thead>
<tbody>{s.exports.map(e => <tr key={e.id}>
<td>
<div className="table-person">
<span className="file-icon">
<Icon name="file"/>
</span>
<div>
<strong>{e.filename}</strong>
<small className="mono">SHA-256 {e.sha256.slice(0, 18)}...</small>
</div>
</div>
</td>
<td>{e.invoice_count}</td>
<td>{e.created_by_name}</td>
<td>{date(e.created_at, s.numberLocale)}</td>
<td>
<IconButton icon="download" label={t('download')} onClick={() => store.download('/exports/' + e.id + '/download', e.filename)}/>
</td>
</tr>)}</tbody>
</table>
</div> : <Empty icon="download" title={t('noExports')} body={t('noExportsSub')}/>}</div>;
}

