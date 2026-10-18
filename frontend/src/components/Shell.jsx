import React from 'react';
import { money, date, time, initials, localDateTime } from '../format.ts';
import { Icon, Logo, Button, IconButton, Badge, Avatar, Empty, Field, finance, PageHeading } from './ui.jsx';

export function Sidebar({ state: s, store }) {
    const t = (k, v) => store.t(k, v), navigation = [['inbox', 'inbox', s.summary.review], ['approvals', 'shield', s.summary.awaiting], ['suppliers', 'building'], ['projects', 'folder'], ['exports', 'download'], ['audit', 'clock']];
    return <aside className="sidebar">
<a className="brand" href="#" onClick={e => {
        e.preventDefault();
        store.navigate('inbox');
    }} aria-label="BelegFlow">
<Logo />
<span>Beleg<span className="brand-flow">Flow</span>
</span>
<span className="brand-dot"/>
</a>
<div className="brand-caption">{t('brandTag')}</div>
  <div className="organization">
<span className="org-monogram">N</span>
<div>
<strong>{s.me.organization.replace(' GmbH (demo)', '').replace(' (demo)', '')}</strong>
<small>{t('workspace')}</small>
</div>
<Icon name="down" size={14}/>
</div>
  <div className="nav-label">{t('workspace')}</div>
<nav aria-label={t('workspace')}>{navigation.filter(([view]) => view !== 'exports' || finance(s.me)).map(([view, icon, count], index) => <div className="nav-group" key={view}>{index === 2 && <div className="nav-label second">{t('manage')}</div>}<button type="button" className={'nav-item ' + (s.view === view ? 'active' : '')} onClick={() => store.navigate(view)} title={t(view)} aria-current={s.view === view ? 'page' : undefined}>
<Icon name={icon}/>
<span>{t(view)}</span>{count > 0 && <em>{count}</em>}</button>
</div>)}</nav>
  <div className="sidebar-bottom">
<div className="sidebar-note">
<span className="mini-orbit">
<Icon name="spark" size={16}/>
</span>
<strong>{t('evidenceFirst')}</strong>
<p>{t('guideIntro')}</p>
</div>
   <button className={'nav-item ' + (s.view === 'settings' ? 'active' : '')} type="button" onClick={() => store.navigate('settings')} title={t('settings')}>
<Icon name="settings"/>
<span>{t('settings')}</span>
</button>
   <button className="nav-item" type="button" onClick={() => store.openDialog('help')} title={t('help')}>
<Icon name="help"/>
<span>{t('help')}</span>
</button>
   {s.demo && <label className="demo-role">
<span>{t('demoRole')}</span>
<select aria-label={t('demoRole')} value={s.me.email} disabled={s.busy} onChange={e => store.loginDemo(e.target.value)}>{[['finance', 'Nora / ' + t('FINANCE')], ['leon', 'Leon / ' + t('APPROVER')], ['admin', 'Alex / ' + t('ADMIN')], ['mira', 'Mira / ' + t('APPROVER')], ['viewer', 'Jan / ' + t('VIEWER')], ['isolated', t('isolatedWorkspace')]].map(([key, name]) => <option key={key} value={key + '@belegflow.demo'}>{name}</option>)}</select>
</label>}
   <div className="profile">
<Avatar name={s.me.name} tone={1}/>
<div className="profile-name">
<strong>{s.me.name}</strong>
<small>{t(s.me.role)}</small>
</div>
<IconButton icon="logout" label={t('logout')} onClick={() => store.logout()}/>
</div>
  </div>
</aside>;
}

export function Topbar({ state: s, store }) {
    const t = (k, v) => store.t(k, v);
    return <header className="topbar">
<div className="breadcrumbs">
<span>{t('workspace')}</span>
<Icon name="chevron" size={13}/>
<strong>{t(s.view)}</strong>
</div>
<div className="topbar-actions">
<button className="quick-search" type="button" onClick={() => store.openDialog('commands')} aria-label={t('searchCommands')}>
<Icon name="search"/>
<span>{t('searchCommands')}</span>
<kbd>Ctrl K</kbd>
</button>
<div className="locale-switch" aria-label={t('language')}>
<button type="button" aria-pressed={s.locale === 'en'} onClick={() => store.setLocale('en')}>EN</button>
<button type="button" aria-pressed={s.locale === 'de'} onClick={() => store.setLocale('de')}>DE</button>
</div>
<IconButton icon={s.theme === 'light' ? 'moon' : 'sun'} label={t('theme')} onClick={() => store.toggleTheme()}/>
<span className="notification-wrap">
<IconButton icon="bell" label={t('notifications')} onClick={() => store.openDialog('notifications')}/>{s.notifications.some(n => !n.is_read) && <i />}</span>
<Avatar name={s.me.name} tone={1} className="header-avatar"/>
<IconButton className="icon-button mobile-logout" icon="logout" label={t('logout')} onClick={() => store.logout()}/>
</div>
</header>;
}

