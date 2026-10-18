package com.belegflow.server;
import com.belegflow.domain.*;
import com.belegflow.server.audit.AuditService;
import com.belegflow.server.identity.IdentityService;
import com.belegflow.server.invoices.InvoiceRepository;
import com.belegflow.server.documents.DocumentService;
import com.belegflow.server.exports.ExportService;
import com.belegflow.server.notifications.ReminderService;
import com.belegflow.server.shared.Sql;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
/** Adapter-level unit tests. IntegrationTest verifies the actual SQL and Spring filters. */
class AdapterTest {
    private final UUID tenant=UUID.randomUUID(),user=UUID.randomUUID();
    private final JdbcTemplate db=mock(JdbcTemplate.class);
    private final IdentityService identity=mock(IdentityService.class);
    private final Clock clock=Clock.fixed(Instant.parse("2026-10-18T12:00:00Z"),ZoneOffset.UTC);
    @AfterEach void clearContext() {
        SecurityContextHolder.clearContext();
    }
    @Test void normalizesDatabaseValuesWithoutFloatingPoint() {
        Map<String,Object> row=new HashMap<>();
        row.put("gross",new BigDecimal("1190.00"));
        row.put("day",java.sql.Date.valueOf("2026-10-18"));
        row.put("time",java.sql.Timestamp.from(clock.instant()));
        row.put("optional",null);
        var api=Sql.api(row);
        assertEquals("1190.00",api.get("gross"));
        assertEquals("2026-10-18",api.get("day"));
        assertEquals("2026-10-18T12:00:00Z",api.get("time"));
        assertTrue(api.containsKey("optional"));
    }
    @Test void boundedRequiredText() {
        assertEquals("a",Sql.text(" a ",2,true));
        assertThrows(BusinessException.class,()->Sql.text("aaa",2,false));
        assertThrows(BusinessException.class,()->Sql.text(null,5,true));
    }
    @Test void unauthenticatedIdentityCannotReachTheDatabase() {
        assertThrows(BusinessException.class,()->new IdentityService(db).actor());
        verifyNoInteractions(db);
    }
    @Test void missingApproverRejectedBeforeQuery() {
        assertThrows(BusinessException.class,()->new IdentityService(db).approverExists(tenant,null));
        verifyNoInteractions(db);
    }
    @Test void staleInvoiceVersionRejected() {
        var repo=new InvoiceRepository(db);
        assertDoesNotThrow(()->repo.version(Map.of("version",3),3));
        var e=assertThrows(BusinessException.class,()->repo.version(Map.of("version",3),2));
        assertEquals("VERSION_CONFLICT",e.code());
    }
    @Test void auditCarriesTenantAndActor() {
        Actor a=new Actor(tenant,user,"Nora",Role.FINANCE);
        UUID invoice=UUID.randomUUID();
        new AuditService(db).append(a,invoice,"INVOICE_REVIEWED",null);
        verify(db).update("INSERT INTO audit_event(tenant_id,invoice_id,actor_id,actor_name,event_type,detail) VALUES (?,?,?,?,?,?)",tenant,invoice,user,"Nora","INVOICE_REVIEWED","");
    }
    @Test void documentLookupIsScopedAndMissingIsNotFound() {
        when(identity.actor()).thenReturn(new Actor(tenant,user,"Nora",Role.FINANCE));
        UUID id=UUID.randomUUID();
        when(db.queryForList("SELECT filename,content_type,sha256,content FROM document WHERE tenant_id=? AND id=?",tenant,id)).thenReturn(List.of());
        var service=new DocumentService(db,identity,new InvoiceRepository(db),new AuditService(db),clock);
        assertEquals("NOT_FOUND",assertThrows(BusinessException.class,()->service.download(id)).code());
    }
    @Test void exportRejectsReadOnlyRoleBeforeDatabaseWork() {
        when(identity.actor()).thenReturn(new Actor(tenant,user,"Jan",Role.VIEWER));
        var service=new ExportService(db,identity,new InvoiceRepository(db),new AuditService(db),clock);
        assertThrows(BusinessException.class,()->service.create(new ExportService.Request(UUID.randomUUID(),List.of())));
        verifyNoInteractions(db);
    }
    @Test void remindersUseBusinessDateAndEmptyWorkReturnsZero() {
        String sql="SELECT tenant_id,id,approver_id,first_approved_by,created_by,second_required FROM invoice WHERE stage='READY_FOR_APPROVAL' AND due_date<=? AND approver_id IS NOT NULL AND tenant_id=?";
        when(db.queryForList(sql,LocalDate.of(2026,10,21),tenant)).thenReturn(List.of());
        assertEquals(0,new ReminderService(db,clock).run(tenant));
        verify(db).queryForList(sql,LocalDate.of(2026,10,21),tenant);
    }
    @Test void catalogWriteRejectsReadOnlyRoleBeforeDatabaseWork() {
        when(identity.actor()).thenReturn(new Actor(tenant,user,"Viewer",Role.VIEWER));
        var catalog=new com.belegflow.server.catalog.CatalogService(db,identity,new AuditService(db),mock(org.springframework.security.crypto.password.PasswordEncoder.class));
        assertThrows(BusinessException.class,()->catalog.costCenter(new com.belegflow.server.catalog.CatalogService.CostCenterBody("C1","Office")));
        verifyNoInteractions(db);
    }
    @Test void catalogThresholdRejectsZeroBeforeDatabaseWork() {
        when(identity.actor()).thenReturn(new Actor(tenant,user,"Admin",Role.ADMIN));
        var catalog=new com.belegflow.server.catalog.CatalogService(db,identity,new AuditService(db),mock(org.springframework.security.crypto.password.PasswordEncoder.class));
        assertThrows(BusinessException.class,()->catalog.rules(new com.belegflow.server.catalog.CatalogService.RuleBody("0.00")));
        verifyNoInteractions(db);
    }
    @Test void importRejectsViewerBeforeStorageOrParsing() {
        when(identity.actor()).thenReturn(new Actor(tenant,user,"Viewer",Role.VIEWER));
        var documents=mock(DocumentService.class);
        var service=new com.belegflow.server.documents.InvoiceImportService(db,identity,documents,new AuditService(db));
        assertThrows(BusinessException.class,()->service.ingest("a.xml","<a/>".getBytes(java.nio.charset.StandardCharsets.UTF_8),Map.of()));
        verifyNoInteractions(db,documents);
    }
}
