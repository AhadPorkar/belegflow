package com.belegflow.server;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

/** Real HTTP/security/SQL module contracts. Every test rolls back in a disposable PostgreSQL database. */
@SpringBootTest(properties={"bf.demo-enabled=true","server.servlet.session.cookie.secure=false","bf.reminder-delay-ms=3600000","bf.reminder-initial-delay-ms=3600000"})
@AutoConfigureMockMvc @Transactional
@EnabledIfEnvironmentVariable(named="BF_INTEGRATION",matches="true")
class ModuleIntegrationTest {
    @Autowired MockMvc mvc; @Autowired JdbcTemplate db;
    @Autowired UserDetailsService users; @Autowired JsonMapper json;
    static final String FINANCE="finance@belegflow.demo", ADMIN="admin@belegflow.demo", LEON="leon@belegflow.demo", VIEWER="viewer@belegflow.demo", ISOLATED="isolated@belegflow.demo";
    RequestPostProcessor as(String email){return user(users.loadUserByUsername(email));}
    UUID tenant(){return db.queryForObject("SELECT tenant_id FROM app_user WHERE email=?",UUID.class,FINANCE);}
    UUID userId(String email){return db.queryForObject("SELECT id FROM app_user WHERE email=?",UUID.class,email);}
    UUID invoice(String number){return db.queryForObject("SELECT id FROM invoice WHERE tenant_id=? AND invoice_number=? ORDER BY received_at DESC LIMIT 1",UUID.class,tenant(),number);}
    int version(UUID id){return db.queryForObject("SELECT version FROM invoice WHERE id=?",Integer.class,id);}
    long count(String table){return db.queryForObject("SELECT count(*) FROM "+table+" WHERE tenant_id=?",Long.class,tenant());}
    String request(MockHttpServletRequestBuilder b,String who,Object body,int status)throws Exception {
        return mvc.perform(b.with(as(who)).with(csrf()).contentType("application/json").content(json.writeValueAsString(body)))
            .andExpect(status().is(status)).andReturn().getResponse().getContentAsString();
    }
    UUID created(MockHttpServletRequestBuilder b,String who,Object body)throws Exception {
        return UUID.fromString(JsonPath.read(request(b,who,body,200),"$.id"));
    }
    void command(UUID id,String who,String action,int status)throws Exception {
        request(post("/api/invoices/"+id+"/commands"),who,Map.of("version",version(id),"action",action,"note","Integration review"),status);
    }
    MockHttpServletRequestBuilder pdf(String number){
        return multipart("/api/invoices/import").file(new MockMultipartFile("file","invoice.pdf","application/pdf",("%PDF-1.7\n"+number).getBytes(StandardCharsets.UTF_8)))
            .param("supplier_id",db.queryForObject("SELECT supplier_id FROM invoice WHERE id=?",UUID.class,invoice("AC-26-104")).toString())
            .param("invoice_number",number).param("net","100.00").param("tax","19.00").param("gross","119.00");
    }
    @Test void publicConfigurationIsEnglishFirstAndVersioned()throws Exception {
        mvc.perform(get("/api/public/config")).andExpect(status().isOk()).andExpect(jsonPath("$.defaultLocale").value("en")).andExpect(jsonPath("$.version").value("0.2.0"));
    }
    @Test void localePersistsOnlyForTheAuthenticatedMember()throws Exception {
        request(put("/api/session/locale"),FINANCE,Map.of("locale","de"),200);
        mvc.perform(get("/api/session").with(as(FINANCE))).andExpect(jsonPath("$.locale").value("de"));
        mvc.perform(get("/api/session").with(as(LEON))).andExpect(jsonPath("$.locale").value("en"));
    }
    @Test void unsupportedLocaleIsAValidationError()throws Exception {request(put("/api/session/locale"),FINANCE,Map.of("locale","fr"),422);}
    @Test void supplierCreateUpdateAndCatalogUseRealSql()throws Exception {
        UUID id=created(post("/api/suppliers"),FINANCE,Map.of("name","Integration supplier","email","billing@example.test","vat_id","DE123","active",true));
        request(put("/api/suppliers/"+id),FINANCE,Map.of("name","Integration supplier renamed","email","billing@example.test","vat_id","DE123","active",false),200);
        assertEquals(false,db.queryForObject("SELECT active FROM supplier WHERE tenant_id=? AND id=?",Boolean.class,tenant(),id));
        mvc.perform(get("/api/catalog").with(as(FINANCE))).andExpect(status().isOk()).andExpect(jsonPath("$.suppliers[?(@.name == 'Integration supplier renamed')]").isNotEmpty());
    }
    @Test void duplicateNormalizedSupplierReturnsConflict()throws Exception {
        var b=Map.of("name","Integration Duplicate","active",true);
        created(post("/api/suppliers"),FINANCE,b);
        request(post("/api/suppliers"),FINANCE,Map.of("name","Integration-Duplicate","active",true),409);
    }
    @Test void foreignSupplierUpdateIsNotFound()throws Exception {
        UUID supplier=db.queryForObject("SELECT supplier_id FROM invoice WHERE id=?",UUID.class,invoice("AC-26-104"));
        request(put("/api/suppliers/"+supplier),ISOLATED,Map.of("name","Not allowed","active",true),404);
    }
    @Test void projectsAndCostCentersHaveTenantScopedReferences()throws Exception {
        UUID project=created(post("/api/projects"),FINANCE,Map.of("code","INT-PRJ","name","Integration project","manager_id",userId(LEON),"evidence_required",true));
        UUID cost=created(post("/api/cost-centers"),FINANCE,Map.of("code","INT-COST","name","Integration cost center"));
        assertEquals(tenant(),db.queryForObject("SELECT tenant_id FROM project WHERE id=?",UUID.class,project));
        assertEquals(tenant(),db.queryForObject("SELECT tenant_id FROM cost_center WHERE id=?",UUID.class,cost));
    }
    @Test void projectCannotReferenceAnApproverInAnotherTenant()throws Exception {
        request(post("/api/projects"),FINANCE,Map.of("code","INT-BAD","name","Bad project","manager_id",userId(ISOLATED),"evidence_required",false),422);
    }
    @Test void administratorCreatesUserWithHashedPasswordAndEnglishDefault()throws Exception {
        UUID id=created(post("/api/users"),ADMIN,Map.of("email","new-member@example.test","display_name","Integration Member","role","APPROVER","password","A-strong-example-password!","active",true));
        assertEquals("en",db.queryForObject("SELECT locale FROM app_user WHERE id=?",String.class,id));
        assertNotEquals("A-strong-example-password!",db.queryForObject("SELECT password_hash FROM app_user WHERE id=?",String.class,id));
        mvc.perform(post("/api/login").with(csrf()).param("username","new-member@example.test").param("password","A-strong-example-password!")).andExpect(status().isNoContent());
    }
    @Test void userRoleEditInvalidatesPreviouslyIssuedSession()throws Exception {
        var oldPrincipal=users.loadUserByUsername(LEON);
        request(put("/api/users/"+userId(LEON)),ADMIN,Map.of("email",LEON,"display_name","Leon Fischer","role","VIEWER","password","","active",true),200);
        mvc.perform(get("/api/session").with(user(oldPrincipal))).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("SESSION_REVOKED"));
    }
    @Test void adminCannotDisableOwnMembership()throws Exception {
        request(put("/api/users/"+userId(ADMIN)),ADMIN,Map.of("email",ADMIN,"display_name","Admin","role","ADMIN","password","","active",false),422);
    }
    @Test void delegationBindsIsoInstantsAndCanBeRevoked()throws Exception {
        UUID id=created(post("/api/delegations"),ADMIN,Map.of("owner_id",userId(LEON),"delegate_id",userId(ADMIN),"valid_from","2026-10-18T10:00:00Z","valid_until","2026-10-19T10:00:00Z"));
        assertEquals(java.time.Instant.parse("2026-10-18T10:00:00Z"),db.queryForObject("SELECT valid_from FROM delegation WHERE id=?",java.sql.Timestamp.class,id).toInstant());
        request(delete("/api/delegations/"+id),ADMIN,Map.of(),200);
        assertEquals(false,db.queryForObject("SELECT active FROM delegation WHERE id=?",Boolean.class,id));
    }
    @Test void selfDelegationAndInvalidIntervalAreRejected()throws Exception {
        request(post("/api/delegations"),ADMIN,Map.of("owner_id",userId(LEON),"delegate_id",userId(LEON),"valid_from","2026-10-18T10:00:00Z","valid_until","2026-10-18T10:00:00Z"),422);
    }
    @Test void ruleThresholdIsExactAndAudited()throws Exception {
        request(put("/api/settings/rules"),ADMIN,Map.of("threshold","1234.56"),200);
        assertEquals("1234.56",db.queryForObject("SELECT second_approval_threshold FROM organization WHERE id=?",java.math.BigDecimal.class,tenant()).toPlainString());
        assertEquals(1L,db.queryForObject("SELECT count(*) FROM audit_event WHERE tenant_id=? AND event_type='APPROVAL_RULE_CHANGED' AND detail='1234.56'",Long.class,tenant()));
    }
    @Test void manualPdfWithoutDatesReturnsValidationNotServerError()throws Exception {
        long before=count("invoice");
        mvc.perform(pdf("INT-MISSING-DATE").with(as(FINANCE)).with(csrf())).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        assertEquals(before,count("invoice"));
    }
    @Test void malformedManualPdfDateReturnsValidationNotServerError()throws Exception {
        mvc.perform(pdf("INT-BAD-DATE").param("issue_date","2026-99-99").param("due_date","2026-10-31").with(as(FINANCE)).with(csrf())).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }
    @Test void validManualPdfPreservesBytesAndDecimalValues()throws Exception {
        var response=mvc.perform(pdf("INT-PDF-OK").param("issue_date","2026-10-01").param("due_date","2026-10-31").with(as(FINANCE)).with(csrf())).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        UUID id=UUID.fromString(JsonPath.read(response,"$.id"));
        mvc.perform(get("/api/invoices/"+id).with(as(FINANCE))).andExpect(jsonPath("$.gross").value("119.00")).andExpect(jsonPath("$.issue_date").value("2026-10-01")).andExpect(jsonPath("$.stage").value("RECEIVED"));
        assertArrayEquals("%PDF-1.7\nINT-PDF-OK".getBytes(StandardCharsets.UTF_8),db.queryForObject("SELECT content FROM document WHERE invoice_id=? AND kind='ORIGINAL'",byte[].class,id));
    }
    @Test void evidenceUploadInvalidatesPriorAcceptanceAndPreservesBytes()throws Exception {
        UUID id=invoice("KE-2026-1048");
        request(post("/api/invoices/"+id+"/evidence-acceptance"),LEON,Map.of("version",version(id),"note","Accepted previous evidence"),200);
        byte[] data="New service report".getBytes(StandardCharsets.UTF_8);
        String result=mvc.perform(multipart("/api/invoices/"+id+"/evidence").file(new MockMultipartFile("file","new-report.txt","text/plain",data)).param("version",String.valueOf(version(id))).with(as(FINANCE)).with(csrf())).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        UUID doc=UUID.fromString(JsonPath.read(result,"$.id"));
        assertNull(db.queryForObject("SELECT evidence_accepted_by FROM invoice WHERE id=?",UUID.class,id));
        assertArrayEquals(data,mvc.perform(get("/api/documents/"+doc).with(as(FINANCE))).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray());
    }
    @Test void finalizedInvoiceCannotReceiveNewEvidence()throws Exception {
        UUID id=invoice("LR-2026-302");
        mvc.perform(multipart("/api/invoices/"+id+"/evidence").file(new MockMultipartFile("file","report.txt","text/plain","report".getBytes(StandardCharsets.UTF_8))).param("version",String.valueOf(version(id))).with(as(FINANCE)).with(csrf())).andExpect(status().isUnprocessableEntity());
    }
    @Test void clarificationRequiresQuestionResolutionBeforeResubmission()throws Exception {
        UUID id=invoice("AC-26-104");command(id,FINANCE,"CLARIFY",200);command(id,FINANCE,"RESOLVE",422);
        UUID question=db.queryForObject("SELECT id FROM discussion WHERE invoice_id=? AND is_question=true ORDER BY created_at DESC LIMIT 1",UUID.class,id);
        request(post("/api/invoices/"+id+"/questions/"+question+"/resolution"),FINANCE,Map.of("body","Confirmed with supplier"),200);
        command(id,FINANCE,"RESOLVE",200);assertEquals("REVIEWED",db.queryForObject("SELECT stage FROM invoice WHERE id=?",String.class,id));
    }
    @Test void commentTextIsStoredLiterallyAndViewerCannotPost()throws Exception {
        UUID id=invoice("AC-26-104");String text="<script>alert('not executable')</script>";
        UUID comment=created(post("/api/invoices/"+id+"/comments"),FINANCE,Map.of("body",text));
        assertEquals(text,db.queryForObject("SELECT body FROM discussion WHERE id=?",String.class,comment));
        request(post("/api/invoices/"+id+"/comments"),VIEWER,Map.of("body","Denied"),403);
    }
    @Test void duplicateResolutionRequiresAnExplicitReasonAndAudit()throws Exception {
        UUID id=db.queryForObject("SELECT id FROM invoice WHERE tenant_id=? AND duplicate_warning=true ORDER BY received_at DESC LIMIT 1",UUID.class,tenant());
        request(post("/api/invoices/"+id+"/duplicate-resolution"),FINANCE,Map.of("version",version(id),"note",""),422);
        request(post("/api/invoices/"+id+"/duplicate-resolution"),FINANCE,Map.of("version",version(id),"note","Supplier confirmed separate service"),200);
        assertEquals(true,db.queryForObject("SELECT duplicate_cleared FROM invoice WHERE id=?",Boolean.class,id));
    }
    @Test void rejectedTransitionDoesNotWritePartialAuditOrVersion()throws Exception {
        UUID id=invoice("KE-2026-1048");long audit=count("audit_event");int v=version(id);
        command(id,FINANCE,"SUBMIT",422);assertEquals(v,version(id));assertEquals(audit,count("audit_event"));
    }
    @Test void oneTenantCannotReadAnotherTenantsAudit()throws Exception {
        mvc.perform(get("/api/audit").with(as(ISOLATED))).andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(0));
    }
    @Test void notificationReadIsRestrictedToItsRecipient()throws Exception {
        UUID id=UUID.randomUUID(),invoice=invoice("AC-26-104");
        db.update("INSERT INTO notification(id,tenant_id,user_id,invoice_id,kind,business_date) VALUES(?,?,?,?,?,current_date)",id,tenant(),userId(LEON),invoice,"INTEGRATION_ONLY");
        request(post("/api/notifications/"+id+"/read"),FINANCE,Map.of(),404);
        request(post("/api/notifications/"+id+"/read"),LEON,Map.of(),200);
        assertEquals(true,db.queryForObject("SELECT is_read FROM notification WHERE id=?",Boolean.class,id));
    }
    @Test void databaseRejectsOriginalOverwrite(){
        UUID id=invoice("AC-26-104");
        assertThrows(DataAccessException.class,()->db.update("UPDATE document SET filename='tampered.xml' WHERE invoice_id=? AND kind='ORIGINAL'",id));
    }
    @Test void databaseRejectsAuditDeletion(){
        assertThrows(DataAccessException.class,()->db.update("DELETE FROM audit_event WHERE tenant_id=?",tenant()));
    }
    @Test void invoicePaginationAndSearchValidateInputs()throws Exception {
        mvc.perform(get("/api/invoices").param("q","AC-26-104").param("size","1").with(as(FINANCE))).andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(1));
        mvc.perform(get("/api/invoices").param("size","101").with(as(FINANCE))).andExpect(status().isUnprocessableEntity());
    }
    @Test void exportEndpointRejectsUnsignedInvoicesAndDoesNotCreatePackage()throws Exception {
        UUID id=invoice("AC-26-104");long before=count("accounting_export");
        request(post("/api/exports"),FINANCE,Map.of("idempotency_key",UUID.randomUUID(),"invoices",List.of(Map.of("id",id,"version",version(id)))),422);
        assertEquals(before,count("accounting_export"));
    }
}
