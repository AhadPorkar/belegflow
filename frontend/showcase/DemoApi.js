/** Synthetic, browser-local showcase adapter. NOT a security boundary or production API.
 * The production entrypoint imports HttpApi only; this adapter is never auto-selected.
 */
import { ApiError, saveBlob } from '../src/api.ts';
import { csv, decimalCents, validTotals } from '../src/format.ts';
const key = 'bf.showcase.v1';
const uid = () => crypto.randomUUID();
const now = () => new Date().toISOString();
const datePlus = n => { const d = new Date(); d.setDate(d.getDate() + n); return d.toISOString().slice(0, 10); };
const bytes = s => new TextEncoder().encode(s);
const from64 = s => Uint8Array.from(atob(s), c => c.charCodeAt(0));
const to64 = b => { let s = ''; for (let p = 0; p < b.length; p += 8192)
    s += String.fromCharCode(...b.subarray(p, p + 8192)); return btoa(s); };
const sha = async (b) => Array.from(new Uint8Array(await crypto.subtle.digest('SHA-256', b)), x => x.toString(16).padStart(2, '0')).join('');
const xmlEscape = s => String(s).replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;');
const finance = u => ['FINANCE', 'ADMIN'].includes(u.role), approver = u => ['APPROVER', 'ADMIN'].includes(u.role), editable = i => ['RECEIVED', 'REVIEWED', 'CLARIFICATION'].includes(i.stage);
const assert = (condition, code = 'FORBIDDEN') => { if (!condition)
    throw new ApiError(code, code === 'NOT_FOUND' ? 404 : code === 'VERSION_CONFLICT' ? 409 : 422); };
