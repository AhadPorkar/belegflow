import React from 'react';
import { money, date, time, initials, localDateTime } from '../../format.ts';
import { Icon, Logo, Button, IconButton, Badge, Avatar, Empty, Field, finance, PageHeading } from '../../components/ui.jsx';

export function Login({ state: s, store }) {
    const t = (k, v) => store.t(k, v);
    return <main className="login-page">
<section className="login-story">
<a href="#" className="brand">
<Logo />
<span>BelegFlow</span>
</a>
<div className="login-copy">
<span className="eyebrow">{t('brandTag')}</span>
<h1>{t('loginTitle')}</h1>
<p>{t('loginSub')}</p>
<div className="login-orbit">
<span className="orbit one"/>
<span className="orbit two"/>
<span className="orbit three"/>
<div className="orbit-document">
<Icon name="file" size={40}/>
<span />
<span />
<strong>
<Icon name="check" size={15}/>{t('APPROVED')}</strong>
</div>
<span className="orbit-badge">
<Icon name="shield" size={25}/>
</span>
</div>
</div>
<p className="login-footer">{t('guideIntro')}</p>
</section>
<section className="login-form-side">
<div className="login-controls">
<div className="locale-switch">
<button type="button" aria-pressed={s.locale === 'en'} onClick={() => store.setLocale('en')}>EN</button>
<button type="button" aria-pressed={s.locale === 'de'} onClick={() => store.setLocale('de')}>DE</button>
</div>
<IconButton icon={s.theme === 'light' ? 'moon' : 'sun'} label={t('theme')} onClick={() => store.toggleTheme()}/>
</div>
<div className="login-box">
<span className="login-welcome-icon">
<Icon name="inbox" size={26}/>
</span>
<h2>{t('welcome')}</h2>
<p>{t('signIn')}</p>
<form onSubmit={e => {
        e.preventDefault();
        const fd = new FormData(e.currentTarget);
        store.login(String(fd.get('email')), String(fd.get('password')));
    }}>
<Field label={t('email')}>
<input type="email" name="email" autoComplete="username" required autoFocus placeholder="you@company.de"/>
</Field>
<Field label={t('password')}>
<input type="password" name="password" autoComplete="current-password" required/>
</Field>{s.error && <p className="error-message" role="alert">{t('error_' + s.error)}</p>}<button type="submit" className="button primary full" disabled={s.busy}>{t(s.busy ? 'working' : 'signInButton')}<Icon name="arrow"/>
</button>
</form>{s.demo && <div className="demo-accounts">
<span>{t('demoAccounts')}</span>
<div>{[['finance', 'FINANCE'], ['leon', 'APPROVER'], ['admin', 'ADMIN']].map(([user, key]) => <Button key={user} disabled={s.busy} onClick={() => store.loginDemo(user + '@belegflow.demo')}>{t(key)}</Button>)}</div>
<small>{t(s.standalone ? 'standaloneBanner' : 'demoBanner')}</small>
</div>}</div>
</section>
</main>;
}

