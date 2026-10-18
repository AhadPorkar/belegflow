import type { Api, Session } from './models.ts';
export class ApiError extends Error {
    readonly code: string;
    readonly status: number;
    constructor(code: string, status = 0) { super(code); this.code = code; this.status = status; }
}
export class HttpApi implements Api {
    private token: string | null = null;
    private async csrf(): Promise<string> { if (this.token)
        return this.token; const r = await this.request<{
        token: string;
    }>('GET', '/csrf'); this.token = r.token; return this.token; }
    private async request<T>(method: string, path: string, body?: BodyInit, contentType?: string): Promise<T> {
        const headers: Record<string, string> = { Accept: 'application/json' };
        if (contentType)
            headers['Content-Type'] = contentType;
        if (!['GET', 'HEAD', 'OPTIONS'].includes(method))
            headers['X-CSRF-TOKEN'] = await this.csrf();
        let response: Response;
        try {
            response = await fetch('/api' + path, { method, headers, body, credentials: 'same-origin', cache: 'no-store' });
        }
        catch {
            throw new ApiError('NETWORK_ERROR');
        }
        if (response.status === 401) this.token = null;
        const text = await response.text();
        let result: unknown;
        try {
            result = text ? JSON.parse(text) : undefined;
        }
        catch {
            throw new ApiError('UNEXPECTED_ERROR', response.status);
        }
        if (!response.ok) {
            const code = (result as {
                code?: string;
            } | undefined)?.code ?? 'UNEXPECTED_ERROR';
            if (response.status === 401)
                this.token = null;
            throw new ApiError(code, response.status);
        }
        return result as T;
    }
    get<T>(path: string): Promise<T> { return this.request<T>('GET', path); }
    send<T>(method: string, path: string, body?: unknown): Promise<T> { return this.request<T>(method, path, body === undefined ? undefined : JSON.stringify(body), 'application/json'); }
    upload<T>(path: string, body: FormData): Promise<T> { return this.request<T>('POST', path, body); }
    async login(email: string, password: string): Promise<Session> {
        this.token = null;
        await this.request('POST', '/login', new URLSearchParams({ username: email, password }), 'application/x-www-form-urlencoded');
        this.token = null;
        await this.csrf();
        return this.get<Session>('/session');
    }
    async logout(): Promise<void> { await this.send('POST', '/logout'); this.token = null; }
    async download(path: string, filename?: string): Promise<void> {
        let r: Response;
        try {
            r = await fetch('/api' + path, { credentials: 'same-origin', cache: 'no-store' });
        }
        catch {
            throw new ApiError('NETWORK_ERROR');
        }
        if (!r.ok) {
            if (r.status === 401) { this.token = null; throw new ApiError('AUTH_REQUIRED', 401); }
            const fallback = r.status === 403 ? 'FORBIDDEN' : r.status === 404 ? 'NOT_FOUND' : 'UNEXPECTED_ERROR';
            const error = await r.json().catch(() => null) as { code?: string } | null;
            throw new ApiError(error?.code ?? fallback, r.status);
        }
        const disposition = r.headers.get('Content-Disposition') ?? '';
        const encoded = /filename\*=UTF-8''([^;]+)/i.exec(disposition)?.[1];
        const plain = /filename="([^"]+)"/i.exec(disposition)?.[1];
        let name = filename ?? plain ?? 'belegflow-download';
        if (encoded) {
            try {
                name = decodeURIComponent(encoded);
            }
            catch { }
        }
        saveBlob(await r.blob(), name);
    }
}
export function saveBlob(blob: Blob, name: string): void {
    const url = URL.createObjectURL(blob), a = document.createElement('a');
    a.href = url;
    a.download = name;
    a.rel = 'noopener';
    document.body.append(a);
    a.click();
    a.remove();
    setTimeout(() => URL.revokeObjectURL(url), 10000);
}
