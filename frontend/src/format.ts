/** Currency display is separate from interface language. All business instants use Europe/Berlin. */
export function money(value: string | number, locale = 'en-GB', currency = 'EUR'): string {
    const number = Number(value);
    if (!Number.isFinite(number))
        return '—';
    try {
        return new Intl.NumberFormat(locale, { style: 'currency', currency, minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(number);
    }
    catch {
        return '—';
    }
}
export function date(value: string | null | undefined, locale = 'en-GB'): string {
    if (!value)
        return '—';
    const d = new Date(value.length === 10 ? value + 'T12:00:00Z' : value);
    return Number.isNaN(d.getTime()) ? '—' : new Intl.DateTimeFormat(locale, { day: '2-digit', month: 'short', year: 'numeric', timeZone: 'Europe/Berlin' }).format(d);
}
export function time(value: string, locale = 'en-GB'): string { return new Intl.DateTimeFormat(locale, { hour: '2-digit', minute: '2-digit', timeZone: 'Europe/Berlin' }).format(new Date(value)); }
export function initials(name: string): string { return name.trim().split(/\s+/).filter(Boolean).slice(0, 2).map(p => Array.from(p)[0]).join('').toUpperCase(); }
export function csvCell(value: unknown): string {
    let text = String(value ?? '');
    if (/^[\s]*[=+@-]/.test(text))
        text = "'" + text;
    return '"' + text.replaceAll('"', '""') + '"';
}
export function csv(rows: unknown[][]): string { return '﻿' + rows.map(row => row.map(csvCell).join(';')).join('\r\n') + '\r\n'; }
export function decimalCents(value: string): bigint {
    if (!/^-?\d{1,12}(\.\d{1,2})?$/.test(value))
        throw new Error('INVALID_AMOUNT');
    const negative = value.startsWith('-'), [whole, fraction = ''] = value.replace('-', '').split('.');
    return (BigInt(whole) * 100n + BigInt(fraction.padEnd(2, '0'))) * (negative ? -1n : 1n);
}
export function validTotals(net: string, tax: string, gross: string, currency: string): boolean {
    try {
        return currency === 'EUR' && decimalCents(net) >= 0n && decimalCents(tax) >= 0n && decimalCents(gross) > 0n && decimalCents(net) + decimalCents(tax) === decimalCents(gross);
    }
    catch {
        return false;
    }
}
export function localDateTime(value?: string): string { const d = value ? new Date(value) : new Date(); return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 16); }
