package com.belegflow.server.bootstrap;
import com.belegflow.domain.*;
import com.belegflow.server.documents.*;
import com.belegflow.server.audit.AuditService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.time.*;
import java.sql.Timestamp;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
/** Opt-in synthetic data only. Production startup never silently creates demo users. */
@Component public class BootstrapData implements ApplicationRunner {
    public static final UUID TENANT=UUID.fromString("10000000-0000-0000-0000-000000000001");
    public static final UUID FINANCE=UUID.fromString("20000000-0000-0000-0000-000000000001");
    public static final UUID LEON=UUID.fromString("20000000-0000-0000-0000-000000000002");
    public static final UUID ADMIN=UUID.fromString("20000000-0000-0000-0000-000000000003");
    public static final UUID MIRA=UUID.fromString("20000000-0000-0000-0000-000000000004");
    public static final UUID VIEWER=UUID.fromString("20000000-0000-0000-0000-000000000005");
    private final JdbcTemplate db;
    private final PasswordEncoder passwords;
    private final InvoiceImportService importer;
    private final DocumentService documents;
    private final AuditService audit;
    private final Clock clock;
    private final boolean demo;
    private final String email,password,organization;
    public BootstrapData(JdbcTemplate db,PasswordEncoder passwords,InvoiceImportService importer,DocumentService documents,AuditService audit,Clock clock, @Value("${bf.demo-enabled}")boolean demo,@Value("${bf.bootstrap-email}")String email,@Value("${bf.bootstrap-password}")String password,@Value("${bf.bootstrap-organization}")String organization) {
        this.db=db;
        this.passwords=passwords;
        this.importer=importer;
        this.documents=documents;
        this.audit=audit;
        this.clock=clock;
        this.demo=demo;
        this.email=email;
        this.password=password;
        this.organization=organization;
    }
    @Override @Transactional public void run(ApplicationArguments args) {
        db.execute("SELECT pg_advisory_xact_lock(781642103)");
        if(db.queryForObject("SELECT count(*) FROM organization",Long.class)>0)return;
        if(demo) {
            seed();
            return;
        }
        BusinessException.require(!email.isBlank()&&!organization.isBlank()&&password.length()>=16&&!password.toLowerCase(Locale.ROOT).contains("change-me"),"BOOTSTRAP_CONFIGURATION_REQUIRED");
        UUID tenant=UUID.randomUUID(),admin=UUID.randomUUID();
        db.update("INSERT INTO organization(id,name) VALUES(?,?)",tenant,organization);
        user(tenant,admin,email,organization+" admin",Role.ADMIN,password);
        audit.append(new Actor(tenant,admin,organization+" admin",Role.ADMIN),null,"ORGANIZATION_CREATED",organization);
    }
    private void user(UUID tenant,UUID id,String email,String name,Role role,String secret) {
        db.update("INSERT INTO app_user(id,tenant_id,email,display_name,role,password_hash) VALUES(?,?,?,?,?,?)",id,tenant,email.toLowerCase(Locale.ROOT),name,role.name(),passwords.encode(secret));
    }
    private static UUID id(int category,int n) {
        return UUID.fromString(category+"0000000-0000-0000-0000-"+String.format("%012d",n));
    }
    private void seed() {
        String secret="BelegFlow-Demo-2026!";
        db.update("INSERT INTO organization(id,name) VALUES(?,?)",TENANT,"Nordlicht Services GmbH (demo)");
        user(TENANT,FINANCE,"finance@belegflow.demo","Nora Becker",Role.FINANCE,secret);
        user(TENANT,LEON,"leon@belegflow.demo","Leon Fischer",Role.APPROVER,secret);
        user(TENANT,ADMIN,"admin@belegflow.demo","Alex Weber",Role.ADMIN,secret);
        user(TENANT,MIRA,"mira@belegflow.demo","Mira Hoffmann",Role.APPROVER,secret);
        user(TENANT,VIEWER,"viewer@belegflow.demo","Jan Wagner",Role.VIEWER,secret);
        String[] suppliers= {
            "K\u00f6hler Elektrotechnik","Studio Formwerk","Hafen IT Services","Licht & Raum GmbH","Albrecht Consulting","Nordwerk Maintenance"
        };
        for(int k=0;k<suppliers.length;k++)db.update("INSERT INTO supplier(id,tenant_id,name,normalized_name,email,vat_id) VALUES(?,?,?,?,?,?)",id(3,k+1),TENANT,suppliers[k],DuplicateFingerprint.normalize(suppliers[k]),"accounts@vendor"+(k+1)+".example","DE-DEMO-"+(k+1));
        String[] projectNames= {
            "Berlin office expansion","Hamburg service contract","Digital workplace"
        };
        for(int k=0;k<3;k++) {
            db.update("INSERT INTO project(id,tenant_id,code,name,manager_id,evidence_required) VALUES(?,?,?,?,?,?)",id(4,k+1),TENANT,"PRJ-"+(100+k),projectNames[k],k==1?MIRA:LEON,k!=2);
            db.update("INSERT INTO cost_center(id,tenant_id,code,name) VALUES(?,?,?,?)",id(5,k+1),TENANT,"CC-"+(4100+k*100),new String[] {
                "Facilities & infrastructure","Service operations","Technology & delivery"
            }
            [k]);
        }
        Actor finance=new Actor(TENANT,FINANCE,"Nora Becker",Role.FINANCE),leon=new Actor(TENANT,LEON,"Leon Fischer",Role.APPROVER);
        LocalDate today=ReminderPolicy.businessDate(clock.instant());
        String[] net= {
            "2480.00","7800.00","14800.00","3200.00","1650.00","4800.00","980.00","6300.00","2480.00","890.00","1240.00","4200.00"
        };
        String[] statuses= {
            "REVIEWED","CLARIFICATION","READY_FOR_APPROVAL","APPROVED","RECEIVED","READY_FOR_APPROVAL","APPROVED","REVIEWED","RECEIVED","REJECTED","RECEIVED","APPROVED"
        };
        String[] descriptions= {
            "Electrical installation & final commissioning","Workplace design - additional services","Infrastructure migration and handover","Meeting room lighting installation","Process mapping workshop","Quarterly ventilation maintenance","Workplace support subscription","Phase 2 installation materials","Revised electrical installation invoice","Cancelled workshop booking","Network health assessment","Scheduled building maintenance"
        };
        for(int k=0;k<net.length;k++) {
            int vendor=k==8?0:k%suppliers.length,project=k%3;
            UUID manager=project==1?MIRA:LEON;
            String number=k==8?"KE-2026-1048":new String[] {
                "KE-2026-1048","FW-2026-087","HIT-26041","LR-2026-302","AC-26-104","NW-2026-219","HIT-26044","KE-2026-1091","KE-2026-1048","AC-26-098","HIT-26045","NW-2026-223"
            }
            [k];
            BigDecimal n=new BigDecimal(net[k]),tax=n.multiply(new BigDecimal("0.19")).setScale(2),gross=n.add(tax);
            byte[] xml=ubl(number,suppliers[vendor],today.minusDays(7+k),today.plusDays(k-2),n.toPlainString(),tax.toPlainString(),gross.toPlainString(),descriptions[k]);
            var imported=importer.ingestFor(finance,"demo-"+(k+1)+".xml",xml,Map.of("supplier_id",id(3,vendor+1).toString(),"project_id",id(4,project+1).toString(),"cost_center_id",id(5,project+1).toString(),"approver_id",manager.toString()));
            UUID invoice=imported.id();
            String stage=statuses[k];
            boolean accepted=Set.of("READY_FOR_APPROVAL","APPROVED").contains(stage);
            if(k==0||accepted) {
                byte[] evidence=("SYNTHETIC DEMO SERVICE REPORT\nProject: "+projectNames[project]+"\nWork: "+descriptions[k]+"\nAll records in this demonstration are fictional.\n").getBytes(StandardCharsets.UTF_8);
                documents.insert(finance,invoice,"EVIDENCE","service-report-demo.txt","text/plain",evidence);
                audit.append(finance,invoice,"EVIDENCE_UPLOADED","service-report-demo.txt");
            }
            if(!stage.equals("RECEIVED"))audit.append(finance,invoice,"INVOICE_REVIEWED","");
            if(accepted) {
                db.update("UPDATE invoice SET evidence_accepted_by=?,evidence_accepted_at=now(),evidence_note=? WHERE id=?",manager,"Synthetic service-completion acceptance",invoice);
                audit.append(new Actor(TENANT,manager,manager.equals(LEON)?"Leon Fischer":"Mira Hoffmann",Role.APPROVER),invoice,"SERVICE_EVIDENCE_ACCEPTED","Synthetic fixture acceptance");
            }
            boolean approved=stage.equals("APPROVED")||k==2;
            db.update("UPDATE invoice SET stage=?,first_approved_by=?,received_at=?,updated_at=? WHERE id=?",stage,approved?manager:null,Timestamp.from(clock.instant().minusSeconds(k*3600L)),Timestamp.from(clock.instant()),invoice);
            if(Set.of("READY_FOR_APPROVAL","APPROVED").contains(stage))audit.append(finance,invoice,"SUBMITTED_FOR_APPROVAL","");
            if(approved)audit.append(new Actor(TENANT,manager,manager.equals(LEON)?"Leon Fischer":"Mira Hoffmann",Role.APPROVER),invoice,k==2?"FIRST_APPROVAL_RECORDED":"INVOICE_APPROVED","");
            if(stage.equals("CLARIFICATION")) {
                db.update("INSERT INTO discussion(id,tenant_id,invoice_id,author_id,body,is_question) VALUES(?,?,?,?,?,true)",UUID.randomUUID(),TENANT,invoice,FINANCE,"Please confirm whether the additional design work was included in the agreed scope.");
                audit.append(finance,invoice,"CLARIFICATION_REQUESTED","Additional work needs confirmation.");
            }
            if(stage.equals("REJECTED"))audit.append(finance,invoice,"INVOICE_REJECTED","Booking was cancelled before delivery.");
        }
        UUID secondTenant=id(1,2),secondUser=id(2,6);
        db.update("INSERT INTO organization(id,name) VALUES(?,?)",secondTenant,"Elbe Technik (isolated demo)");
        user(secondTenant,secondUser,"isolated@belegflow.demo","Isolation Tester",Role.ADMIN,secret);
    }
    /** Small extraction fixture, intentionally not advertised as a conformant XRechnung. */
    public static byte[] ubl(String number,String supplier,LocalDate issue,LocalDate due,String net,String tax,String gross,String note) {
        String xml="""
    <?xml version="1.0" encoding="UTF-8"?>
    <Invoice xmlns="urn:oasis:names:specification:ubl:schema:xsd:Invoice-2" xmlns:cbc="urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2" xmlns:cac="urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2">
    <cbc:ID>%s</cbc:ID><cbc:IssueDate>%s</cbc:IssueDate><cbc:DueDate>%s</cbc:DueDate><cbc:InvoiceTypeCode>380</cbc:InvoiceTypeCode><cbc:Note>%s</cbc:Note><cbc:DocumentCurrencyCode>EUR</cbc:DocumentCurrencyCode>
    <cac:AccountingSupplierParty><cac:Party><cac:PartyLegalEntity><cbc:RegistrationName>%s</cbc:RegistrationName></cac:PartyLegalEntity></cac:Party></cac:AccountingSupplierParty>
    <cac:TaxTotal><cbc:TaxAmount currencyID="EUR">%s</cbc:TaxAmount></cac:TaxTotal>
    <cac:LegalMonetaryTotal><cbc:TaxExclusiveAmount currencyID="EUR">%s</cbc:TaxExclusiveAmount><cbc:TaxInclusiveAmount currencyID="EUR">%s</cbc:TaxInclusiveAmount><cbc:PayableAmount currencyID="EUR">%s</cbc:PayableAmount></cac:LegalMonetaryTotal>
    </Invoice>
    """.formatted(escape(number),issue,due,escape(note),escape(supplier),tax,net,gross,gross);
        return xml.getBytes(StandardCharsets.UTF_8);
    }
    private static String escape(String text) {
        return text.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");
    }
}
