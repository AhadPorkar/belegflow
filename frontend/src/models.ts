export type Locale = 'en' | 'de';
export type Role = 'ADMIN' | 'FINANCE' | 'APPROVER' | 'VIEWER';
export type Stage = 'RECEIVED' | 'REVIEWED' | 'CLARIFICATION' | 'READY_FOR_APPROVAL' | 'APPROVED' | 'REJECTED' | 'EXPORTED';
export type View = 'inbox' | 'approvals' | 'suppliers' | 'projects' | 'exports' | 'audit' | 'settings';
export interface Session {
    id: string;
    tenant_id: string;
    name: string;
    role: Role;
    organization: string;
    email: string;
    locale: Locale;
    threshold: string;
    demo: boolean;
}
export interface User {
    id: string;
    display_name: string;
    email: string;
    role: Role;
    active: boolean;
}
export interface Supplier {
    id: string;
    name: string;
    email: string;
    vat_id: string;
    active: boolean;
}
export interface Project {
    id: string;
    code: string;
    name: string;
    manager_id: string;
    manager_name: string;
    evidence_required: boolean;
    active: boolean;
}
export interface CostCenter {
    id: string;
    code: string;
    name: string;
    active: boolean;
}
export interface Delegation {
    id: string;
    owner_id: string;
    delegate_id: string;
    owner_name: string;
    delegate_name: string;
    valid_from: string;
    valid_until: string;
    active: boolean;
}
export interface Catalog {
    suppliers: Supplier[];
    projects: Project[];
    cost_centers: CostCenter[];
    users: User[];
    delegations: Delegation[];
    threshold: string;
}
export interface Invoice {
    id: string;
    tenant_id: string;
    supplier_id: string;
    supplier_name: string;
    supplier_email: string;
    supplier_vat_id: string;
    invoice_number: string;
    description: string;
    issue_date: string;
    due_date: string | null;
    net: string;
    tax: string;
    gross: string;
    currency: string;
    source_format: 'UBL' | 'CII' | 'PDF';
    basic_valid: boolean;
    validation_notes: string;
    project_id: string | null;
    project_name: string | null;
    project_code: string | null;
    project_manager_id: string | null;
    cost_center_id: string | null;
    cost_center_name: string | null;
    cost_center_code: string | null;
    approver_id: string | null;
    approver_name: string | null;
    evidence_required: boolean;
    evidence_accepted_by: string | null;
    evidence_accepted_name: string | null;
    evidence_accepted_at: string | null;
    evidence_note: string;
    stage: Stage;
    duplicate_warning: boolean;
    duplicate_cleared: boolean;
    second_required: boolean;
    first_approved_by: string | null;
    second_approved_by: string | null;
    created_by: string;
    creator_name: string;
    received_at: string;
    updated_at: string;
    version: number;
    business_key: string;
}
export interface Doc {
    id: string;
    kind: 'ORIGINAL' | 'EVIDENCE';
    filename: string;
    content_type: string;
    sha256: string;
    size_bytes: number;
    created_at: string;
}
export interface Comment {
    id: string;
    body: string;
    author_name: string;
    is_question: boolean;
    resolved_at: string | null;
    resolution: string | null;
    created_at: string;
}
export interface Audit {
    id: number;
    invoice_id?: string | null;
    invoice_number?: string | null;
    actor_name: string;
    event_type: string;
    detail: string;
    created_at: string;
}
export interface InvoiceDetail extends Invoice {
    documents: Doc[];
    discussion: Comment[];
    audit: Audit[];
    possible_duplicates: Partial<Invoice>[];
    allowed_actions: string[];
    can_edit: boolean;
    can_upload_evidence: boolean;
    can_accept_evidence: boolean;
    open_questions: number;
}
export interface Summary {
    total: number;
    review: number;
    awaiting: number;
    approved: number;
    exported: number;
    overdue: number;
    open_value: string;
    approved_value: string;
}
export interface Page<T> {
    items: T[];
    total: number;
    page: number;
    size: number;
}
export interface ExportPackage {
    id: string;
    filename: string;
    sha256: string;
    created_at: string;
    created_by_name: string;
    invoice_count: number;
}
export interface Notification {
    id: string;
    invoice_id: string;
    invoice_number: string;
    kind: string;
    is_read: boolean;
    created_at: string;
}
export interface Api {
    get<T>(path: string): Promise<T>;
    send<T>(method: string, path: string, body?: unknown): Promise<T>;
    upload<T>(path: string, body: FormData): Promise<T>;
    download(path: string, filename?: string): Promise<void>;
    login(email: string, password: string): Promise<Session>;
    logout(): Promise<void>;
}
export interface DialogState {
    kind: string;
    record?: Record<string, unknown>;
    invoiceId?: string;
    version?: number;
    action?: string;
    questionId?: string;
}
export interface State {
    ready: boolean;
    busy: boolean;
    loading: boolean;
    error: string | null;
    toast: string | null;
    locale: Locale;
    numberLocale: 'en-GB' | 'de-DE';
    theme: 'light' | 'dark';
    me: Session | null;
    demo: boolean;
    standalone: boolean;
    view: View;
    query: string;
    stage: string;
    page: number;
    invoices: Invoice[];
    total: number;
    summary: Summary;
    catalog: Catalog;
    selectedId: string | null;
    detail: InvoiceDetail | null;
    documentTab: 'document' | 'evidence';
    activityTab: 'activity' | 'discussion';
    mobilePane: 'queue' | 'document' | 'decision';
    exports: ExportPackage[];
    audit: Audit[];
    auditNext: number;
    notifications: Notification[];
    dialog: DialogState | null;
    dialogFile: File | null;
}
