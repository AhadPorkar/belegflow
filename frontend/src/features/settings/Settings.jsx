import React from 'react';
import { money, date, time, initials, localDateTime } from '../../format.ts';
import { Icon, Logo, Button, IconButton, Badge, Avatar, Empty, Field, finance, PageHeading } from '../../components/ui.jsx';

export function Settings({ state: s, store }) {
    const t = (k, v) => store.t(k, v), admin = s.me.role === 'ADMIN';
    return <div className="standard-page">
<div className="mobile-profile">
<Avatar name={s.me.name}/>
<div>
<strong>{s.me.name}</strong>
<small>{t(s.me.role)}</small>{s.demo && <select aria-label={t('demoRole')} value={s.me.email} disabled={s.busy} onChange={e => store.loginDemo(e.target.value)}>{[['finance', 'Nora'], ['leon', 'Leon'], ['admin', 'Alex'], ['mira', 'Mira'], ['viewer', 'Jan'], ['isolated', t('isolatedWorkspace')]].map(([key, name]) => <option value={key + '@belegflow.demo'} key={key}>{name}</option>)}</select>}</div>
</div>
<PageHeading title={t('settingsTitle')} subtitle={t('settingsSub')}/>
<div className="settings-grid">
<section className="settings-card">
<div className="settings-card-heading">
<Icon name="shield"/>
<h2>{t('approvalRules')}</h2>
</div>
<form onSubmit={e => {
        e.preventDefault();
        store.saveRules(String(new FormData(e.currentTarget).get('threshold')));
    }}>
<Field id="settings-threshold" label={t('threshold')} hint={t('thresholdHelp')}>
<input key={s.catalog.threshold} name="threshold" type="number" step="0.01" min="0.01" max="999999999999.99" required defaultValue={s.catalog.threshold} disabled={!admin}/>
</Field>{admin && <button className="button primary" type="submit" disabled={s.busy}>{t('save')}</button>}</form>
</section>
<section className="settings-card">
<div className="settings-card-heading">
<Icon name="settings"/>
<h2>{t('preferences')}</h2>
</div>
<Field id="settings-language" label={t('language')}>
<select name="interface_language" value={s.locale} onChange={e => store.setLocale(e.target.value)}>
<option value="en">English</option>
<option value="de">Deutsch</option>
</select>
</Field>
<Field id="settings-number-format" label={t('numberFormat')}>
<select name="number_locale" value={s.numberLocale} onChange={e => store.setNumberLocale(e.target.value)}>
<option value="en-GB">1,234.56 &middot; 18 Oct 2026</option>
<option value="de-DE">1.234,56 &middot; 18.10.2026</option>
</select>
</Field>
<Button icon={s.theme === 'light' ? 'moon' : 'sun'} onClick={() => store.toggleTheme()}>{t(s.theme === 'light' ? 'dark' : 'light')}</Button>
</section>
</div>
 <div className="section-bar">
<h2>{t('team')}</h2>{admin && <Button icon="plus" onClick={() => store.openDialog('user')}>{t('addUser')}</Button>}</div>{!admin && <p className="muted">{t('adminOnly')}</p>}<div className="table-card">
<table>
<thead>
<tr>{['name', 'email', 'role', 'status', 'edit'].map(k => <th scope="col" key={k}>{t(k)}</th>)}</tr>
</thead>
<tbody>{s.catalog.users.map((u, n) => <tr key={u.id}>
<td>
<div className="table-person">
<Avatar name={u.display_name} tone={n % 5}/>
<strong>{u.display_name}</strong>
</div>
</td>
<td>{u.email}</td>
<td>{t(u.role)}</td>
<td>
<span className={'pill ' + (u.active ? 'positive' : 'neutral')}>{t(u.active ? 'active' : 'inactive')}</span>
</td>
<td>{admin && <IconButton icon="edit" label={t('edit')} onClick={() => store.openDialog('user', u)}/>}</td>
</tr>)}</tbody>
</table>
</div>
 <div className="section-bar">
<h2>{t('delegations')}</h2>{admin && <Button icon="plus" onClick={() => store.openDialog('delegation')}>{t('addDelegation')}</Button>}</div>
<p className="muted explanatory">{t('delegationHelp')}</p>
<div className="table-card">
<table>
<thead>
<tr>{['owner', 'delegate', 'from', 'until', 'status'].map(k => <th scope="col" key={k}>{t(k)}</th>)}</tr>
</thead>
<tbody>{s.catalog.delegations.map(d => <tr key={d.id}>
<td>{d.owner_name}</td>
<td>{d.delegate_name}</td>
<td>{date(d.valid_from, s.numberLocale)} {time(d.valid_from, s.numberLocale)}</td>
<td>{date(d.valid_until, s.numberLocale)} {time(d.valid_until, s.numberLocale)}</td>
<td>{d.active && admin ? <button className="text-button danger-text" type="button" onClick={() => store.revoke(d.id)}>{t('revoke')}</button> : t(d.active ? 'active' : 'inactive')}</td>
</tr>)}</tbody>
</table>{!s.catalog.delegations.length && <p className="table-empty">{t('noDelegations')}</p>}</div>
<section className="settings-card reminder-settings">
<div className="settings-card-heading">
<Icon name="bell"/>
<h2>{t('notifications')}</h2>
</div>
<p>{t('reminderHelp')}</p>{admin && <Button icon="refresh" onClick={() => store.runReminders()}>{t('runReminders')}</Button>}{s.standalone && <Button icon="refresh" onClick={() => store.openDialog('reset')}>{t('resetDemo')}</Button>}</section>
</div>;
}

