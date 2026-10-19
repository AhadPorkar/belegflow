import React from 'react';
import { money, date, time, initials, localDateTime } from '../format.ts';

const iconPaths = {
    inbox: ['M3 4h18v16H3z', 'M3 13h5l2 3h4l2-3h5'], check: ['M5 12l4 4L19 6'], arrow: ['M5 12h14', 'm13 6 6 6-6 6'], chevron: ['m9 5 7 7-7 7'], down: ['m6 9 6 6 6-6'],
    file: ['M14 2H5v20h14V7z', 'M14 2v6h5', 'M8 12h8M8 16h6'], search: ['M21 21l-5-5', 'M10 18a8 8 0 1 0 0-16 8 8 0 0 0 0 16'], plus: ['M12 5v14M5 12h14'], close: ['m6 6 12 12M6 18 18 6'],
    building: ['M4 21V4h12v17M16 10h4v11M8 8h4M8 12h4M8 16h4M2 21h20'], folder: ['M3 5h7l2 3h9v12H3z'], download: ['M12 3v12m-5-5 5 5 5-5', 'M4 16v5h16v-5'], upload: ['M12 16V4m-5 5 5-5 5 5', 'M4 17v4h16v-4'],
    clock: ['M12 8v5l3 2', 'M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20'], shield: ['m12 2 8 3v7c0 5-8 10-8 10S4 17 4 12V5z', 'm8 12 3 3 5-6'], list: ['M8 6h13M8 12h13M8 18h13M3 6h1M3 12h1M3 18h1'],
    settings: ['M12 8a4 4 0 1 0 0 8 4 4 0 0 0 0-8', 'm10 2-1 3-3 1-3-1-2 4 2 2v3l-2 2 2 4 3-1 3 1 1 3h4l1-3 3-1 3 1 2-4-2-2v-3l2-2-2-4-3 1-3-1-1-3z'],
    bell: ['M5 16h14l-2-3V8a5 5 0 0 0-10 0v5z', 'M10 20h4'], moon: ['M20 15A9 9 0 0 1 9 3 9 9 0 1 0 20 15'], sun: ['M12 8a4 4 0 1 0 0 8 4 4 0 0 0 0-8', 'M12 2v2M12 20v2M2 12h2M20 12h2m-2-8-2 2M6 18l-2 2M4 4l2 2m12 12 2 2'],
    help: ['M9 9a3 3 0 0 1 6 0c0 2-3 2-3 5', 'M12 17h.01', 'M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20'], logout: ['M9 3H3v18h6M9 12h12m-5-5 5 5-5 5'], lock: ['M5 10h14v12H5z', 'M8 10V6a4 4 0 0 1 8 0v4'], info: ['M12 11v6M12 7h.01', 'M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20'],
    alert: ['m12 3 10 18H2z', 'M12 9v5M12 17h.01'], edit: ['m16 3 5 5-12 12-6 1 1-6z', 'm13 6 5 5'], send: ['m22 2-7 20-4-9-9-4z', 'M11 13 22 2'], link: ['m10 13 4-4', 'M8 16l-2 2a4 4 0 0 1-6-6l5-5a4 4 0 0 1 6 0', 'M16 8l2-2a4 4 0 0 1 6 6l-5 5a4 4 0 0 1-6 0'],
    users: ['M16 21v-3a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v3', 'M9 3a4 4 0 1 0 0 8 4 4 0 0 0 0-8', 'M17 4a4 4 0 0 1 0 7M22 21v-3a4 4 0 0 0-3-4'], grid: ['M3 3h7v7H3zM14 3h7v7h-7zM3 14h7v7H3zM14 14h7v7h-7z'],
    refresh: ['M20 7v5h-5', 'M4 17v-5h5', 'M5 7a8 8 0 0 1 13-2l2 2M4 17l2 2a8 8 0 0 0 13-2'], spark: ['m12 2 3 7 7 3-7 3-3 7-3-7-7-3 7-3z'], paperclip: ['m8 12 6-6a4 4 0 0 1 6 6l-9 9a6 6 0 0 1-9-9L13 1', 'm5 15 8-8a2 2 0 0 1 3 3l-8 8'], euro: ['M18 5a7 7 0 0 0-12 7 7 7 0 0 0 12 7', 'M3 10h12M3 14h11'],
};

export function Icon({ name, size = 18, ...rest }) {
    return <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" {...rest}>{(iconPaths[name] ?? iconPaths.file).map((d, i) => <path key={i} d={d}/>)}</svg>;
}

export function Logo() {
    return <span className="brand-icon" aria-hidden="true">
<svg viewBox="0 0 40 40">
<path d="M11 9h10a7 7 0 0 1 5 12 6 6 0 0 1-5 10H11V9zm6 5v7h5a3.5 3.5 0 0 0 0-7h-5zm0 11v2h5a2 2 0 0 0 0-4h-5z" fill="currentColor"/>
<circle cx="30" cy="10" r="3" fill="#bfb1ff"/>
</svg>
</span>;
}

export function Button({ icon, children, tone = 'secondary', className = '', ...props }) {
    return <button type="button" className={'button ' + tone + ' ' + className} {...props}>{icon && <Icon name={icon}/>}<span>{children}</span>
</button>;
}

export function IconButton({ icon, label, ...props }) {
    return <button type="button" className="icon-button" title={label} aria-label={label} {...props}>
<Icon name={icon}/>
</button>;
}

export function Badge({ stage, t }) {
    return <span className={'status-badge status-' + stage}>
<span className="status-dot"/>{t(stage)}</span>;
}

export function Avatar({ name, tone = 0, className = '' }) {
    return <span className={'avatar avatar-' + tone + ' ' + className}>{initials(name ?? '')}</span>;
}

export function Empty({ icon = 'inbox', title, body, children }) {
    return <div className="empty-state">
<span className="empty-icon">
<Icon name={icon} size={28}/>
</span>
<h3>{title}</h3>{body && <p>{body}</p>}{children}</div>;
}

/** A single labelled control. Pass a page-unique id for an explicit label and hint relationship. */
export function Field({ label, children, hint, className = '', id }) {
    if (id) {
        const control = React.Children.only(children);
        const hintId = hint ? id + '-hint' : undefined;
        const describedBy = [control.props['aria-describedby'], hintId].filter(Boolean).join(' ') || undefined;
        return <div className={'field ' + className}>
<label className="field-label" htmlFor={id}>{label}</label>
{React.cloneElement(control, { id, 'aria-describedby': describedBy })}
{hint && <span className="field-hint" id={hintId}>{hint}</span>}
</div>;
    }
    return <label className={'field ' + className}>
<span className="field-label">{label}</span>{children}{hint && <span className="field-hint">{hint}</span>}</label>;
}

export const finance = me => ['ADMIN', 'FINANCE'].includes(me?.role);

export function PageHeading({ title, subtitle, children }) {
    return <div className="page-heading">
<div>
<h1>{title}</h1>
<p>{subtitle}</p>
</div>
<div className="heading-actions">{children}</div>
</div>;
}