const roles = [['finance', 'Nora Becker', 'FINANCE'], ['leon', 'Leon Fischer', 'APPROVER'], ['admin', 'Alex Weber', 'ADMIN'], ['mira', 'Mira Hoffmann', 'APPROVER'], ['viewer', 'Jan Wagner', 'VIEWER'], ['isolated', 'Isolated Administrator', 'ADMIN']];
function invoiceXml(i) { return `<?xml version="1.0" encoding="UTF-8"?><Invoice xmlns="urn:oasis:names:specification:ubl:schema:xsd:Invoice-2" xmlns:cbc="urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2" xmlns:cac="urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2"><cbc:ID>${xmlEscape(i.invoice_number)}</cbc:ID><cbc:IssueDate>${i.issue_date}</cbc:IssueDate><cbc:DueDate>${i.due_date}</cbc:DueDate><cbc:InvoiceTypeCode>380</cbc:InvoiceTypeCode><cbc:Note>${xmlEscape(i.description)}</cbc:Note><cbc:DocumentCurrencyCode>EUR</cbc:DocumentCurrencyCode><cac:AccountingSupplierParty><cac:Party><cac:PartyLegalEntity><cbc:RegistrationName>${xmlEscape(i.supplier_name)}</cbc:RegistrationName></cac:PartyLegalEntity></cac:Party></cac:AccountingSupplierParty><cac:TaxTotal><cbc:TaxAmount currencyID="EUR">${i.tax}</cbc:TaxAmount></cac:TaxTotal><cac:LegalMonetaryTotal><cbc:TaxExclusiveAmount currencyID="EUR">${i.net}</cbc:TaxExclusiveAmount><cbc:TaxInclusiveAmount currencyID="EUR">${i.gross}</cbc:TaxInclusiveAmount><cbc:PayableAmount currencyID="EUR">${i.gross}</cbc:PayableAmount></cac:LegalMonetaryTotal></Invoice>`; }
export async function seed() {
    const users = roles.map(([email, display_name, role], n) => ({ id: `user-${n + 1}`, display_name, email: email + '@belegflow.demo', role, active: true, locale: 'en', tenant_id: n === 5 ? 'tenant-two' : 'tenant-one' }));
    const suppliers = [['K\u00f6hler Elektrotechnik', 'office@koehler.example', 'DE-DEMO-001'], ['Studio Formwerk', 'hello@formwerk.example', 'DE-DEMO-002'], ['Hafen IT Services', 'invoice@hafen.example', 'DE-DEMO-003'], ['Licht & Raum', 'office@lichtraum.example', 'DE-DEMO-004'], ['Albrecht Consulting', 'team@albrecht.example', 'DE-DEMO-005'], ['Nordwerk Service', 'service@nordwerk.example', 'DE-DEMO-006']].map(([name, email, vat_id], n) => ({ id: `supplier-${n + 1}`, name, email, vat_id, active: true }));
    const projects = [{ id: 'project-1', code: 'BER-2401', name: 'Berlin office expansion', manager_id: 'user-2', manager_name: 'Leon Fischer', evidence_required: true, active: true }, { id: 'project-2', code: 'HAM-2402', name: 'Hamburg facility services', manager_id: 'user-4', manager_name: 'Mira Hoffmann', evidence_required: true, active: true }, { id: 'project-3', code: 'IT-2403', name: 'Digital workplace', manager_id: 'user-2', manager_name: 'Leon Fischer', evidence_required: false, active: true }];
    const cost_centers = [{ id: 'cost-1', code: 'CC-4100', name: 'Facility operations', active: true }, { id: 'cost-2', code: 'CC-4200', name: 'Technical services', active: true }, { id: 'cost-3', code: 'CC-5300', name: 'IT & infrastructure', active: true }];
    const specs = [['KE-2026-1048', 0, 0, 2480, 'REVIEWED', 'Electrical commissioning and service handover'], ['FW-2026-0831', 1, 0, 1650, 'READY_FOR_APPROVAL', 'Workspace planning and technical drawings'], ['HIT-2026-2390', 2, 2, 4200, 'APPROVED', 'Managed network services'], ['LR-2026-0472', 3, 0, 860, 'CLARIFICATION', 'Lighting installation, meeting rooms'], ['AC-2026-0195', 4, 1, 3600, 'RECEIVED', 'Technical project consulting'], ['NW-2026-1066', 5, 1, 1250, 'REVIEWED', 'Quarterly maintenance service'], ['KE-2026-1012', 0, 0, 14800, 'READY_FOR_APPROVAL', 'Electrical infrastructure modernization'], ['HIT-2026-2341', 2, 2, 780, 'REJECTED', 'Network equipment service'], ['FW-2026-0790', 1, 2, 540, 'APPROVED', 'Workspace documentation'], ['LR-2026-0472', 3, 0, 860, 'RECEIVED', 'Lighting installation, duplicate copy'], ['NW-2026-0998', 5, 1, 2840, 'REVIEWED', 'Heating and ventilation maintenance'], ['AC-2026-0180', 4, 2, 1900, 'RECEIVED', 'Infrastructure assessment']];
    const data = { version: 1, users, catalog: { suppliers, projects, cost_centers, users: users.slice(0, 5), delegations: [], threshold: '10000.00' }, invoices: [], docs: {}, exports: [], audit: [], notifications: [], session: null };
    for (let n = 0; n < specs.length; n++) {
        const [invoice_number, s, p, amount, stage, description] = specs[n], supplier = suppliers[s], project = projects[p], cost = cost_centers[p], created = new Date(Date.now() - n * 2700000 - 7200000).toISOString();
        const i = { id: 'invoice-' + (n + 1), tenant_id: 'tenant-one', supplier_id: supplier.id, supplier_name: supplier.name, supplier_email: supplier.email, supplier_vat_id: supplier.vat_id, invoice_number, description, issue_date: datePlus(-7 - n), due_date: datePlus(n - 2), net: amount.toFixed(2), tax: (amount * .19).toFixed(2), gross: (amount * 1.19).toFixed(2), currency: 'EUR', source_format: 'UBL', basic_valid: true, validation_notes: '', project_id: project.id, project_name: project.name, project_code: project.code, project_manager_id: project.manager_id, cost_center_id: cost.id, cost_center_name: cost.name, cost_center_code: cost.code, approver_id: project.manager_id, approver_name: project.manager_name, evidence_required: project.evidence_required, evidence_accepted_by: n === 1 || n === 6 ? project.manager_id : null, evidence_accepted_name: n === 1 || n === 6 ? project.manager_name : null, evidence_accepted_at: n === 1 || n === 6 ? created : null, evidence_note: 'Synthetic handover evidence.', stage, duplicate_warning: n === 9, duplicate_cleared: false, second_required: amount * 1.19 >= 10000, first_approved_by: ['APPROVED'].includes(stage) || n === 6 ? 'user-2' : null, second_approved_by: null, created_by: 'user-1', creator_name: 'Nora Becker', received_at: created, updated_at: created, version: 0, business_key: supplier.id + invoice_number.replaceAll('-', ''), documents: [], discussion: [], audit: [], possible_duplicates: [], open_questions: 0 };
        const original = bytes(invoiceXml(i)), doc = { id: uid(), kind: 'ORIGINAL', filename: invoice_number + '.xml', content_type: 'application/xml', sha256: await sha(original), size_bytes: original.length, created_at: created };
        data.docs[doc.id] = { ...doc, data: to64(original), invoice_id: i.id };
        i.documents.push(doc);
        if (n === 0 || n === 1 || n === 6) {
            const body = bytes('SYNTHETIC SERVICE EVIDENCE\n' + description + '\nProject: ' + project.code + '\nNot a statutory inspection certificate.'), e = { id: uid(), kind: 'EVIDENCE', filename: 'Service-handover-' + invoice_number + '.txt', content_type: 'text/plain', sha256: await sha(body), size_bytes: body.length, created_at: created };
            data.docs[e.id] = { ...e, data: to64(body), invoice_id: i.id };
            i.documents.push(e);
        }
        const events = [['INVOICE_RECEIVED', 'Original XML preserved.'], ['INVOICE_REVIEWED', 'Arithmetic and required header fields checked.']];
        if (n === 0)
            events.push(['ALLOCATION_UPDATED', 'Berlin office expansion / CC-4100'], ['EVIDENCE_UPLOADED', 'Service handover attached.'], ['INVOICE_REVIEWED', 'Ready for project manager evidence review.']);
        if (n === 3) {
            i.discussion.push({ id: uid(), body: 'Please confirm whether the installation included both meeting rooms.', author_name: 'Leon Fischer', is_question: true, resolved_at: null, resolution: null, created_at: created });
            i.open_questions = 1;
            events.push(['CLARIFICATION_REQUESTED', 'Please confirm the completed installation scope.']);
        }
        for (const [event_type, detail] of events) {
            const event = { id: data.audit.length + 1, invoice_id: i.id, invoice_number, actor_name: 'Nora Becker', event_type, detail, created_at: created };
            i.audit.unshift(event);
            data.audit.unshift(event);
        }
        data.invoices.push(i);
    }
    return data;
}
// ZIP (store method), UTF-8 file names, CRC-32; no compression library or network required.
export function makeZip(entries) {
    const table = Array.from({ length: 256 }, (_, n) => { for (let k = 0; k < 8; k++)
        n = (n & 1) ? 0xedb88320 ^ (n >>> 1) : n >>> 1; return n >>> 0; });
    const crc = b => { let c = 0xffffffff; for (const v of b)
        c = table[(c ^ v) & 255] ^ (c >>> 8); return (c ^ 0xffffffff) >>> 0; };
    const chunks = [], central = [];
    let offset = 0, centralSize = 0;
    for (const [name, body] of entries) {
        const nameBytes = bytes(name), data = body instanceof Uint8Array ? body : bytes(body), check = crc(data), local = new Uint8Array(30 + nameBytes.length), v = new DataView(local.buffer);
        v.setUint32(0, 0x04034b50, true);
        v.setUint16(4, 20, true);
        v.setUint16(6, 0x800, true);
        v.setUint32(14, check, true);
        v.setUint32(18, data.length, true);
        v.setUint32(22, data.length, true);
        v.setUint16(26, nameBytes.length, true);
        local.set(nameBytes, 30);
        chunks.push(local, data);
        const head = new Uint8Array(46 + nameBytes.length), h = new DataView(head.buffer);
        h.setUint32(0, 0x02014b50, true);
        h.setUint16(4, 20, true);
        h.setUint16(6, 20, true);
        h.setUint16(8, 0x800, true);
        h.setUint32(16, check, true);
        h.setUint32(20, data.length, true);
        h.setUint32(24, data.length, true);
        h.setUint16(28, nameBytes.length, true);
        h.setUint32(42, offset, true);
        head.set(nameBytes, 46);
        central.push(head);
        centralSize += head.length;
        offset += local.length + data.length;
    }
    const end = new Uint8Array(22), e = new DataView(end.buffer);
    e.setUint32(0, 0x06054b50, true);
    e.setUint16(8, entries.length, true);
    e.setUint16(10, entries.length, true);
    e.setUint32(12, centralSize, true);
    e.setUint32(16, offset, true);
    const all = [...chunks, ...central, end], result = new Uint8Array(all.reduce((n, b) => n + b.length, 0));
    let p = 0;
    for (const b of all) {
        result.set(b, p);
        p += b.length;
    }
    return result;
}
export class DemoApi {
    constructor() { this.data = null; this.email = null; }
    async init() { try {
        const d = JSON.parse(localStorage.getItem(key) || 'null');
        if (d?.version === 1)
            this.data = d;
    }
    catch { } if (!this.data) {
        this.data = await seed();
        this.save();
    } this.email = this.data.session; return this; }
    save() { this.data.session = this.email; try {
        localStorage.setItem(key, JSON.stringify(this.data));
    }
    catch {
        throw new ApiError('STORAGE_FULL');
    } }
    me() { const u = this.data.users.find(u => u.email === this.email && u.active); if (!u)
        throw new ApiError('AUTH_REQUIRED', 401); return u; }
    session() { const u = this.me(); return { id: u.id, tenant_id: u.tenant_id, name: u.display_name, email: u.email, role: u.role, locale: u.locale ?? 'en', threshold: this.data.catalog.threshold, organization: u.tenant_id === 'tenant-two' ? 'Elbe Technik (demo)' : 'Nordlicht Services GmbH (demo)', demo: true }; }
    async login(email, password) { assert(password === 'BelegFlow-Demo-2026!', 'BAD_CREDENTIALS'); const u = this.data.users.find(u => u.email === email && u.active); assert(u, 'BAD_CREDENTIALS'); this.email = email; this.save(); return this.session(); }
    async logout() { this.email = null; this.save(); }
    scoped() { return this.data.invoices.filter(i => i.tenant_id === this.me().tenant_id); }
    invoice(id) { const i = this.scoped().find(i => i.id === id); assert(i, 'NOT_FOUND'); return i; }
    delegate(owner) { const u = this.me(), at = Date.now(); return this.data.catalog.delegations.some(d => d.active && d.owner_id === owner && d.delegate_id === u.id && Date.parse(d.valid_from) <= at && Date.parse(d.valid_until) > at); }
    event(i, type, detail = '') { const a = { id: this.data.audit.reduce((m, a) => Math.max(m, a.id), 0) + 1, invoice_id: i?.id ?? null, invoice_number: i?.invoice_number ?? null, actor_name: this.me().display_name, event_type: type, detail, created_at: now(), tenant_id: this.me().tenant_id }; this.data.audit.unshift(a); if (i) {
        i.audit.unshift(a);
        i.updated_at = now();
        i.version++;
    } }
    readiness(i) { assert(i.basic_valid, 'VALIDATION_FAILED'); assert(i.project_id && i.cost_center_id && i.approver_id, 'ALLOCATION_REQUIRED'); assert(!i.duplicate_warning || i.duplicate_cleared, 'DUPLICATE_UNRESOLVED'); assert(!i.open_questions, 'OPEN_QUESTIONS'); assert(!i.evidence_required || i.evidence_accepted_by, 'EVIDENCE_REQUIRED'); }
    decision(i, action, note = 'preview') { const u = this.me(), assigned = i.approver_id === u.id || this.delegate(i.approver_id); switch (action) {
        case 'REVIEW':
            assert(finance(u));
            assert(i.stage === 'RECEIVED', 'INVALID_TRANSITION');
            return 'REVIEWED';
        case 'CLARIFY':
            assert(finance(u) || (approver(u) && assigned));
            assert(editable(i) || i.stage === 'READY_FOR_APPROVAL', 'INVALID_TRANSITION');
            assert(note.trim(), 'NOTE_REQUIRED');
            return 'CLARIFICATION';
        case 'RESOLVE':
            assert(finance(u));
            assert(i.stage === 'CLARIFICATION', 'INVALID_TRANSITION');
            assert(!i.open_questions, 'OPEN_QUESTIONS');
            return 'REVIEWED';
        case 'SUBMIT':
            assert(finance(u));
            assert(i.stage === 'REVIEWED', 'INVALID_TRANSITION');
            this.readiness(i);
            assert(i.approver_id !== i.created_by, 'SELF_APPROVAL');
            return 'READY_FOR_APPROVAL';
        case 'APPROVE':
            assert(i.stage === 'READY_FOR_APPROVAL', 'INVALID_TRANSITION');
            assert(approver(u));
            assert(i.created_by !== u.id, 'SELF_APPROVAL');
            this.readiness(i);
            if (!i.first_approved_by) {
                assert(assigned, 'NOT_ASSIGNED');
                return i.second_required ? 'READY_FOR_APPROVAL' : 'APPROVED';
            }
            assert(i.second_required && !i.second_approved_by, 'INVALID_TRANSITION');
            assert(u.role === 'ADMIN', 'SECOND_APPROVAL_ADMIN_ONLY');
            assert(i.first_approved_by !== u.id, 'DISTINCT_APPROVERS_REQUIRED');
            return 'APPROVED';
        case 'REJECT':
            assert(finance(u) || (approver(u) && assigned));
            assert(editable(i) || i.stage === 'READY_FOR_APPROVAL', 'INVALID_TRANSITION');
            assert(note.trim(), 'NOTE_REQUIRED');
            return 'REJECTED';
        case 'REOPEN':
            assert(finance(u));
            assert(i.stage === 'REJECTED', 'INVALID_TRANSITION');
            assert(note.trim(), 'NOTE_REQUIRED');
            return 'REVIEWED';
        case 'EXPORT':
            assert(finance(u));
            assert(i.stage === 'APPROVED' && i.first_approved_by, 'EXPORT_REQUIRES_APPROVAL');
            assert(!i.second_required || i.second_approved_by, 'SECOND_APPROVAL_REQUIRED');
            return 'EXPORTED';
        default: throw new ApiError('INVALID_INPUT');
    } }
    detail(i) { const u = this.me(), actions = ['REVIEW', 'CLARIFY', 'RESOLVE', 'SUBMIT', 'APPROVE', 'REJECT', 'REOPEN', 'EXPORT'].filter(a => { try {
        this.decision(i, a);
        return true;
    }
    catch {
        return false;
    } }); return { ...i, allowed_actions: actions, can_edit: finance(u) && editable(i), can_upload_evidence: editable(i) && (finance(u) || (approver(u) && (i.approver_id === u.id || this.delegate(i.approver_id)))), can_accept_evidence: editable(i) && approver(u) && (i.project_manager_id === u.id || this.delegate(i.project_manager_id)) && i.created_by !== u.id && i.documents.some(d => d.kind === 'EVIDENCE'), possible_duplicates: this.scoped().filter(x => x.id !== i.id && x.business_key === i.business_key) }; }
    async get(path) {
        if (path === '/public/config')
            return { demo: true, standalone: true };
        if (path === '/session')
            return this.session();
        const u = this.me(), url = new URL(path, 'https://showcase.invalid'), p = url.pathname, q = url.searchParams, all = this.scoped();
        let result;
        if (p === '/catalog')
            result = u.tenant_id === 'tenant-two' ? { suppliers: [], projects: [], cost_centers: [], users: [u], delegations: [], threshold: '10000.00' } : this.data.catalog;
        else if (p === '/invoices/summary') {
            const subset = stages => all.filter(i => stages.includes(i.stage)), sum = a => (a.reduce((n, i) => n + decimalCents(i.gross), 0n)).toString(), amount = a => { const s = sum(a); return s.slice(0, -2).padStart(1, '0') + '.' + s.slice(-2).padStart(2, '0'); };
            result = { total: all.length, review: subset(['RECEIVED', 'REVIEWED', 'CLARIFICATION']).length, awaiting: subset(['READY_FOR_APPROVAL']).length, approved: subset(['APPROVED']).length, exported: subset(['EXPORTED']).length, overdue: all.filter(i => !['EXPORTED', 'REJECTED'].includes(i.stage) && i.due_date < datePlus(0)).length, open_value: amount(all.filter(i => !['EXPORTED', 'REJECTED'].includes(i.stage))), approved_value: amount(subset(['APPROVED'])) };
        }
        else if (p === '/invoices') {
            const search = (q.get('q') || '').toLowerCase(), stage = q.get('stage'), page = Number(q.get('page') || 0), size = Number(q.get('size') || 50), filtered = all.filter(i => (!stage || i.stage === stage) && [i.supplier_name, i.invoice_number, i.description].join(' ').toLowerCase().includes(search));
            result = { items: filtered.slice(page * size, (page + 1) * size), total: filtered.length, page, size };
        }
        else if (p.startsWith('/invoices/'))
            result = this.detail(this.invoice(p.split('/')[2]));
        else if (p === '/exports') {
            assert(finance(u));
            result = this.data.exports.filter(x => x.tenant_id === u.tenant_id).map(({ data, ...x }) => x);
        }
        else if (p === '/audit') {
            const before = Number(q.get('before') || Infinity), events = this.data.audit.filter(a => (a.tenant_id === u.tenant_id || (!a.tenant_id && u.tenant_id === 'tenant-one')) && a.id < before).slice(0, 100);
            result = { items: events, next: events.length === 100 ? events.at(-1).id : 0 };
        }
        else if (p === '/notifications')
            result = this.data.notifications.filter(n => n.user_id === u.id);
        else
            throw new ApiError('NOT_FOUND', 404);
        return structuredClone(result);
    }
    async send(method, path, body = {}) {
        const u = this.me(), b = body ?? {}, parts = path.split('/');
        let result = {};
        if (path === '/demo/reset') {
            this.data = await seed();
            this.email = 'finance@belegflow.demo';
        }
        else if (path === '/session/locale') {
            u.locale = b.locale === 'de' ? 'de' : 'en';
        }
        else if (parts[1] === 'invoices') {
            const i = this.invoice(parts[2]), action = parts[3];
            if (['commands', 'allocation', 'evidence-acceptance', 'duplicate-resolution'].includes(action))
                assert(i.version === b.version, 'VERSION_CONFLICT');
            if (action === 'commands') {
                const stage = this.decision(i, b.action, b.note || '');
                if (b.action === 'SUBMIT')
                    i.second_required = decimalCents(i.gross) >= decimalCents(this.data.catalog.threshold);
                if (b.action === 'APPROVE') {
                    if (!i.first_approved_by)
                        i.first_approved_by = u.id;
                    else
                        i.second_approved_by = u.id;
                }
                if (['CLARIFY', 'REOPEN'].includes(b.action)) {
                    i.first_approved_by = null;
                    i.second_approved_by = null;
                }
                if (b.action === 'CLARIFY') {
                    i.discussion.push({ id: uid(), body: b.note, author_name: u.display_name, is_question: true, resolved_at: null, resolution: null, created_at: now() });
                    i.open_questions++;
                }
                i.stage = stage;
                this.event(i, { REVIEW: 'INVOICE_REVIEWED', SUBMIT: 'SUBMITTED_FOR_APPROVAL', APPROVE: stage === 'APPROVED' ? 'INVOICE_APPROVED' : 'FIRST_APPROVAL_RECORDED', CLARIFY: 'CLARIFICATION_REQUESTED', RESOLVE: 'CLARIFICATION_RESOLVED', REJECT: 'INVOICE_REJECTED', REOPEN: 'INVOICE_REOPENED' }[b.action], b.note || '');
            }
            else if (action === 'allocation') {
                assert(finance(u));
                assert(editable(i), 'INVALID_TRANSITION');
                const project = this.data.catalog.projects.find(p => p.id === b.project_id), cost = this.data.catalog.cost_centers.find(c => c.id === b.cost_center_id), owner = this.data.users.find(x => x.id === b.approver_id && x.active && approver(x));
                assert(project && cost && owner, 'ALLOCATION_REQUIRED');
                assert(b.due_date >= i.issue_date, 'INVALID_DUE_DATE');
                if (i.project_id !== project.id) {
                    i.evidence_accepted_by = null;
                    i.evidence_accepted_at = null;
                }
                Object.assign(i, { project_id: project.id, project_name: project.name, project_code: project.code, project_manager_id: project.manager_id, evidence_required: project.evidence_required, cost_center_id: cost.id, cost_center_name: cost.name, cost_center_code: cost.code, approver_id: owner.id, approver_name: owner.display_name, due_date: b.due_date, basic_valid: validTotals(i.net, i.tax, i.gross, i.currency) });
                this.event(i, 'ALLOCATION_UPDATED', project.name + ' / ' + cost.code);
            }
            else if (action === 'evidence-acceptance') {
                assert(this.detail(i).can_accept_evidence, 'EVIDENCE_PERMISSION');
                assert(b.note?.trim(), 'NOTE_REQUIRED');
                Object.assign(i, { evidence_accepted_by: u.id, evidence_accepted_name: u.display_name, evidence_accepted_at: now(), evidence_note: b.note });
                this.event(i, 'SERVICE_EVIDENCE_ACCEPTED', b.note);
            }
            else if (action === 'duplicate-resolution') {
                assert(finance(u) && editable(i));
                assert(b.note?.trim(), 'NOTE_REQUIRED');
                i.duplicate_cleared = true;
                this.event(i, 'DUPLICATE_CLEARED', b.note);
            }
            else if (action === 'comments') {
                assert(u.role !== 'VIEWER');
                assert(b.body?.trim(), 'INVALID_INPUT');
                i.discussion.push({ id: uid(), body: b.body, author_name: u.display_name, is_question: false, resolved_at: null, resolution: null, created_at: now() });
                this.event(i, 'COMMENT_ADDED', b.body);
            }
            else if (action === 'questions') {
                assert(finance(u) && i.stage === 'CLARIFICATION');
                const q = i.discussion.find(c => c.id === parts[4] && c.is_question && !c.resolved_at);
                assert(q, 'NOT_FOUND');
                assert(b.body?.trim(), 'NOTE_REQUIRED');
                q.resolved_at = now();
                q.resolution = b.body;
                i.open_questions--;
                this.event(i, 'QUESTION_RESOLVED', b.body);
            }
            else
                throw new ApiError('NOT_FOUND', 404);
        }
        else if (path === '/exports') {
            assert(finance(u));
            const previous = this.data.exports.find(e => e.key === b.idempotency_key);
            if (previous) {
                assert(previous.request === JSON.stringify(b.invoices), 'IDEMPOTENCY_CONFLICT');
                return { id: previous.id };
            }
            assert(b.invoices?.length > 0 && b.invoices.length <= 20, 'INVALID_INPUT');
            const invoices = b.invoices.map(x => { const i = this.invoice(x.id); assert(i.version === x.version, 'VERSION_CONFLICT'); this.decision(i, 'EXPORT'); return i; }), id = uid(), entries = [], manifest = [];
            for (const i of invoices)
                for (const d of i.documents) {
                    entries.push([`${i.id}/${d.kind.toLowerCase()}/${d.filename}`, from64(this.data.docs[d.id].data)]);
                    manifest.push({ filename: entries.at(-1)[0], sha256: d.sha256 });
                }
            entries.push(['invoices.csv', csv([['Invoice', 'Supplier', 'Net', 'Tax', 'Gross', 'Currency', 'Project', 'Cost center'], ...invoices.map(i => [i.invoice_number, i.supplier_name, i.net, i.tax, i.gross, i.currency, i.project_code, i.cost_center_code])])], ['manifest.json', JSON.stringify({ showcase: true, notDatev: true, documents: manifest }, null, 2)], ['README.txt', 'BROWSER-LOCAL SYNTHETIC SHOWCASE EXPORT. Not a DATEV integration, not a payment instruction, not an accounting certification. No real client data.']);
            const zip = makeZip(entries), record = { id, filename: 'belegflow-showcase-' + id.slice(0, 8) + '.zip', sha256: await sha(zip), created_at: now(), created_by_name: u.display_name, invoice_count: invoices.length, tenant_id: u.tenant_id, data: to64(zip), key: b.idempotency_key, request: JSON.stringify(b.invoices) };
            for (const i of invoices) {
                i.stage = 'EXPORTED';
                this.event(i, 'ACCOUNTING_PACKAGE_CREATED', record.filename);
            }
            this.data.exports.unshift(record);
            result = { id };
        }
        else if (path === '/settings/rules') {
            assert(u.role === 'ADMIN');
            assert(decimalCents(b.threshold) > 0n, 'INVALID_AMOUNT');
            this.data.catalog.threshold = b.threshold;
            this.event(null, 'APPROVAL_RULE_CHANGED', b.threshold);
        }
        else if (parts[1] === 'suppliers') {
            assert(finance(u));
            if (parts[2]) {
                const s = this.data.catalog.suppliers.find(s => s.id === parts[2]);
                assert(s, 'NOT_FOUND');
                Object.assign(s, b);
                for (const i of this.scoped().filter(i => i.supplier_id === s.id))
                    Object.assign(i, { supplier_name: s.name, supplier_email: s.email, supplier_vat_id: s.vat_id });
                result = { id: s.id };
            }
            else {
                const item = { ...b, id: uid(), active: true };
                this.data.catalog.suppliers.push(item);
                result = { id: item.id };
            }
            this.event(null, 'SUPPLIER_SAVED', b.name);
        }
        else if (path === '/projects') {
            assert(finance(u));
            const owner = this.data.users.find(x => x.id === b.manager_id);
            assert(owner, 'NOT_FOUND');
            const item = { ...b, id: uid(), manager_name: owner.display_name, active: true };
            this.data.catalog.projects.push(item);
            this.event(null, 'PROJECT_CREATED', b.name);
            result = { id: item.id };
        }
        else if (path === '/cost-centers') {
            assert(finance(u));
            const item = { ...b, id: uid(), active: true };
            this.data.catalog.cost_centers.push(item);
            this.event(null, 'COST_CENTER_CREATED', b.name);
            result = { id: item.id };
        }
        else if (parts[1] === 'users') {
            assert(u.role === 'ADMIN');
            if (parts[2]) {
                const user = this.data.users.find(x => x.id === parts[2]);
                assert(user, 'NOT_FOUND');
                assert(user.id !== u.id || (b.active && b.role === 'ADMIN'), 'LAST_ADMIN');
                Object.assign(user, { email: b.email, display_name: b.display_name, active: b.active, role: b.role });
                Object.assign(this.data.catalog.users.find(x => x.id === user.id), user);
            }
            else {
                const user = { id: uid(), tenant_id: u.tenant_id, locale: 'en', email: b.email, display_name: b.display_name, role: b.role, active: b.active };
                this.data.users.push(user);
                this.data.catalog.users.push(user);
            }
            this.event(null, 'USER_UPDATED', b.email);
        }
        else if (parts[1] === 'delegations') {
            assert(u.role === 'ADMIN');
            if (method === 'DELETE') {
                const d = this.data.catalog.delegations.find(d => d.id === parts[2]);
                assert(d, 'NOT_FOUND');
                d.active = false;
                this.event(null, 'DELEGATION_REVOKED');
            }
            else {
                assert(b.owner_id !== b.delegate_id && Date.parse(b.valid_until) > Date.parse(b.valid_from), 'INVALID_DELEGATION');
                const item = { ...b, id: uid(), active: true, owner_name: this.data.users.find(x => x.id === b.owner_id)?.display_name, delegate_name: this.data.users.find(x => x.id === b.delegate_id)?.display_name };
                this.data.catalog.delegations.push(item);
                this.event(null, 'DELEGATION_CREATED');
                result = { id: item.id };
            }
        }
        else if (path === '/notifications/run') {
            assert(u.role === 'ADMIN');
            for (const i of this.scoped().filter(i => i.stage === 'READY_FOR_APPROVAL' && i.due_date <= datePlus(3))) {
                const owner = i.first_approved_by ? 'user-3' : i.approver_id;
                if (!this.data.notifications.some(n => n.invoice_id === i.id && n.user_id === owner && n.created_at.slice(0, 10) === datePlus(0)))
                    this.data.notifications.push({ id: uid(), user_id: owner, invoice_id: i.id, invoice_number: i.invoice_number, kind: 'APPROVAL_DUE', is_read: false, created_at: now() });
            }
        }
        else if (parts[1] === 'notifications') {
            const n = this.data.notifications.find(n => n.id === parts[2] && n.user_id === u.id);
            assert(n, 'NOT_FOUND');
            n.is_read = true;
        }
        else
            throw new ApiError('NOT_FOUND', 404);
        this.save();
        return result;
    }
    async upload(path, form) {
        const file = form.get('file');
        assert(file instanceof File && file.size > 0 && file.size <= 10 * 1024 * 1024, 'INVALID_FILE');
        const content = new Uint8Array(await file.arrayBuffer()), hash = await sha(content), parts = path.split('/'), u = this.me();
        if (parts[3] === 'evidence') {
            const i = this.invoice(parts[2]);
            assert(this.detail(i).can_upload_evidence);
            assert(i.version === Number(form.get('version')), 'VERSION_CONFLICT');
            assert(/\.(txt|pdf|png|jpe?g)$/i.test(file.name), 'UNSUPPORTED_FILE');
            const d = { id: uid(), kind: 'EVIDENCE', filename: file.name, content_type: file.type || 'application/octet-stream', sha256: hash, size_bytes: content.length, created_at: now() };
            i.documents.push(d);
            this.data.docs[d.id] = { ...d, invoice_id: i.id, data: to64(content) };
            i.evidence_accepted_by = null;
            i.evidence_accepted_at = null;
            this.event(i, 'EVIDENCE_UPLOADED', file.name);
            this.save();
            return { id: d.id };
        }
        assert(finance(u));
        const previous = this.scoped().find(i => i.documents.some(d => d.kind === 'ORIGINAL' && d.sha256 === hash));
        if (previous)
            return { id: previous.id, reused: true };
        const text = await file.text();
        let number, net, tax, gross, issue, due, supplierName, description = '', source = 'UBL', currency = 'EUR';
        if (file.name.toLowerCase().endsWith('.pdf')) {
            assert(text.startsWith('%PDF-'), 'UNSUPPORTED_FILE');
            number = form.get('invoice_number');
            net = form.get('net');
            tax = form.get('tax');
            gross = form.get('gross');
            issue = form.get('issue_date');
            due = form.get('due_date');
            description = form.get('description');
            source = 'PDF';
        }
        else {
            assert(file.name.toLowerCase().endsWith('.xml') && !/<!DOCTYPE|<!ENTITY/i.test(text), 'INVALID_XML');
            const xml = new DOMParser().parseFromString(text, 'application/xml');
            assert(!xml.getElementsByTagName('parsererror').length, 'INVALID_XML');
            assert(xml.documentElement.localName === 'Invoice' && xml.documentElement.namespaceURI === 'urn:oasis:names:specification:ubl:schema:xsd:Invoice-2', 'UNSUPPORTED_XML');
            const find = name => xml.getElementsByTagNameNS('*', name)[0]?.textContent ?? '';
            number = find('ID');
            net = find('TaxExclusiveAmount');
            tax = find('TaxAmount');
            gross = find('TaxInclusiveAmount');
            issue = find('IssueDate');
            due = find('DueDate');
            currency = find('DocumentCurrencyCode');
            supplierName = find('RegistrationName') || find('Name');
            description = find('Note');
        }
        assert(number && issue && net && tax && gross, 'MISSING_INVOICE_FIELDS');
        let supplier = this.data.catalog.suppliers.find(s => s.id === form.get('supplier_id') || s.name === supplierName);
        if (!supplier && supplierName) {
            supplier = { id: uid(), name: supplierName, email: '', vat_id: '', active: true };
            this.data.catalog.suppliers.push(supplier);
        }
        assert(supplier, 'MISSING_INVOICE_FIELDS');
        const project = this.data.catalog.projects.find(p => p.id === form.get('project_id')), cost = this.data.catalog.cost_centers.find(c => c.id === form.get('cost_center_id')), owner = this.data.users.find(u => u.id === (form.get('approver_id') || project?.manager_id));
        const i = { ...structuredClone(this.data.invoices[0]), id: uid(), tenant_id: u.tenant_id, supplier_id: supplier.id, supplier_name: supplier.name, supplier_email: supplier.email, supplier_vat_id: supplier.vat_id, invoice_number: number, net, tax, gross, currency, issue_date: issue, due_date: due || null, description, source_format: source, basic_valid: validTotals(net, tax, gross, currency) && !!due && due >= issue, validation_notes: '', project_id: project?.id ?? null, project_name: project?.name ?? null, project_code: project?.code ?? null, project_manager_id: project?.manager_id ?? null, cost_center_id: cost?.id ?? null, cost_center_name: cost?.name ?? null, cost_center_code: cost?.code ?? null, approver_id: owner?.id ?? null, approver_name: owner?.display_name ?? null, evidence_required: project?.evidence_required ?? true, evidence_accepted_by: null, evidence_accepted_name: null, evidence_accepted_at: null, evidence_note: '', stage: 'RECEIVED', duplicate_warning: this.scoped().some(x => x.supplier_id === supplier.id && x.invoice_number.replaceAll('-', '') === number.replaceAll('-', '')), duplicate_cleared: false, first_approved_by: null, second_approved_by: null, second_required: decimalCents(gross) >= decimalCents(this.data.catalog.threshold), created_by: u.id, creator_name: u.display_name, received_at: now(), updated_at: now(), version: 0, documents: [], discussion: [], audit: [], open_questions: 0, business_key: supplier.id + number.replaceAll('-', '') };
        const doc = { id: uid(), kind: 'ORIGINAL', filename: file.name, content_type: source === 'PDF' ? 'application/pdf' : 'application/xml', sha256: hash, size_bytes: content.length, created_at: now() };
        i.documents.push(doc);
        this.data.docs[doc.id] = { ...doc, data: to64(content), invoice_id: i.id };
        this.data.invoices.unshift(i);
        this.event(i, 'INVOICE_RECEIVED', file.name);
        this.save();
        return { id: i.id, reused: false };
    }
    async download(path, name) { let data, type; if (path.startsWith('/documents/')) {
        const d = this.data.docs[path.split('/')[2]];
        assert(d, 'NOT_FOUND');
        this.invoice(d.invoice_id);
        data = from64(d.data);
        type = d.content_type;
        name = name || d.filename;
    }
    else {
        assert(finance(this.me()));
        const e = this.data.exports.find(e => e.id === path.split('/')[2] && e.tenant_id === this.me().tenant_id);
        assert(e, 'NOT_FOUND');
        data = from64(e.data);
        type = 'application/zip';
        name = name || e.filename;
    } saveBlob(new Blob([data], { type }), name || 'download'); }
}
