package com.belegflow.domain;

import java.util.*;

/** Exhaustive role/stage/action matrix for an assigned, independent actor and otherwise valid invoice.
 * Expected transitions are a manually specified contract, not derived from WorkflowPolicy itself.
 * Edge cases such as delegation, missing evidence and dual signatures live in DomainChecks.
 */
public final class PolicyMatrixChecks {
    private PolicyMatrixChecks() {}
    public static List<DomainChecks.Check> cases() {
        var result=new ArrayList<DomainChecks.Check>();
        UUID tenant=UUID.fromString("10000000-0000-0000-0000-000000000001");
        UUID actorId=UUID.fromString("20000000-0000-0000-0000-000000000002");
        UUID creatorId=UUID.fromString("20000000-0000-0000-0000-000000000001");
        for(var role:Role.values()) for(var stage:InvoiceStage.values()) for(var action:WorkflowAction.values()) {
            String name="matrix: "+role+" / "+stage+" / "+action;
            result.add(new DomainChecks.Check(name,()->{
                var actor=new Actor(tenant,actorId,"Independent assigned actor",role);
                var facts=new WorkflowFacts(stage,creatorId,actorId,true,true,false,true,true,0,false,
                    stage==InvoiceStage.APPROVED || stage==InvoiceStage.EXPORTED ? actorId : null,null);
                InvoiceStage expected=expected(role,stage,action);
                try {
                    var decision=new WorkflowPolicy().decide(facts,action,actor,false,"Matrix reason");
                    if(expected==null) throw new AssertionError("Forbidden transition was allowed: "+name);
                    if(decision.stage()!=expected) throw new AssertionError("Expected "+expected+", got "+decision.stage());
                    if(decision.event()==null || decision.event().isBlank()) throw new AssertionError("Missing audit event");
                } catch(BusinessException e) {
                    if(expected!=null) throw new AssertionError("Allowed transition failed: "+name+" / "+e.code(),e);
                }
            }));
        }
        return List.copyOf(result);
    }
    private static InvoiceStage expected(Role role,InvoiceStage stage,WorkflowAction action) {
        boolean finance=role==Role.FINANCE || role==Role.ADMIN;
        boolean decide=role==Role.APPROVER || role==Role.ADMIN;
        boolean open=EnumSet.of(InvoiceStage.RECEIVED,InvoiceStage.REVIEWED,InvoiceStage.CLARIFICATION,InvoiceStage.READY_FOR_APPROVAL).contains(stage);
        return switch(action) {
            case REVIEW -> finance && stage==InvoiceStage.RECEIVED ? InvoiceStage.REVIEWED : null;
            case SUBMIT -> finance && stage==InvoiceStage.REVIEWED ? InvoiceStage.READY_FOR_APPROVAL : null;
            case CLARIFY -> (finance || decide) && open ? InvoiceStage.CLARIFICATION : null;
            case RESOLVE -> finance && stage==InvoiceStage.CLARIFICATION ? InvoiceStage.REVIEWED : null;
            case APPROVE -> decide && stage==InvoiceStage.READY_FOR_APPROVAL ? InvoiceStage.APPROVED : null;
            case REJECT -> (finance || decide) && open ? InvoiceStage.REJECTED : null;
            case REOPEN -> finance && stage==InvoiceStage.REJECTED ? InvoiceStage.REVIEWED : null;
            case EXPORT -> finance && stage==InvoiceStage.APPROVED ? InvoiceStage.EXPORTED : null;
        };
    }
}
