package com.belegflow.server.support;

import com.belegflow.domain.*;
import com.belegflow.server.audit.AuditService;
import com.belegflow.server.identity.IdentityService;
import com.belegflow.server.invoices.InvoiceRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.mockito.invocation.Invocation;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.mockito.Mockito.*;

/** Isolated adapter fixture. Mocks do not claim that SQL or transactions were executed. */
public abstract class ServiceFixture {
    protected final UUID tenant=UUID.randomUUID(), actorId=UUID.randomUUID(), invoiceId=UUID.randomUUID();
    protected final UUID approverId=UUID.randomUUID(), creatorId=UUID.randomUUID(), projectId=UUID.randomUUID(), costId=UUID.randomUUID();
    protected final JdbcTemplate db=mock(JdbcTemplate.class);
    protected final IdentityService identity=mock(IdentityService.class);
    protected final AuditService audit=mock(AuditService.class);
    protected final InvoiceRepository repo=mock(InvoiceRepository.class);
    protected final PasswordEncoder encoder=mock(PasswordEncoder.class);
    protected final Clock clock=Clock.fixed(Instant.parse("2026-10-18T12:00:00Z"),ZoneOffset.UTC);
    protected Actor actor;

    protected ServiceFixture() {
        as(Role.FINANCE);
        when(db.update(anyString(),any(Object[].class))).thenReturn(1);
    }
    protected void as(Role role) {
        actor=new Actor(tenant,actorId,"Test Actor",role);
        when(identity.actor()).thenReturn(actor);
    }
    protected Map<String,Object> row(String stage) {
        Map<String,Object> row=new HashMap<>();
        row.put("id",invoiceId);row.put("tenant_id",tenant);row.put("version",3);
        row.put("stage",stage);row.put("created_by",creatorId);row.put("approver_id",approverId);
        row.put("project_id",projectId);row.put("project_manager_id",approverId);row.put("cost_center_id",costId);
        row.put("evidence_required",true);row.put("basic_valid",true);row.put("duplicate_warning",false);
        row.put("duplicate_cleared",false);row.put("second_required",false);row.put("validation_notes","");
        row.put("net",new BigDecimal("100.00"));row.put("tax",new BigDecimal("19.00"));row.put("gross",new BigDecimal("119.00"));
        row.put("currency","EUR");row.put("issue_date",java.sql.Date.valueOf("2026-10-01"));
        row.put("due_date",java.sql.Date.valueOf("2026-10-31"));row.put("invoice_number","UNIT-001");
        row.put("supplier_name","Test Supplier");row.put("business_key","unit-key");
        when(repo.get(tenant,invoiceId,true)).thenReturn(row);
        when(repo.get(tenant,invoiceId,false)).thenReturn(row);
        when(repo.facts(row)).thenAnswer(call -> facts(row));
        return row;
    }
    protected WorkflowFacts facts(Map<String,Object> r) {
        return new WorkflowFacts(InvoiceStage.valueOf((String)r.get("stage")),(UUID)r.get("created_by"),
            (UUID)r.get("approver_id"),Boolean.TRUE.equals(r.get("basic_valid")),
            r.get("project_id")!=null&&r.get("cost_center_id")!=null&&r.get("approver_id")!=null,
            Boolean.TRUE.equals(r.get("duplicate_warning"))&&!Boolean.TRUE.equals(r.get("duplicate_cleared")),
            Boolean.TRUE.equals(r.get("evidence_required")),r.get("evidence_accepted_by")!=null,
            (int)r.getOrDefault("open_questions",0),Boolean.TRUE.equals(r.get("second_required")),
            (UUID)r.get("first_approved_by"),(UUID)r.get("second_approved_by"));
    }
    protected List<Invocation> writes() {
        return mockingDetails(db).getInvocations().stream().filter(i->i.getMethod().getName().equals("update")).toList();
    }
    protected Invocation write(String fragment) {
        return writes().stream().filter(i->i.getArgument(0,String.class).contains(fragment)).findFirst().orElseThrow();
    }
    protected Object arg(String sqlFragment,int index) { return write(sqlFragment).getArguments()[index]; }
}
