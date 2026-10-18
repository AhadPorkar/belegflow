package com.belegflow.server;

import com.belegflow.domain.*;
import com.belegflow.server.documents.InvoiceImportService;
import com.belegflow.server.invoices.InvoiceService;
import com.belegflow.server.exports.ExportService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;

/** Uses independent connections and real Spring transactions, not an enclosing rollback transaction.
 * Each test owns a new isolated tenant. Run ONLY against the disposable test database.
 * Immutable histories intentionally remain until that disposable database is removed.
 */
@SpringBootTest(properties={"bf.demo-enabled=true","server.servlet.session.cookie.secure=false","bf.reminder-delay-ms=3600000","bf.reminder-initial-delay-ms=3600000"})
@EnabledIfEnvironmentVariable(named="BF_INTEGRATION",matches="true")
class ConcurrencyIntegrationTest {
    @Autowired JdbcTemplate db; @Autowired UserDetailsService users;
    @Autowired InvoiceImportService importer; @Autowired InvoiceService invoices; @Autowired ExportService exports;
    UUID tenant,finance,approver;String email;
    @BeforeEach void isolatedTenant(){
        tenant=UUID.randomUUID();finance=UUID.randomUUID();approver=UUID.randomUUID();email="concurrency-"+finance+"@example.test";
        db.update("INSERT INTO organization(id,name) VALUES(?,?)",tenant,"Concurrency fixture");
        db.update("INSERT INTO app_user(id,tenant_id,email,display_name,role,password_hash) VALUES(?,?,?,?,?,?)",finance,tenant,email,"Concurrency finance","FINANCE","not-a-login-hash");
        db.update("INSERT INTO app_user(id,tenant_id,email,display_name,role,password_hash) VALUES(?,?,?,?,?,?)",approver,tenant,"approver-"+approver+"@example.test","Concurrency approver","APPROVER","not-a-login-hash");
    }
    <T> T authenticated(Supplier<T> action){
        var principal=users.loadUserByUsername(email);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal,"unused",principal.getAuthorities()));
        try{return action.get();}finally{SecurityContextHolder.clearContext();}
    }
    byte[] source(){
        return ("""
          <Invoice xmlns="urn:oasis:names:specification:ubl:schema:xsd:Invoice-2"
          xmlns:cbc="urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2"
          xmlns:cac="urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2">
          <cbc:ID>CONCURRENT-001</cbc:ID><cbc:IssueDate>2026-10-01</cbc:IssueDate>
          <cbc:DueDate>2026-10-31</cbc:DueDate><cbc:InvoiceTypeCode>380</cbc:InvoiceTypeCode><cbc:DocumentCurrencyCode>EUR</cbc:DocumentCurrencyCode>
          <cac:AccountingSupplierParty><cac:Party><cac:PartyName><cbc:Name>Concurrency supplier</cbc:Name></cac:PartyName></cac:Party></cac:AccountingSupplierParty>
          <cac:TaxTotal><cbc:TaxAmount currencyID="EUR">19.00</cbc:TaxAmount></cac:TaxTotal>
          <cac:LegalMonetaryTotal><cbc:TaxExclusiveAmount currencyID="EUR">100.00</cbc:TaxExclusiveAmount><cbc:TaxInclusiveAmount currencyID="EUR">119.00</cbc:TaxInclusiveAmount><cbc:PayableAmount currencyID="EUR">119.00</cbc:PayableAmount></cac:LegalMonetaryTotal>
          </Invoice>
          """).getBytes(StandardCharsets.UTF_8);
    }
    <T> List<T> race(Supplier<T> action)throws Exception {
        var barrier=new CyclicBarrier(2);
        try(var pool=Executors.newFixedThreadPool(2)){
            Callable<T> call=()->{barrier.await(10,TimeUnit.SECONDS);return authenticated(action);};
            var left=pool.submit(call);var right=pool.submit(call);
            return List.of(left.get(20,TimeUnit.SECONDS),right.get(20,TimeUnit.SECONDS));
        }
    }
    @Test void simultaneousIdenticalImportsCreateOneInvoiceAndOneOriginal()throws Exception {
        var results=race(()->importer.ingest("concurrent.xml",source(),Map.of()));
        assertEquals(results.get(0).id(),results.get(1).id());
        assertEquals(1,results.stream().filter(InvoiceImportService.Result::reused).count());
        assertEquals(1L,db.queryForObject("SELECT count(*) FROM invoice WHERE tenant_id=?",Long.class,tenant));
        assertEquals(1L,db.queryForObject("SELECT count(*) FROM document WHERE tenant_id=? AND kind='ORIGINAL'",Long.class,tenant));
    }
    @Test void simultaneousReviewCommandsCommitOneVersionAndOneAuditEvent()throws Exception {
        UUID id=authenticated(()->importer.ingest("concurrent.xml",source(),Map.of()).id());
        var results=race(()->{try{invoices.command(id,new InvoiceService.Command(0,WorkflowAction.REVIEW,"Review"));return "OK";}catch(BusinessException e){return e.code();}});
        assertEquals(Set.of("OK","VERSION_CONFLICT"),new HashSet<>(results));
        assertEquals(1,db.queryForObject("SELECT version FROM invoice WHERE id=?",Integer.class,id));
        assertEquals(1L,db.queryForObject("SELECT count(*) FROM audit_event WHERE tenant_id=? AND invoice_id=? AND event_type='INVOICE_REVIEWED'",Long.class,tenant,id));
    }
    @Test void simultaneousExportsWithSameKeyCreateOneStoredPackage()throws Exception {
        UUID id=authenticated(()->importer.ingest("concurrent.xml",source(),Map.of()).id());
        db.update("UPDATE invoice SET stage='APPROVED',first_approved_by=? WHERE tenant_id=? AND id=?",approver,tenant,id);
        var request=new ExportService.Request(UUID.randomUUID(),List.of(new ExportService.Entry(id,0)));
        var results=race(()->exports.create(request));assertEquals(results.get(0),results.get(1));
        assertEquals(1L,db.queryForObject("SELECT count(*) FROM accounting_export WHERE tenant_id=?",Long.class,tenant));
        assertEquals(1L,db.queryForObject("SELECT count(*) FROM export_invoice WHERE tenant_id=? AND invoice_id=?",Long.class,tenant,id));
        assertEquals(1,db.queryForObject("SELECT version FROM invoice WHERE id=?",Integer.class,id));
    }
}
