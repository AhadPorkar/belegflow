import React from 'react';
import { money, date, time, initials, localDateTime } from '../../format.ts';
import { Icon, Logo, Button, IconButton, Badge, Avatar, Empty, Field, finance, PageHeading } from '../../components/ui.jsx';

export function Suppliers({ state: s, store }) {
    const t = (k, v) => store.t(k, v);
    return <div className="standard-page">
<PageHeading title={t('supplierTitle')} subtitle={t('supplierSub')}>{finance(s.me) && <Button tone="primary" icon="plus" onClick={() => store.openDialog('supplier')}>{t('addSupplier')}</Button>}</PageHeading>
<div className="table-card">
<table>
<thead>
<tr>{['supplierName', 'email', 'vatId', 'status', 'edit'].map(k => <th key={k} scope="col">{t(k)}</th>)}</tr>
</thead>
<tbody>{s.catalog.suppliers.map((u, n) => <tr key={u.id}>
<td>
<div className="table-person">
<Avatar name={u.name} tone={n % 5}/>
<strong>{u.name}</strong>
</div>
</td>
<td>{u.email || '\u2014'}</td>
<td className="mono">{u.vat_id || '\u2014'}</td>
<td>
<span className={'pill ' + (u.active ? 'positive' : 'neutral')}>{t(u.active ? 'active' : 'inactive')}</span>
</td>
<td>{finance(s.me) && <IconButton icon="edit" label={t('edit')} onClick={() => store.openDialog('supplier', u)}/>}</td>
</tr>)}</tbody>
</table>{!s.catalog.suppliers.length && <Empty icon="building" title={t('addSupplier')}/>}</div>
</div>;
}

export function Projects({ state: s, store }) {
    const t = (k, v) => store.t(k, v);
    return <div className="standard-page">
<PageHeading title={t('projectTitle')} subtitle={t('projectSub')}>{finance(s.me) && <Button tone="primary" icon="plus" onClick={() => store.openDialog('project')}>{t('addProject')}</Button>}</PageHeading>
<div className="project-grid">{s.catalog.projects.map((p, n) => <article className="project-card" key={p.id}>
<div className="project-card-head">
<span className={'project-icon tone-' + n % 3}>
<Icon name="folder" size={25}/>
</span>
<span className="mono">{p.code}</span>
</div>
<h2>{p.name}</h2>
<p className="project-policy">
<Icon name={p.evidence_required ? 'shield' : 'check'} size={15}/>{p.evidence_required ? t('evidenceFirst') : t('notRequired')}</p>
<div className="project-owner">
<Avatar name={p.manager_name} tone={n % 5}/>
<div>
<small>{t('projectOwner')}</small>
<strong>{p.manager_name}</strong>
</div>
</div>
</article>)}</div>
<div className="section-bar">
<h2>{t('costCenters')}</h2>{finance(s.me) && <Button icon="plus" onClick={() => store.openDialog('costCenter')}>{t('addCostCenter')}</Button>}</div>
<div className="cost-center-grid">{s.catalog.cost_centers.map(c => <div className="cost-center" key={c.id}>
<Icon name="grid"/>
<span>
<strong>{c.name}</strong>
<small>{c.code}</small>
</span>
</div>)}</div>
</div>;
}

