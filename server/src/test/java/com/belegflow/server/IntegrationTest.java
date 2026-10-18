package com.belegflow.server;
import com.belegflow.domain.DuplicateFingerprint;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipInputStream;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
/** Real PostgreSQL + Spring Security tests. Opt-in prevents an absent DB being reported as tested. */
@SpringBootTest(properties= {
    "bf.demo-enabled=true","server.servlet.session.cookie.secure=false","bf.reminder-delay-ms=3600000"
}
) @AutoConfigureMockMvc @EnabledIfEnvironmentVariable(named="BF_INTEGRATION",matches="true") @Transactional class IntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate db;
    @Autowired UserDetailsService users;
    private static final String FINANCE="finance@belegflow.demo",LEON="leon@belegflow.demo",ADMIN="admin@belegflow.demo",VIEWER="viewer@belegflow.demo",ISOLATED="isolated@belegflow.demo";
    private RequestPostProcessor as(String email) {
        return user(users.loadUserByUsername(email));
    }
    private UUID tenant() {
        return db.queryForObject("SELECT tenant_id FROM app_user WHERE email=?",UUID.class,FINANCE);
    }
    private UUID invoice(String number) {
        return db.queryForObject("SELECT id FROM invoice WHERE tenant_id=? AND invoice_number=? ORDER BY received_at DESC LIMIT 1",UUID.class,tenant(),number);
    }
    private int version(UUID invoice) {
        return db.queryForObject("SELECT version FROM invoice WHERE id=?",Integer.class,invoice);
    }
    private void command(UUID invoice,String actor,String action,int expected)throws Exception {
        mvc.perform(post("/api/invoices/"+invoice+"/commands").with(as(actor)).with(csrf()).contentType("application/json").content("{\"version\":"+version(invoice)+",\"action\":\""+action+"\",\"note\":\"Verified in integration test\"}")).andExpect(status().is(expected));
    }
    @Test void authenticationRequired()throws Exception {
        mvc.perform(get("/api/invoices")).andExpect(status().isUnauthorized());
    }
    @Test void csrfRequiredForMutations()throws Exception {
        mvc.perform(post("/api/suppliers").with(as(FINANCE)).contentType("application/json").content("{\"name\":\"Blocked\"}")).andExpect(status().isForbidden());
    }
    @Test void loginChecksPasswordAndIssuesSession()throws Exception {
        mvc.perform(post("/api/login").with(csrf()).param("username",FINANCE).param("password","wrong-password")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/login").with(csrf()).param("username",FINANCE).param("password","BelegFlow-Demo-2026!")).andExpect(status().isNoContent());
    }
    @Test void foreignInvoiceAndDocumentAreInvisible()throws Exception {
        UUID i=invoice("KE-2026-1048"),doc=db.queryForObject("SELECT id FROM document WHERE invoice_id=? AND kind='ORIGINAL'",UUID.class,i);
        mvc.perform(get("/api/invoices/"+i).with(as(ISOLATED))).andExpect(status().isNotFound());
        mvc.perform(get("/api/documents/"+doc).with(as(ISOLATED))).andExpect(status().isNotFound());
        mvc.perform(get("/api/invoices").with(as(ISOLATED))).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
    }
    @Test void readOnlyUserCannotMutateOrExport()throws Exception {
        command(invoice("AC-26-104"),VIEWER,"REVIEW",403);
        mvc.perform(get("/api/exports").with(as(VIEWER))).andExpect(status().isForbidden());
    }
    @Test void evidenceCannotBeBypassedByDirectApiCall()throws Exception {
        UUID i=invoice("KE-2026-1048");
        command(i,FINANCE,"SUBMIT",422);
        mvc.perform(post("/api/invoices/"+i+"/evidence-acceptance").with(as(FINANCE)).with(csrf()).contentType("application/json").content("{\"version\":"+version(i)+",\"note\":\"Trying to accept own invoice\"}")).andExpect(status().isForbidden());
    }
    @Test void staleVersionReturnsConflictWithoutSecondAuditWrite()throws Exception {
        UUID i=invoice("AC-26-104");
        int before=version(i);
        command(i,FINANCE,"REVIEW",200);
        mvc.perform(post("/api/invoices/"+i+"/commands").with(as(FINANCE)).with(csrf()).contentType("application/json").content("{\"version\":"+before+",\"action\":\"REVIEW\"}")).andExpect(status().isConflict());
        assertEquals(before+1,version(i));
    }
    @Test void fullEvidenceApprovalExportIsBytePreservingAndIdempotent()throws Exception {
        UUID i=invoice("KE-2026-1048");
        mvc.perform(post("/api/invoices/"+i+"/evidence-acceptance").with(as(LEON)).with(csrf()).contentType("application/json").content("{\"version\":"+version(i)+",\"note\":\"The service was completed and checked.\"}")).andExpect(status().isOk());
        command(i,FINANCE,"SUBMIT",200);
        command(i,LEON,"APPROVE",200);
        UUID key=UUID.randomUUID();
        String request="{\"idempotency_key\":\""+key+"\",\"invoices\":[{\"id\":\""+i+"\",\"version\":"+version(i)+"}]}";
        String response=mvc.perform(post("/api/exports").with(as(FINANCE)).with(csrf()).contentType("application/json").content(request)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String id=JsonPath.read(response,"$.id");
        mvc.perform(post("/api/exports").with(as(FINANCE)).with(csrf()).contentType("application/json").content(request)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));
        byte[] zip=mvc.perform(get("/api/exports/"+id+"/download").with(as(FINANCE))).andExpect(status().isOk()).andExpect(header().string("Content-Type","application/zip")).andReturn().getResponse().getContentAsByteArray();
        byte[] original=db.queryForObject("SELECT content FROM document WHERE invoice_id=? AND kind='ORIGINAL'",byte[].class,i);
        boolean found=false;
        Set<String> names=new HashSet<>();
        try(var stream=new ZipInputStream(new ByteArrayInputStream(zip),StandardCharsets.UTF_8)) {
            java.util.zip.ZipEntry entry;
            while((entry=stream.getNextEntry())!=null) {
                names.add(entry.getName());
                byte[] data=stream.readAllBytes();
                if(entry.getName().startsWith("originals/")) {
                    assertArrayEquals(original,data);
                    found=true;
                }
            }
        }
        assertTrue(found);
        assertTrue(names.containsAll(Set.of("invoices.csv","audit.csv","manifest.csv","README.en.txt","README.de.txt")));
        assertEquals("EXPORTED",db.queryForObject("SELECT stage FROM invoice WHERE id=?",String.class,i));
        assertEquals(DuplicateFingerprint.sha256(zip),db.queryForObject("SELECT sha256 FROM accounting_export WHERE id=?",String.class,UUID.fromString(id)));
    }
    @Test void highValueSecondApprovalRequiresIndependentAdministrator()throws Exception {
        UUID i=invoice("HIT-26041");
        command(i,LEON,"APPROVE",403);
        command(i,ADMIN,"APPROVE",200);
        assertEquals("APPROVED",db.queryForObject("SELECT stage FROM invoice WHERE id=?",String.class,i));
    }
    @Test void exactOriginalImportReusesExistingInvoice()throws Exception {
        UUID i=invoice("KE-2026-1048");
        byte[] source=db.queryForObject("SELECT content FROM document WHERE invoice_id=? AND kind='ORIGINAL'",byte[].class,i);
        mvc.perform(multipart("/api/invoices/import").file(new MockMultipartFile("file","invoice.xml","application/xml",source)).with(as(FINANCE)).with(csrf())).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(i.toString())).andExpect(jsonPath("$.reused").value(true));
    }
    @Test void unsafeXmlRejectedBeforeSupplierCreation()throws Exception {
        var file=new MockMultipartFile("file","attack.xml","application/xml","<!DOCTYPE a [<!ENTITY x SYSTEM 'file:///etc/passwd'>]><a>&x;</a>".getBytes(StandardCharsets.UTF_8));
        mvc.perform(multipart("/api/invoices/import").file(file).with(as(FINANCE)).with(csrf())).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("INVALID_XML"));
    }
    @Test void originalContentHasAttachmentAndNosniffHeaders()throws Exception {
        UUID i=invoice("KE-2026-1048"),doc=db.queryForObject("SELECT id FROM document WHERE invoice_id=? AND kind='ORIGINAL'",UUID.class,i);
        mvc.perform(get("/api/documents/"+doc).with(as(FINANCE))).andExpect(status().isOk()).andExpect(header().string("X-Content-Type-Options","nosniff")).andExpect(header().exists("Content-Disposition")).andExpect(header().exists("X-Content-SHA256"));
    }
    @Test void nonAdminCannotChangeApprovalThreshold()throws Exception {
        mvc.perform(put("/api/settings/rules").with(as(FINANCE)).with(csrf()).contentType("application/json").content("{\"threshold\":\"1.00\"}")).andExpect(status().isForbidden());
    }
    @Test void membersAndCatalogAreTenantScoped()throws Exception {
        mvc.perform(get("/api/catalog").with(as(ISOLATED))).andExpect(status().isOk()).andExpect(jsonPath("$.suppliers.length()").value(0)).andExpect(jsonPath("$.users.length()").value(1));
    }
    @Test void remindersAreDeduplicated()throws Exception {
        mvc.perform(post("/api/notifications/run").with(as(ADMIN)).with(csrf())).andExpect(status().isOk());
        long before=db.queryForObject("SELECT count(*) FROM notification WHERE tenant_id=?",Long.class,tenant());
        mvc.perform(post("/api/notifications/run").with(as(ADMIN)).with(csrf())).andExpect(status().isOk());
        assertEquals(before,db.queryForObject("SELECT count(*) FROM notification WHERE tenant_id=?",Long.class,tenant()).longValue());
    }
}
