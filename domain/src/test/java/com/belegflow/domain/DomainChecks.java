package com.belegflow.domain;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.function.Consumer;
/** Same cases run in JUnit and in the offline javac harness. No mocked business rules. */
public final class DomainChecks {
    public record Check(String name,Runnable body) {
    }
    private static final UUID TENANT=UUID.fromString("10000000-0000-0000-0000-000000000001"), CREATOR=UUID.fromString("20000000-0000-0000-0000-000000000001"), ASSIGNEE=UUID.fromString("20000000-0000-0000-0000-000000000002"), ADMIN=UUID.fromString("20000000-0000-0000-0000-000000000003"), OTHER=UUID.fromString("20000000-0000-0000-0000-000000000004");
    private static final Actor FINANCE=new Actor(TENANT,CREATOR,"Finance",Role.FINANCE), APPROVER=new Actor(TENANT,ASSIGNEE,"Approver",Role.APPROVER), ADMIN_ACTOR=new Actor(TENANT,ADMIN,"Admin",Role.ADMIN), VIEWER=new Actor(TENANT,OTHER,"Viewer",Role.VIEWER);
    private static final WorkflowPolicy POLICY=new WorkflowPolicy();
    static final class Facts {
        InvoiceStage stage=InvoiceStage.REVIEWED;
        UUID creator=CREATOR,assignee=ASSIGNEE,first=null,second=null;
        boolean basic=true,allocated=true,duplicate=false,required=true,accepted=true,high=false;
        int questions=0;
        WorkflowFacts get() {
            return new WorkflowFacts(stage,creator,assignee,basic,allocated,duplicate,required,accepted,questions,high,first,second);
        }
    }
    private static WorkflowFacts facts(Consumer<Facts> configure) {
        Facts f=new Facts();
        configure.accept(f);
        return f.get();
    }
    private static void eq(Object actual,Object expected) {
        if(!Objects.equals(actual,expected))throw new AssertionError("Expected "+expected+", got "+actual);
    }
    private static void yes(boolean condition) {
        if(!condition)throw new AssertionError("Expected true");
    }
    private static void no(boolean condition) {
        if(condition)throw new AssertionError("Expected false");
    }
    private static void error(String code,Runnable action) {
        try {
            action.run();
            throw new AssertionError("Expected "+code);
        } catch(BusinessException ex) {
            eq(ex.code(),code);
        }
    }
    private static WorkflowPolicy.Decision decide(Consumer<Facts> f,WorkflowAction a,Actor actor) {
        return POLICY.decide(facts(f),a,actor,false,"Reviewed with evidence.");
    }
    private static byte[] bytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }
    private static String fixture(String name) {
        try(var in=DomainChecks.class.getResourceAsStream("/"+name)) {
            if(in!=null)return new String(in.readAllBytes(),StandardCharsets.UTF_8);
            return Files.readString(Path.of("domain/src/test/resources",name));
        } catch(Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
    public static List<Check> cases() {
        List<Check> c=new ArrayList<>();
        c.add(new Check("workflow: receive and review",()->eq(decide(f->f.stage=InvoiceStage.RECEIVED,WorkflowAction.REVIEW,FINANCE).stage(),InvoiceStage.REVIEWED)));
        c.add(new Check("workflow: review requires finance",()->error("FORBIDDEN",()->decide(f->f.stage=InvoiceStage.RECEIVED,WorkflowAction.REVIEW,APPROVER))));
        c.add(new Check("workflow: cannot review approved",()->error("INVALID_TRANSITION",()->decide(f->f.stage=InvoiceStage.APPROVED,WorkflowAction.REVIEW,FINANCE))));
        c.add(new Check("workflow: ready submission",()->eq(decide(f-> {
        },WorkflowAction.SUBMIT,FINANCE).stage(),InvoiceStage.READY_FOR_APPROVAL)));
        c.add(new Check("workflow: basic validation gates submission",()->error("VALIDATION_FAILED",()->decide(f->f.basic=false,WorkflowAction.SUBMIT,FINANCE))));
        c.add(new Check("workflow: allocation gates submission",()->error("ALLOCATION_REQUIRED",()->decide(f->f.allocated=false,WorkflowAction.SUBMIT,FINANCE))));
        c.add(new Check("workflow: duplicate gates submission",()->error("DUPLICATE_UNRESOLVED",()->decide(f->f.duplicate=true,WorkflowAction.SUBMIT,FINANCE))));
        c.add(new Check("workflow: open questions gate submission",()->error("OPEN_QUESTIONS",()->decide(f->f.questions=1,WorkflowAction.SUBMIT,FINANCE))));
        c.add(new Check("workflow: evidence gates submission",()->error("EVIDENCE_REQUIRED",()->decide(f->f.accepted=false,WorkflowAction.SUBMIT,FINANCE))));
        c.add(new Check("workflow: optional evidence",()->eq(decide(f-> {
            f.required=false;f.accepted=false;
        },WorkflowAction.SUBMIT,FINANCE).stage(),InvoiceStage.READY_FOR_APPROVAL)));
        c.add(new Check("workflow: self-assignment blocked",()->error("SELF_APPROVAL",()->decide(f->f.assignee=CREATOR,WorkflowAction.SUBMIT,FINANCE))));
        c.add(new Check("workflow: approver cannot submit",()->error("FORBIDDEN",()->decide(f-> {
        },WorkflowAction.SUBMIT,APPROVER))));
        c.add(new Check("workflow: assigned approval",()-> {
            var d=decide(f->f.stage=InvoiceStage.READY_FOR_APPROVAL,WorkflowAction.APPROVE,APPROVER);eq(d.stage(),InvoiceStage.APPROVED);eq(d.approvalSlot(),1);
        }));
        c.add(new Check("workflow: unrelated admin cannot first-approve",()->error("NOT_ASSIGNED",()->decide(f->f.stage=InvoiceStage.READY_FOR_APPROVAL,WorkflowAction.APPROVE,ADMIN_ACTOR))));
        c.add(new Check("workflow: active delegation can approve",()->eq(POLICY.decide(facts(f->f.stage=InvoiceStage.READY_FOR_APPROVAL),WorkflowAction.APPROVE,ADMIN_ACTOR,true,"").stage(),InvoiceStage.APPROVED)));
        c.add(new Check("workflow: approval rejects viewer",()->error("FORBIDDEN",()->decide(f->f.stage=InvoiceStage.READY_FOR_APPROVAL,WorkflowAction.APPROVE,VIEWER))));
        c.add(new Check("workflow: creator admin cannot approve own upload",()->error("SELF_APPROVAL",()->decide(f-> {
            f.stage=InvoiceStage.READY_FOR_APPROVAL;f.creator=ADMIN;f.assignee=ADMIN;
        },WorkflowAction.APPROVE,ADMIN_ACTOR))));
        c.add(new Check("workflow: evidence checked again on approval",()->error("EVIDENCE_REQUIRED",()->decide(f-> {
            f.stage=InvoiceStage.READY_FOR_APPROVAL;f.accepted=false;
        },WorkflowAction.APPROVE,APPROVER))));
        c.add(new Check("workflow: high-value first approval does not finalize",()-> {
            var d=decide(f-> {
                f.stage=InvoiceStage.READY_FOR_APPROVAL;f.high=true;
            },WorkflowAction.APPROVE,APPROVER);eq(d.stage(),InvoiceStage.READY_FOR_APPROVAL);eq(d.event(),"FIRST_APPROVAL_RECORDED");
        }));
        c.add(new Check("workflow: high-value admin finalizes",()-> {
            var d=decide(f-> {
                f.stage=InvoiceStage.READY_FOR_APPROVAL;f.high=true;f.first=ASSIGNEE;
            },WorkflowAction.APPROVE,ADMIN_ACTOR);eq(d.stage(),InvoiceStage.APPROVED);eq(d.approvalSlot(),2);
        }));
        c.add(new Check("workflow: second signature needs admin",()->error("SECOND_APPROVAL_ADMIN_ONLY",()->decide(f-> {
            f.stage=InvoiceStage.READY_FOR_APPROVAL;f.high=true;f.first=ASSIGNEE;
        },WorkflowAction.APPROVE,APPROVER))));
        c.add(new Check("workflow: same admin cannot sign twice",()->error("DISTINCT_APPROVERS_REQUIRED",()->decide(f-> {
            f.stage=InvoiceStage.READY_FOR_APPROVAL;f.high=true;f.first=ADMIN;
        },WorkflowAction.APPROVE,ADMIN_ACTOR))));
        c.add(new Check("workflow: clarification requires reason",()->error("NOTE_REQUIRED",()->POLICY.decide(facts(f-> {
        }),WorkflowAction.CLARIFY,FINANCE,false," "))));
        c.add(new Check("workflow: assigned approver can clarify",()->eq(decide(f->f.stage=InvoiceStage.READY_FOR_APPROVAL,WorkflowAction.CLARIFY,APPROVER).stage(),InvoiceStage.CLARIFICATION)));
        c.add(new Check("workflow: unrelated approver cannot clarify",()->error("FORBIDDEN",()->decide(f->f.stage=InvoiceStage.READY_FOR_APPROVAL,WorkflowAction.CLARIFY,new Actor(TENANT,OTHER,"Other",Role.APPROVER)))));
        c.add(new Check("workflow: resolve needs all questions closed",()->error("OPEN_QUESTIONS",()->decide(f-> {
            f.stage=InvoiceStage.CLARIFICATION;f.questions=2;
        },WorkflowAction.RESOLVE,FINANCE))));
        c.add(new Check("workflow: clarification resolved",()->eq(decide(f->f.stage=InvoiceStage.CLARIFICATION,WorkflowAction.RESOLVE,FINANCE).stage(),InvoiceStage.REVIEWED)));
        c.add(new Check("workflow: rejection",()->eq(decide(f-> {
        },WorkflowAction.REJECT,FINANCE).stage(),InvoiceStage.REJECTED)));
        c.add(new Check("workflow: rejection needs reason",()->error("NOTE_REQUIRED",()->POLICY.decide(facts(f-> {
        }),WorkflowAction.REJECT,FINANCE,false,null))));
        c.add(new Check("workflow: reason length bounded",()->error("NOTE_REQUIRED",()->POLICY.decide(facts(f-> {
        }),WorkflowAction.CLARIFY,FINANCE,false,"x".repeat(2001)))));
        c.add(new Check("workflow: rejected can reopen",()->eq(decide(f->f.stage=InvoiceStage.REJECTED,WorkflowAction.REOPEN,FINANCE).stage(),InvoiceStage.REVIEWED)));
        c.add(new Check("workflow: approved is not rejectable",()->error("INVALID_TRANSITION",()->decide(f->f.stage=InvoiceStage.APPROVED,WorkflowAction.REJECT,FINANCE))));
        c.add(new Check("workflow: approved signed export",()->eq(decide(f-> {
            f.stage=InvoiceStage.APPROVED;f.first=ASSIGNEE;
        },WorkflowAction.EXPORT,FINANCE).stage(),InvoiceStage.EXPORTED)));
        c.add(new Check("workflow: unsigned export blocked",()->error("EXPORT_REQUIRES_APPROVAL",()->decide(f->f.stage=InvoiceStage.APPROVED,WorkflowAction.EXPORT,FINANCE))));
        c.add(new Check("workflow: second signature required for export",()->error("SECOND_APPROVAL_REQUIRED",()->decide(f-> {
            f.stage=InvoiceStage.APPROVED;f.first=ASSIGNEE;f.high=true;
        },WorkflowAction.EXPORT,FINANCE))));
        c.add(new Check("workflow: cannot export twice",()->error("EXPORT_REQUIRES_APPROVAL",()->decide(f-> {
            f.stage=InvoiceStage.EXPORTED;f.first=ASSIGNEE;
        },WorkflowAction.EXPORT,FINANCE))));
        c.add(new Check("workflow: viewer cannot export",()->error("FORBIDDEN",()->decide(f-> {
            f.stage=InvoiceStage.APPROVED;f.first=ASSIGNEE;
        },WorkflowAction.EXPORT,VIEWER))));
        c.add(new Check("roles: finance and admin permissions",()-> {
            yes(Role.ADMIN.managesFinance());yes(Role.FINANCE.managesFinance());no(Role.VIEWER.mayApprove());no(Role.FINANCE.mayApprove());yes(Role.ADMIN.mayApprove());
        }));
        c.add(new Check("money: exact decimal arithmetic",()->yes(MoneyRules.validTotals(MoneyRules.amount("0.10"),MoneyRules.amount("0.20"),MoneyRules.amount("0.30"),"EUR"))));
        c.add(new Check("money: amount scale normalized",()->eq(MoneyRules.amount("42"),new BigDecimal("42.00"))));
        c.add(new Check("money: fraction rejected",()->error("INVALID_AMOUNT",()->MoneyRules.amount("1.001"))));
        c.add(new Check("money: localized input rejected",()->error("INVALID_AMOUNT",()->MoneyRules.amount("1,20"))));
        c.add(new Check("money: scientific notation rejected",()->error("INVALID_AMOUNT",()->MoneyRules.amount("1e3"))));
        c.add(new Check("money: blank and null rejected",()-> {
            error("INVALID_AMOUNT",()->MoneyRules.amount(""));error("INVALID_AMOUNT",()->MoneyRules.amount(null));
        }));
        c.add(new Check("money: database precision bound",()-> {
            eq(MoneyRules.amount("999999999999.99"),new BigDecimal("999999999999.99"));error("INVALID_AMOUNT",()->MoneyRules.amount("1000000000000"));
        }));
        c.add(new Check("money: mismatched total",()->no(MoneyRules.validTotals(MoneyRules.amount("100"),MoneyRules.amount("19"),MoneyRules.amount("118"),"EUR"))));
        c.add(new Check("money: EUR-only",()->no(MoneyRules.validTotals(MoneyRules.amount("100"),MoneyRules.amount("19"),MoneyRules.amount("119"),"USD"))));
        c.add(new Check("money: negative and zero invoices blocked",()-> {
            no(MoneyRules.validTotals(MoneyRules.amount("-100"),MoneyRules.amount("19"),MoneyRules.amount("-81"),"EUR"));no(MoneyRules.validTotals(BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO,"EUR"));
        }));
        c.add(new Check("money: threshold inclusive",()-> {
            yes(MoneyRules.needsSecondApproval(MoneyRules.amount("10000"),MoneyRules.amount("10000")));no(MoneyRules.needsSecondApproval(MoneyRules.amount("9999.99"),MoneyRules.amount("10000")));
        }));
        c.add(new Check("money: invalid threshold rejected",()->error("INVALID_AMOUNT",()->MoneyRules.needsSecondApproval(BigDecimal.ONE,BigDecimal.ZERO))));
        c.add(new Check("duplicates: punctuation and case normalization",()->eq(DuplicateFingerprint.normalize(" Ab-2026 / 12 "),"ab202612")));
        c.add(new Check("duplicates: Unicode compatibility normalization",()->eq(DuplicateFingerprint.normalize("\uFF21\uFF22-12"),"ab12")));
        c.add(new Check("duplicates: different suppliers are distinct",()->no(DuplicateFingerprint.businessKey(CREATOR,"INV-1").equals(DuplicateFingerprint.businessKey(ADMIN,"INV-1")))));
        c.add(new Check("duplicates: standard SHA-256 vector",()->eq(DuplicateFingerprint.sha256(bytes("abc")),"ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad")));
        c.add(new Check("delegation: half-open interval",()-> {
            var start=Instant.parse("2026-10-01T00:00:00Z");var end=start.plusSeconds(3600);yes(DelegationPolicy.active(ASSIGNEE,ADMIN,ASSIGNEE,ADMIN,start,end,start));no(DelegationPolicy.active(ASSIGNEE,ADMIN,ASSIGNEE,ADMIN,start,end,end));no(DelegationPolicy.active(ASSIGNEE,ADMIN,ASSIGNEE,ADMIN,start,end,start.minusSeconds(1)));
        }));
        c.add(new Check("delegation: self delegation rejected",()->error("INVALID_DELEGATION",()->DelegationPolicy.validate(ADMIN,ADMIN,Instant.EPOCH,Instant.EPOCH.plusSeconds(1)))));
        c.add(new Check("delegation: invalid interval rejected",()->error("INVALID_DELEGATION",()->DelegationPolicy.validate(ASSIGNEE,ADMIN,Instant.EPOCH,Instant.EPOCH))));
        c.add(new Check("reminder: Berlin midnight is previous UTC date",()->eq(ReminderPolicy.businessDate(Instant.parse("2026-06-01T22:30:00Z")),LocalDate.of(2026,6,2))));
        c.add(new Check("reminder: DST winter boundary",()->eq(ReminderPolicy.businessDate(Instant.parse("2026-10-25T23:30:00Z")),LocalDate.of(2026,10,26))));
        c.add(new Check("reminder: due horizon inclusive",()-> {
            var now=Instant.parse("2026-10-18T12:00:00Z");yes(ReminderPolicy.due(InvoiceStage.READY_FOR_APPROVAL,LocalDate.of(2026,10,21),now));no(ReminderPolicy.due(InvoiceStage.READY_FOR_APPROVAL,LocalDate.of(2026,10,22),now));yes(ReminderPolicy.due(InvoiceStage.READY_FOR_APPROVAL,LocalDate.of(2026,10,1),now));no(ReminderPolicy.due(InvoiceStage.APPROVED,LocalDate.of(2026,10,18),now));
        }));
        c.add(new Check("CSV: quotes and multiline escaped",()->eq(SafeCsv.cell("a\"b\nc"),"\"a\"\"b\nc\"")));
        c.add(new Check("CSV: formula prefixes neutralized",()-> {
            for(String value:List.of("=1+1","+SUM(A1)","-2","@cmd","\t=SUM(A1)","  =1"))yes(SafeCsv.cell(value).startsWith("\"'"));
        }));
        c.add(new Check("CSV: BOM, delimiter and CRLF",()->eq(new String(SafeCsv.encode(List.of(List.of("a","b"))),StandardCharsets.UTF_8),"\uFEFF\"a\";\"b\"\r\n")));
        c.add(new Check("files: original XML",()->eq(FilePolicy.validate("invoice.xml",bytes("<?xml version=\"1.0\"?><a/>"),true),"application/xml")));
        c.add(new Check("files: PDF signature",()->eq(FilePolicy.validate("invoice.PDF",bytes("%PDF-1.7\nexample"),true),"application/pdf")));
        c.add(new Check("files: mislabeled executable rejected",()->error("UNSUPPORTED_FILE",()->FilePolicy.validate("invoice.pdf",bytes("MZ executable"),true))));
        c.add(new Check("files: traversal rejected",()->error("INVALID_FILE",()->FilePolicy.validate("../invoice.xml",bytes("<a/>"),true))));
        c.add(new Check("files: control filename rejected",()->error("INVALID_FILE",()->FilePolicy.validate("a\nb.xml",bytes("<a/>"),true))));
        c.add(new Check("files: PNG allowed only as evidence",()-> {
            byte[] png= {
                (byte)137,80,78,71,13,10,26,10
            };eq(FilePolicy.validate("evidence.png",png,false),"image/png");error("UNSUPPORTED_FILE",()->FilePolicy.validate("original.png",png,true));
        }));
        c.add(new Check("files: empty or oversized rejected",()-> {
            error("INVALID_FILE",()->FilePolicy.validate("a.pdf",new byte[0],true));error("INVALID_FILE",()->FilePolicy.validate("a.pdf",new byte[FilePolicy.MAX_BYTES+1],true));
        }));
        c.add(new Check("XML: UBL extraction",()-> {
            var x=new InvoiceXmlParser().parse(bytes(fixture("invoice-ubl.xml")));eq(x.format(),"UBL");eq(x.number(),"DEMO-2026-9001");eq(x.gross(),new BigDecimal("1190.00"));yes(x.checks().isEmpty());
        }));
        c.add(new Check("XML: CII extraction",()-> {
            var x=new InvoiceXmlParser().parse(bytes(fixture("invoice-cii.xml")));eq(x.format(),"CII");eq(x.dueDate(),LocalDate.of(2026,10,31));yes(x.checks().isEmpty());
        }));
        c.add(new Check("XML: missing due date is recoverable",()->yes(new InvoiceXmlParser().parse(bytes(fixture("invoice-ubl.xml").replace("<cbc:DueDate>2026-10-31</cbc:DueDate>",""))).checks().contains("DUE_DATE_REQUIRED"))));
        c.add(new Check("XML: before-issue due date flagged",()->yes(new InvoiceXmlParser().parse(bytes(fixture("invoice-ubl.xml").replace("2026-10-31","2026-09-30"))).checks().contains("DUE_BEFORE_ISSUE"))));
        c.add(new Check("XML: inconsistent totals flagged",()->yes(new InvoiceXmlParser().parse(bytes(fixture("invoice-ubl.xml").replace(">190.00<",">191.00<"))).checks().contains("TOTALS_OR_CURRENCY_INVALID"))));
        c.add(new Check("XML: wrong namespace rejected",()->error("UNSUPPORTED_XML",()->new InvoiceXmlParser().parse(bytes("<Invoice><ID>1</ID></Invoice>")))));
        c.add(new Check("XML: malformed rejected",()->error("INVALID_XML",()->new InvoiceXmlParser().parse(bytes("<Invoice>")))));
        c.add(new Check("XML: external entity forbidden",()->error("INVALID_XML",()->new InvoiceXmlParser().parse(bytes("<!DOCTYPE a [<!ENTITY x SYSTEM 'file:///etc/passwd'>]><a>&x;</a>")))));
        c.add(new Check("XML: entity expansion forbidden",()->error("INVALID_XML",()->new InvoiceXmlParser().parse(bytes("<!DOCTYPE a [<!ENTITY x 'b'><!ENTITY y '&x;&x;'>]><a>&y;</a>")))));
        c.add(new Check("XML: credit note unsupported",()->error("UNSUPPORTED_INVOICE_TYPE",()->new InvoiceXmlParser().parse(bytes(fixture("invoice-ubl.xml").replace(">380<",">381<"))))));
        c.add(new Check("XML: prepayment unsupported",()->error("UNSUPPORTED_PREPAYMENT",()->new InvoiceXmlParser().parse(bytes(fixture("invoice-ubl.xml").replace("<cbc:PayableAmount currencyID=\"EUR\">1190.00","<cbc:PayableAmount currencyID=\"EUR\">1000.00"))))));
        c.add(new Check("XML: decimal scale bounded",()->error("INVALID_AMOUNT",()->new InvoiceXmlParser().parse(bytes(fixture("invoice-ubl.xml").replace("190.00","190.001"))))));
        return List.copyOf(c);
    }
    public static void main(String[] args) {
        int failures=0;
        var tests=cases();
        for(Check c:tests) {
            try {
                c.body().run();
                System.out.println("PASS "+c.name());
            } catch(Throwable t) {
                failures++;
                System.err.println("FAIL "+c.name()+": "+t);
            }
        }
        System.out.println("RESULT "+(tests.size()-failures)+"/"+tests.size()+" passed");
        if(failures>0)System.exit(1);
    }
}
