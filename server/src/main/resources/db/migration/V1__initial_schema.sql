-- PostgreSQL only. Composite foreign keys make cross-organization references invalid.
CREATE TABLE organization (
 id uuid PRIMARY KEY, name varchar(200) NOT NULL,
 second_approval_threshold numeric(14,2) NOT NULL DEFAULT 10000 CHECK (second_approval_threshold > 0),
 created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE app_user (
 id uuid PRIMARY KEY, tenant_id uuid NOT NULL REFERENCES organization(id),
 email varchar(254) NOT NULL UNIQUE, display_name varchar(120) NOT NULL,
 password_hash varchar(500) NOT NULL, role varchar(20) NOT NULL CHECK (role IN ('ADMIN','FINANCE','APPROVER','VIEWER')),
 auth_version integer NOT NULL DEFAULT 0, active boolean NOT NULL DEFAULT true, locale varchar(2) NOT NULL DEFAULT 'en' CHECK (locale IN ('en','de')),
 created_at timestamptz NOT NULL DEFAULT now(), UNIQUE (tenant_id,id),
 CHECK (email = lower(email))
);
CREATE INDEX app_user_tenant ON app_user (tenant_id,active);
CREATE TABLE supplier (
 id uuid PRIMARY KEY, tenant_id uuid NOT NULL REFERENCES organization(id),
 name varchar(200) NOT NULL, normalized_name varchar(300) NOT NULL,
 email varchar(254) NOT NULL DEFAULT '', vat_id varchar(40) NOT NULL DEFAULT '',
 active boolean NOT NULL DEFAULT true, created_at timestamptz NOT NULL DEFAULT now(),
 UNIQUE (tenant_id,id), UNIQUE (tenant_id,normalized_name)
);
CREATE TABLE project (
 id uuid PRIMARY KEY, tenant_id uuid NOT NULL REFERENCES organization(id),
 code varchar(40) NOT NULL, name varchar(160) NOT NULL, manager_id uuid NOT NULL,
 evidence_required boolean NOT NULL DEFAULT true, active boolean NOT NULL DEFAULT true,
 UNIQUE (tenant_id,id), UNIQUE (tenant_id,code),
 FOREIGN KEY (tenant_id,manager_id) REFERENCES app_user(tenant_id,id)
);
CREATE TABLE cost_center (
 id uuid PRIMARY KEY, tenant_id uuid NOT NULL REFERENCES organization(id),
 code varchar(40) NOT NULL, name varchar(160) NOT NULL, active boolean NOT NULL DEFAULT true,
 UNIQUE (tenant_id,id), UNIQUE (tenant_id,code)
);
CREATE TABLE delegation (
 id uuid PRIMARY KEY, tenant_id uuid NOT NULL REFERENCES organization(id),
 owner_id uuid NOT NULL, delegate_id uuid NOT NULL,
 valid_from timestamptz NOT NULL, valid_until timestamptz NOT NULL, active boolean NOT NULL DEFAULT true,
 CHECK (owner_id <> delegate_id), CHECK (valid_until > valid_from),
 FOREIGN KEY (tenant_id,owner_id) REFERENCES app_user(tenant_id,id),
 FOREIGN KEY (tenant_id,delegate_id) REFERENCES app_user(tenant_id,id)
);
CREATE INDEX delegation_lookup ON delegation(tenant_id,owner_id,delegate_id,valid_until) WHERE active;
CREATE TABLE invoice (
 id uuid PRIMARY KEY, tenant_id uuid NOT NULL REFERENCES organization(id), supplier_id uuid NOT NULL,
 invoice_number varchar(120) NOT NULL, description varchar(2000) NOT NULL DEFAULT '',
 issue_date date NOT NULL, due_date date, net numeric(14,2) NOT NULL, tax numeric(14,2) NOT NULL, gross numeric(14,2) NOT NULL,
 currency varchar(3) NOT NULL DEFAULT 'EUR', source_format varchar(10) NOT NULL CHECK (source_format IN ('UBL','CII','PDF')),
 basic_valid boolean NOT NULL DEFAULT false, validation_notes varchar(2000) NOT NULL DEFAULT '',
 project_id uuid, cost_center_id uuid, approver_id uuid, evidence_required boolean NOT NULL DEFAULT true,
 evidence_accepted_by uuid, evidence_accepted_at timestamptz, evidence_note varchar(2000) NOT NULL DEFAULT '',
 stage varchar(30) NOT NULL DEFAULT 'RECEIVED' CHECK (stage IN ('RECEIVED','REVIEWED','CLARIFICATION','READY_FOR_APPROVAL','APPROVED','REJECTED','EXPORTED')),
 duplicate_warning boolean NOT NULL DEFAULT false, duplicate_cleared boolean NOT NULL DEFAULT false,
 business_key varchar(64) NOT NULL, second_required boolean NOT NULL DEFAULT false,
 first_approved_by uuid, second_approved_by uuid, created_by uuid NOT NULL,
 received_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now(),
 version integer NOT NULL DEFAULT 0 CHECK (version >= 0), UNIQUE (tenant_id,id),
 FOREIGN KEY (tenant_id,supplier_id) REFERENCES supplier(tenant_id,id),
 FOREIGN KEY (tenant_id,project_id) REFERENCES project(tenant_id,id),
 FOREIGN KEY (tenant_id,cost_center_id) REFERENCES cost_center(tenant_id,id),
 FOREIGN KEY (tenant_id,approver_id) REFERENCES app_user(tenant_id,id),
 FOREIGN KEY (tenant_id,evidence_accepted_by) REFERENCES app_user(tenant_id,id),
 FOREIGN KEY (tenant_id,created_by) REFERENCES app_user(tenant_id,id),
 FOREIGN KEY (tenant_id,first_approved_by) REFERENCES app_user(tenant_id,id),
 FOREIGN KEY (tenant_id,second_approved_by) REFERENCES app_user(tenant_id,id),
 CHECK (first_approved_by IS NULL OR first_approved_by <> created_by),
 CHECK (second_approved_by IS NULL OR (second_approved_by <> first_approved_by AND second_approved_by <> created_by))
);
CREATE INDEX invoice_queue ON invoice(tenant_id,stage,received_at DESC);
CREATE INDEX invoice_due ON invoice(tenant_id,due_date) WHERE stage NOT IN ('EXPORTED','REJECTED');
CREATE INDEX invoice_duplicate ON invoice(tenant_id,business_key);
CREATE TABLE document (
 id uuid PRIMARY KEY, tenant_id uuid NOT NULL, invoice_id uuid NOT NULL,
 kind varchar(12) NOT NULL CHECK (kind IN ('ORIGINAL','EVIDENCE')),
 filename varchar(180) NOT NULL, content_type varchar(80) NOT NULL,
 sha256 varchar(64) NOT NULL, size_bytes integer NOT NULL CHECK (size_bytes > 0 AND size_bytes <= 10485760),
 content bytea NOT NULL, uploaded_by uuid NOT NULL, created_at timestamptz NOT NULL DEFAULT now(),
 UNIQUE(tenant_id,id), FOREIGN KEY (tenant_id,invoice_id) REFERENCES invoice(tenant_id,id),
 FOREIGN KEY(tenant_id,uploaded_by) REFERENCES app_user(tenant_id,id), CHECK (octet_length(content) = size_bytes)
);
CREATE UNIQUE INDEX one_original_per_invoice ON document(tenant_id,invoice_id) WHERE kind='ORIGINAL';
CREATE UNIQUE INDEX original_dedup ON document(tenant_id,sha256) WHERE kind='ORIGINAL';
CREATE INDEX document_invoice ON document(tenant_id,invoice_id);
CREATE TABLE discussion (
 id uuid PRIMARY KEY, tenant_id uuid NOT NULL, invoice_id uuid NOT NULL, author_id uuid NOT NULL,
 body varchar(2000) NOT NULL, is_question boolean NOT NULL DEFAULT false,
 resolved_by uuid, resolution varchar(2000), resolved_at timestamptz, created_at timestamptz NOT NULL DEFAULT now(),
 FOREIGN KEY (tenant_id,invoice_id) REFERENCES invoice(tenant_id,id),
 FOREIGN KEY (tenant_id,author_id) REFERENCES app_user(tenant_id,id),
 FOREIGN KEY (tenant_id,resolved_by) REFERENCES app_user(tenant_id,id),
 CHECK (char_length(trim(body)) > 0)
);
CREATE INDEX discussion_invoice ON discussion(tenant_id,invoice_id,created_at);
CREATE TABLE audit_event (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY, tenant_id uuid NOT NULL REFERENCES organization(id),
 invoice_id uuid, actor_id uuid, actor_name varchar(120) NOT NULL,
 event_type varchar(60) NOT NULL, detail varchar(2000) NOT NULL DEFAULT '',
 created_at timestamptz NOT NULL DEFAULT now(),
 FOREIGN KEY(tenant_id,invoice_id) REFERENCES invoice(tenant_id,id),
 FOREIGN KEY(tenant_id,actor_id) REFERENCES app_user(tenant_id,id)
);
CREATE INDEX audit_invoice ON audit_event(tenant_id,invoice_id,id);
CREATE TABLE notification (
 id uuid PRIMARY KEY, tenant_id uuid NOT NULL, user_id uuid NOT NULL, invoice_id uuid NOT NULL,
 kind varchar(40) NOT NULL, business_date date NOT NULL, is_read boolean NOT NULL DEFAULT false,
 created_at timestamptz NOT NULL DEFAULT now(),
 UNIQUE (tenant_id,user_id,invoice_id,kind,business_date),
 FOREIGN KEY(tenant_id,user_id) REFERENCES app_user(tenant_id,id),
 FOREIGN KEY(tenant_id,invoice_id) REFERENCES invoice(tenant_id,id)
);
CREATE INDEX notification_inbox ON notification(tenant_id,user_id,is_read,created_at DESC);
CREATE TABLE accounting_export (
 id uuid PRIMARY KEY, tenant_id uuid NOT NULL REFERENCES organization(id),
 idempotency_key uuid NOT NULL, request_fingerprint varchar(64) NOT NULL,
 created_by uuid NOT NULL, created_at timestamptz NOT NULL DEFAULT now(),
 filename varchar(180) NOT NULL, sha256 varchar(64) NOT NULL, content bytea NOT NULL,
 UNIQUE (tenant_id,id), UNIQUE(tenant_id,idempotency_key),
 FOREIGN KEY (tenant_id,created_by) REFERENCES app_user(tenant_id,id)
);
CREATE TABLE export_invoice (
 tenant_id uuid NOT NULL, export_id uuid NOT NULL, invoice_id uuid NOT NULL,
 PRIMARY KEY(tenant_id,export_id,invoice_id), UNIQUE(tenant_id,invoice_id),
 FOREIGN KEY(tenant_id,export_id) REFERENCES accounting_export(tenant_id,id),
 FOREIGN KEY(tenant_id,invoice_id) REFERENCES invoice(tenant_id,id)
);
-- Prevent application-level overwriting of originals / history. A DB administrator can still change the database.
-- This is NOT WORM storage, a tamper-proof archive, or a legal retention certification.
CREATE FUNCTION deny_immutable_change() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN RAISE EXCEPTION 'Immutable record: append a new record instead'; END; $$;
CREATE TRIGGER immutable_document BEFORE UPDATE OR DELETE ON document FOR EACH ROW EXECUTE FUNCTION deny_immutable_change();
CREATE TRIGGER immutable_audit BEFORE UPDATE OR DELETE ON audit_event FOR EACH ROW EXECUTE FUNCTION deny_immutable_change();
CREATE TRIGGER immutable_export BEFORE UPDATE OR DELETE ON accounting_export FOR EACH ROW EXECUTE FUNCTION deny_immutable_change();
