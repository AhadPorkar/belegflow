import type { Api, State, View, Locale, Session, Catalog, Invoice, InvoiceDetail, Page, Summary, ExportPackage, Notification, Audit } from './models.ts';
import { ApiError } from './api.ts';
import { translate } from './i18n.ts';
const emptyCatalog: Catalog = { suppliers: [], projects: [], cost_centers: [], users: [], delegations: [], threshold: '10000.00' };
const emptySummary: Summary = { total: 0, review: 0, awaiting: 0, approved: 0, exported: 0, overdue: 0, open_value: '0.00', approved_value: '0.00' };
function preference(key: string, fallback: string): string { try {
    return localStorage.getItem('bf.ui.' + key) ?? fallback;
}
catch {
    return fallback;
} }
function persist(key: string, value: string): void { try {
    localStorage.setItem('bf.ui.' + key, value);
}
catch { /* Private browsing must not break the app. */ } }
function initial(): State {
    return { ready: false, busy: false, loading: false, error: null, toast: null, locale: preference('locale', 'en') === 'de' ? 'de' : 'en', numberLocale: preference('numbers', 'en-GB') === 'de-DE' ? 'de-DE' : 'en-GB', theme: preference('theme', 'light') === 'dark' ? 'dark' : 'light', me: null, demo: false, standalone: false, view: 'inbox', query: '', stage: '', page: 0, invoices: [], total: 0, summary: emptySummary, catalog: emptyCatalog, selectedId: null, detail: null, documentTab: 'document', activityTab: 'activity', mobilePane: 'queue', exports: [], audit: [], auditNext: 0, notifications: [], dialog: null, dialogFile: null };
}
/** Small immutable external store. React views are pure; API and UI state are independently testable. */
export class WorkspaceStore {
    private api: Api;
    private state: State = initial();
    private listeners = new Set<() => void>();
    private authTicket = 0;
    private selectionTicket = 0;
    private listTicket = 0;
    private searchTimer: ReturnType<typeof setTimeout> | undefined;
    private toastTimer: ReturnType<typeof setTimeout> | undefined;
    constructor(api: Api) { this.api = api; }
    getSnapshot = (): State => this.state;
    subscribe = (listener: () => void): (() => void) => { this.listeners.add(listener); return () => this.listeners.delete(listener); };
    patch(patch: Partial<State>): void {
        this.state = { ...this.state, ...patch };
        if (typeof document !== 'undefined') {
            document.documentElement.lang = this.state.locale;
            document.documentElement.dataset.theme = this.state.theme;
        }
        for (const listener of this.listeners)
            listener();
    }
    t(key: string, variables: Record<string, string | number> = {}): string { return translate(this.state.locale, key, variables); }
    private fail(error: unknown): void {
        const code = error instanceof ApiError ? error.code : error instanceof RangeError ? 'INVALID_INPUT' : 'UNEXPECTED_ERROR';
        this.patch({ error: code, ...(['AUTH_REQUIRED', 'SESSION_REVOKED'].includes(code) ? { me: null, dialog: null } : {}) });
    }
    private toast(key = 'saved'): void { if (this.toastTimer)
        clearTimeout(this.toastTimer); this.patch({ toast: key }); this.toastTimer = setTimeout(() => this.patch({ toast: null }), 5000); }
    async boot(): Promise<void> {
        try {
            const config = await this.api.get<{
                demo: boolean;
                standalone?: boolean;
            }>('/public/config');
            this.patch({ demo: config.demo, standalone: config.standalone ?? false });
            try {
                const me = await this.api.get<Session>('/session');
                this.patch({ me, locale: me.locale === 'de' ? 'de' : 'en' });
                await this.loadAll();
            }
            catch (e) {
                if (!(e instanceof ApiError && e.status === 401))
                    throw e;
            }
        }
        catch (e) {
            this.fail(e);
        }
        finally {
            this.patch({ ready: true });
        }
    }
    async login(email: string, password: string): Promise<void> {
        if (this.state.busy)
            return;
        this.authTicket++;
        this.listTicket++;
        this.selectionTicket++;
        this.patch({ busy: true, error: null, detail: null, invoices: [] });
        try {
            const me = await this.api.login(email, password);
            this.patch({ me, locale: me.locale === 'de' ? 'de' : 'en', dialog: null, view: 'inbox', query: '', stage: '', page: 0, selectedId: null });
            await this.loadAll();
        }
        catch (e) {
            this.fail(e);
        }
        finally {
            this.patch({ busy: false, ready: true });
        }
    }
    async loginDemo(email: string): Promise<void> { if (this.state.demo)
        await this.login(email, 'BelegFlow-Demo-2026!'); }
    async logout(): Promise<void> { this.authTicket++; this.listTicket++; this.selectionTicket++; if (this.searchTimer)
        clearTimeout(this.searchTimer); try {
        await this.api.logout();
        this.patch({ me: null, detail: null, invoices: [], catalog: emptyCatalog, summary: emptySummary, notifications: [], exports: [], audit: [], dialog: null });
    }
    catch (e) {
        this.fail(e);
    } }
    async loadAll(): Promise<void> {
        if (!this.state.me)
            return;
        const ticket = this.authTicket;
        const userId = this.state.me.id;
        const [catalog, summary, notifications] = await Promise.all([this.api.get<Catalog>('/catalog'), this.api.get<Summary>('/invoices/summary'), this.api.get<Notification[]>('/notifications')]);
        if (ticket !== this.authTicket || this.state.me?.id !== userId)
            return;
        this.patch({ catalog, summary, notifications });
        await this.loadView();
    }
    async refresh(): Promise<void> { this.patch({ error: null }); try {
        await this.loadAll();
    }
    catch (e) {
        this.fail(e);
    } }
    async navigate(view: View): Promise<void> {
        this.closeDialog();
        this.patch({ view, page: 0, query: '', stage: view === 'approvals' ? 'READY_FOR_APPROVAL' : '', error: null, mobilePane: 'queue' });
        try {
            await this.loadView();
        }
        catch (e) {
            this.fail(e);
        }
    }
    private async loadView(): Promise<void> {
        const ticket = this.authTicket;
        const view = this.state.view;
        if (view === 'inbox' || view === 'approvals') {
            await this.loadInvoices();
        } else if (view === 'exports') {
            const exports = await this.api.get<ExportPackage[]>('/exports');
            if (ticket === this.authTicket && this.state.view === view && this.state.me) this.patch({ exports });
        } else if (view === 'audit') {
            const data = await this.api.get<{ items: Audit[]; next: number }>('/audit');
            if (ticket === this.authTicket && this.state.view === view && this.state.me) this.patch({ audit: data.items, auditNext: data.next });
        }
    }
    private async loadInvoices(): Promise<void> {
        if (!this.state.me)
            return;
        const ticket = ++this.listTicket;
        this.patch({ loading: true });
        const query = new URLSearchParams({ q: this.state.query, stage: this.state.stage, page: String(this.state.page), size: '50' });
        try {
            const result = await this.api.get<Page<Invoice>>('/invoices?' + query);
            if (ticket !== this.listTicket)
                return;
            let id = this.state.selectedId;
            if (!result.items.some(i => i.id === id))
                id = result.items[0]?.id ?? null;
            this.patch({ invoices: result.items, total: result.total, selectedId: id });
            if (id)
                await this.loadDetail(id);
            else
                this.patch({ detail: null });
        }
        finally {
            if (ticket === this.listTicket)
                this.patch({ loading: false });
        }
    }
    private async loadDetail(id: string): Promise<void> { const ticket = ++this.selectionTicket; const detail = await this.api.get<InvoiceDetail>('/invoices/' + id); if (ticket === this.selectionTicket && this.state.selectedId === id)
        this.patch({ detail }); }
    async selectInvoice(id: string): Promise<void> { this.patch({ selectedId: id, detail: null, mobilePane: 'document', error: null }); try {
        await this.loadDetail(id);
    }
    catch (e) {
        this.fail(e);
    } }
    async openInvoice(id: string): Promise<void> { this.closeDialog(); this.patch({ view: 'inbox', stage: '', query: '', page: 0 }); try {
        await this.loadInvoices();
        await this.selectInvoice(id);
    }
    catch (e) {
        this.fail(e);
    } }
    search(query: string): void { this.patch({ query, page: 0 }); if (this.searchTimer)
        clearTimeout(this.searchTimer); this.searchTimer = setTimeout(() => { void this.loadInvoices().catch(e => this.fail(e)); }, 250); }
    async filter(stage: string): Promise<void> { this.patch({ stage, page: 0 }); try {
        await this.loadInvoices();
    }
    catch (e) {
        this.fail(e);
    } }
    async page(delta: number): Promise<void> { this.patch({ page: Math.max(0, this.state.page + delta) }); try {
        await this.loadInvoices();
    }
    catch (e) {
        this.fail(e);
    } }
    setLocale(locale: Locale): void { this.patch({ locale }); persist('locale', locale); if (this.state.me)
        void this.api.send('PUT', '/session/locale', { locale }).catch(e => this.fail(e)); }
    setNumberLocale(value: 'en-GB' | 'de-DE'): void { this.patch({ numberLocale: value }); persist('numbers', value); }
    toggleTheme(): void { const theme = this.state.theme === 'light' ? 'dark' : 'light'; this.patch({ theme }); persist('theme', theme); }
    openDialog(kind: string, record?: Record<string, unknown>, action?: string, questionId?: string): void {
        const i = this.state.detail;
        this.patch({ dialog: { kind, record, action, questionId, invoiceId: i?.id, version: i?.version }, dialogFile: null, error: null });
    }
    closeDialog(): void { if (typeof document !== 'undefined')
        document.querySelector('dialog')?.close(); this.patch({ dialog: null, dialogFile: null, error: null }); }
    setDialogFile(file: File | undefined): void { this.patch({ dialogFile: file ?? null }); }
    private async mutate(task: () => Promise<void>, message = 'saved'): Promise<boolean> {
        if (this.state.busy)
            return false;
        this.patch({ busy: true, error: null });
        try {
            await task();
            this.closeDialog();
            await this.loadAll();
            this.toast(message);
            return true;
        }
        catch (e) {
            this.fail(e);
            return false;
        }
        finally {
            this.patch({ busy: false });
        }
    }
    async command(action: string): Promise<void> {
        const d = this.state.detail;
        if (!d)
            return;
        if (['APPROVE', 'CLARIFY', 'REJECT', 'REOPEN'].includes(action)) {
            this.openDialog('command', undefined, action);
            return;
        }
        if (action === 'EXPORT') {
            this.openExport([{ id: d.id, version: d.version }]);
            return;
        }
        await this.mutate(async () => { await this.api.send('POST', '/invoices/' + d.id + '/commands', { version: d.version, action, note: '' }); });
    }
    async comment(text: string): Promise<boolean> { const d = this.state.detail; if (!d)
        return false; return this.mutate(async () => { await this.api.send('POST', '/invoices/' + d.id + '/comments', { body: text }); }); }
    async submitDialog(form: HTMLFormElement): Promise<void> {
        const d = this.state.dialog;
        if (!d)
            return;
        const fd = new FormData(form);
        const value = (name: string) => String(fd.get(name) ?? '');
        const maybe = (name: string) => value(name) || null;
        const invoicePath = '/invoices/' + d.invoiceId;
        let downloadId: string | undefined;
        let message = 'saved';
        const success = await this.mutate(async () => {
            switch (d.kind) {
                case 'import': {
                    const file = this.state.dialogFile ?? fd.get('file');
                    if (!(file instanceof File) || !file.size)
                        throw new ApiError('INVALID_FILE');
                    fd.set('file', file);
                    const result = await this.api.upload<{
                        id: string;
                        reused: boolean;
                    }>('/invoices/import', fd);
                    this.patch({ selectedId: result.id, view: 'inbox', query: '', stage: '', page: 0, mobilePane: 'document' });
                    message = result.reused ? 'reused' : 'imported';
                    break;
                }
                case 'evidenceUpload': {
                    const file = this.state.dialogFile ?? fd.get('file');
                    if (!(file instanceof File) || !file.size)
                        throw new ApiError('INVALID_FILE');
                    fd.set('file', file);
                    fd.set('version', String(d.version));
                    await this.api.upload(invoicePath + '/evidence', fd);
                    break;
                }
                case 'allocation':
                    await this.api.send('PUT', invoicePath + '/allocation', { version: d.version, project_id: maybe('project_id'), cost_center_id: maybe('cost_center_id'), approver_id: maybe('approver_id'), due_date: value('due_date') });
                    break;
                case 'command':
                    await this.api.send('POST', invoicePath + '/commands', { version: d.version, action: d.action, note: value('note') });
                    break;
                case 'acceptEvidence':
                    await this.api.send('POST', invoicePath + '/evidence-acceptance', { version: d.version, note: value('note') });
                    break;
                case 'duplicate':
                    await this.api.send('POST', invoicePath + '/duplicate-resolution', { version: d.version, note: value('note') });
                    break;
                case 'resolveQuestion':
                    await this.api.send('POST', invoicePath + '/questions/' + d.questionId + '/resolution', { body: value('note') });
                    break;
                case 'supplier':
                    await this.api.send(d.record?.id ? 'PUT' : 'POST', '/suppliers' + (d.record?.id ? '/' + d.record.id : ''), { name: value('name'), email: value('email'), vat_id: value('vat_id'), active: fd.has('active') });
                    break;
                case 'project':
                    await this.api.send('POST', '/projects', { code: value('code'), name: value('name'), manager_id: value('manager_id'), evidence_required: fd.has('evidence_required') });
                    break;
                case 'costCenter':
                    await this.api.send('POST', '/cost-centers', { code: value('code'), name: value('name') });
                    break;
                case 'user':
                    await this.api.send(d.record?.id ? 'PUT' : 'POST', '/users' + (d.record?.id ? '/' + d.record.id : ''), { email: value('email'), display_name: value('display_name'), role: value('role'), password: value('password'), active: fd.has('active') });
                    break;
                case 'delegation':
                    await this.api.send('POST', '/delegations', { owner_id: value('owner_id'), delegate_id: value('delegate_id'), valid_from: new Date(value('valid_from')).toISOString(), valid_until: new Date(value('valid_until')).toISOString() });
                    break;
                case 'export': {
                    const result = await this.api.send<{
                        id: string;
                    }>('POST', '/exports', { idempotency_key: d.record?.key, invoices: d.record?.invoices });
                    downloadId = result.id;
                    message = 'exportedToast';
                    break;
                }
                case 'reset':
                    if (this.state.standalone) {
                        await this.api.send('POST', '/demo/reset');
                        this.patch({ selectedId: null });
                    }
                    break;
                default: throw new ApiError('INVALID_INPUT');
            }
        }, message);
        if (success) {
            this.toast(message);
            if (downloadId)
                await this.download('/exports/' + downloadId + '/download');
        }
    }
    openExport(invoices: {
        id: string;
        version: number;
    }[]): void { if (!invoices.length)
        return; this.openDialog('export', { invoices, key: crypto.randomUUID() }); }
    async exportApproved(): Promise<void> { try {
        const page = await this.api.get<Page<Invoice>>('/invoices?stage=APPROVED&size=20&page=0&q=');
        this.openExport(page.items.map(i => ({ id: i.id, version: i.version })));
    }
    catch (e) {
        this.fail(e);
    } }
    async saveRules(threshold: string): Promise<void> { await this.mutate(async () => { await this.api.send('PUT', '/settings/rules', { threshold }); }); }
    async revoke(id: string): Promise<void> { await this.mutate(async () => { await this.api.send('DELETE', '/delegations/' + id); }); }
    async runReminders(): Promise<void> { await this.mutate(async () => { await this.api.send('POST', '/notifications/run'); }); }
    async markRead(id: string): Promise<void> { try {
        await this.api.send('POST', '/notifications/' + id + '/read');
        this.patch({ notifications: await this.api.get<Notification[]>('/notifications') });
    }
    catch (e) {
        this.fail(e);
    } }
    async earlierAudit(): Promise<void> { try {
        const r = await this.api.get<{
            items: Audit[];
            next: number;
        }>('/audit?before=' + this.state.auditNext);
        this.patch({ audit: [...this.state.audit, ...r.items], auditNext: r.next });
    }
    catch (e) {
        this.fail(e);
    } }
    async download(path: string, name?: string): Promise<void> { try {
        await this.api.download(path, name);
    }
    catch (e) {
        this.fail(e);
    } }
}
