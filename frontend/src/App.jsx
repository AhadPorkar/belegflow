import React from 'react';
import { money, date, time, initials, localDateTime } from './format.ts';
import { Icon, Logo, Button, IconButton, Badge, Avatar, Empty, Field, finance, PageHeading } from './components/ui.jsx';
import { Sidebar, Topbar } from './components/Shell.jsx';
import { Workspace } from './features/invoices/Workspace.jsx';
import { Suppliers, Projects } from './features/catalog/CatalogPages.jsx';
import { Exports } from './features/accounting/Exports.jsx';
import { AuditPage } from './features/audit/AuditPage.jsx';
import { Settings } from './features/settings/Settings.jsx';
import { Modal } from './components/Dialogs.jsx';
import { Login } from './features/identity/Login.jsx';

export function App({ state: s, store }) {
    const t = (k, v) => store.t(k, v);
    if (!s.ready)
        return <div className="boot-screen">
<Logo />
<span>{t('loading')}</span>
</div>;
    if (!s.me)
        return <Login state={s} store={store}/>;
    return <div className="app-shell">
<a className="skip-link" href="#main-content">{t('workspace')}</a>
<Sidebar state={s} store={store}/>
<div className="app-main">
<Topbar state={s} store={store}/>{s.demo && <div className={'demo-banner ' + (s.standalone ? 'standalone' : '')}>
<span className="demo-tag">DEMO</span>{t(s.standalone ? 'standaloneBanner' : 'demoBanner')}{s.standalone && <button type="button" onClick={() => store.openDialog('reset')} title={t('resetDemo')}>
<Icon name="refresh" size={12}/>
</button>}</div>}<main id="main-content">{(s.view === 'inbox' || s.view === 'approvals') ? <Workspace state={s} store={store}/> : s.view === 'suppliers' ? <Suppliers state={s} store={store}/> : s.view === 'projects' ? <Projects state={s} store={store}/> : s.view === 'exports' ? (finance(s.me) ? <Exports state={s} store={store}/> : <Empty icon="lock" title={t('error_FORBIDDEN')}/>) : s.view === 'audit' ? <AuditPage state={s} store={store}/> : <Settings state={s} store={store}/>}</main>
</div>{s.error && !s.dialog && <div className="global-error" role="alert">
<Icon name="alert"/>
<span>{t('error_' + s.error)}</span>
<Button onClick={() => store.refresh()}>{t('retry')}</Button>
<IconButton icon="close" label={t('close')} onClick={() => store.patch({ error: null })}/>
</div>}{s.toast && <div className="toast" role="status">
<span>
<Icon name="check" size={14}/>
</span>{t(s.toast)}</div>}<Modal state={s} store={store}/>
</div>;
}

export class ErrorBoundary extends React.Component {
    constructor(props) {
        super(props);
        this.state = { failed: false };
    }
    static getDerivedStateFromError() {
        return { failed: true };
    }
    render() {
        return this.state.failed ? <div className="empty-state">
<h1>BelegFlow</h1>
<p>{this.props.store.t('errorGeneric')}</p>
<Button onClick={() => location.reload()}>{this.props.store.t('retry')}</Button>
</div> : this.props.children;
    }
}

