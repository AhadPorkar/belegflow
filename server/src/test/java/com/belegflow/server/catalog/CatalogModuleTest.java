package com.belegflow.server.catalog;
import com.belegflow.domain.*;
import com.belegflow.server.support.ServiceFixture;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CatalogModuleTest extends ServiceFixture {
    final CatalogService service=new CatalogService(db,identity,audit,encoder);
    @Test void createsNormalizedSupplierWithTenantAndAudit(){
        UUID id=service.supplier(null,new CatalogService.SupplierBody("  North-Works  ","sales@example.test","DE123",true));
        assertEquals(tenant,arg("INSERT INTO supplier",2));assertEquals("North-Works",arg("INSERT INTO supplier",3));assertEquals("northworks",arg("INSERT INTO supplier",4));
        verify(audit).append(actor,null,"SUPPLIER_SAVED",id.toString());
    }
    @Test void supplierUpdateMustAffectTheCurrentTenant(){
        UUID id=UUID.randomUUID();when(db.update(anyString(),any(Object[].class))).thenReturn(0);
        assertEquals("NOT_FOUND",assertThrows(BusinessException.class,()->service.supplier(id,new CatalogService.SupplierBody("Vendor","","",true))).code());verifyNoInteractions(audit);
    }
    @Test void punctuationOnlySupplierNameIsRejected(){
        assertThrows(BusinessException.class,()->service.supplier(null,new CatalogService.SupplierBody("---","","",true)));assertTrue(writes().isEmpty());
    }
    @Test void projectChecksManagerAndPersistsEvidencePolicy(){
        UUID id=service.project(new CatalogService.ProjectBody("PRJ-1","Warehouse",approverId,true));
        verify(identity).approverExists(tenant,approverId);assertEquals(true,arg("INSERT INTO project",6));verify(audit).append(actor,null,"PROJECT_CREATED",id.toString());
    }
    @Test void costCenterHasBoundedCodeAndName(){
        assertThrows(BusinessException.class,()->service.costCenter(new CatalogService.CostCenterBody(" ","Office")));
        assertTrue(writes().isEmpty());
    }
    @Test void createsCostCenterWithAudit(){
        UUID id=service.costCenter(new CatalogService.CostCenterBody(" CC-1 "," Office "));
        assertEquals("CC-1",arg("INSERT INTO cost_center",3));verify(audit).append(actor,null,"COST_CENTER_CREATED",id.toString());
    }
    @Test void administratorCanSetExactMoneyThreshold(){
        as(Role.ADMIN);service.rules(new CatalogService.RuleBody("1234.56"));assertEquals("1234.56",arg("UPDATE organization",1).toString());verify(audit).append(actor,null,"APPROVAL_RULE_CHANGED","1234.56");
    }
    @Test void financeCannotAlterRules(){assertThrows(BusinessException.class,()->service.rules(new CatalogService.RuleBody("123.00")));assertTrue(writes().isEmpty());}
    @Test void delegationChecksBothMembershipsAndStoresExactInstants(){
        as(Role.ADMIN);Instant from=clock.instant(),until=from.plusSeconds(3600);
        UUID id=service.delegate(new CatalogService.DelegationBody(approverId,actorId,from,until));
        verify(identity).approverExists(tenant,approverId);verify(identity).approverExists(tenant,actorId);
        assertEquals(java.sql.Timestamp.from(from),arg("INSERT INTO delegation",5));verify(audit).append(actor,null,"DELEGATION_CREATED",id.toString());
    }
    @Test void selfDelegationDoesNotWrite(){as(Role.ADMIN);assertThrows(BusinessException.class,()->service.delegate(new CatalogService.DelegationBody(actorId,actorId,clock.instant(),clock.instant().plusSeconds(1))));assertTrue(writes().isEmpty());}
    @Test void revokeIsTenantScopedAndAudited(){as(Role.ADMIN);UUID id=UUID.randomUUID();service.revoke(id);verify(db).update("UPDATE delegation SET active=false WHERE tenant_id=? AND id=?",tenant,id);verify(audit).append(actor,null,"DELEGATION_REVOKED",id.toString());}
    @Test void weakPasswordDoesNotCreateUser(){as(Role.ADMIN);assertThrows(BusinessException.class,()->service.user(null,new CatalogService.UserBody("user@example.test","User",Role.APPROVER,"short",true)));assertTrue(writes().isEmpty());}
    @Test void createsUserWithHashAndNormalizedEmail(){
        as(Role.ADMIN);when(encoder.encode("a-long-random-password")).thenReturn("encoded-hash");
        UUID id=service.user(null,new CatalogService.UserBody("USER@example.test","User",Role.APPROVER,"a-long-random-password",true));
        assertEquals("user@example.test",arg("INSERT INTO app_user",3));assertEquals("encoded-hash",arg("INSERT INTO app_user",6));verify(audit).append(actor,null,"USER_SAVED",id.toString());
    }
    @Test void cannotDisableOrDemoteSelf(){as(Role.ADMIN);assertEquals("CANNOT_DISABLE_SELF",assertThrows(BusinessException.class,()->service.user(actorId,new CatalogService.UserBody("a@example.test","A",Role.FINANCE,"",true))).code());assertTrue(writes().isEmpty());}
    @Test void userUpdateInvalidatesOldSessionsWithoutRehashingBlankPassword(){
        as(Role.ADMIN);UUID id=UUID.randomUUID();service.user(id,new CatalogService.UserBody("a@example.test","A",Role.VIEWER,"",true));
        assertTrue(write("UPDATE app_user").getArgument(0,String.class).contains("auth_version=auth_version+1"));verifyNoInteractions(encoder);
    }
    @Test void readOnlyUsersCannotCreateCatalogRecords(){
        as(Role.VIEWER);assertThrows(BusinessException.class,()->service.supplier(null,new CatalogService.SupplierBody("A","","",true)));assertThrows(BusinessException.class,()->service.project(new CatalogService.ProjectBody("P","P",approverId,false)));assertThrows(BusinessException.class,()->service.costCenter(new CatalogService.CostCenterBody("C","C")));assertTrue(writes().isEmpty());
    }
    @Test void missingUserCannotBeUpdated(){
        as(Role.ADMIN);when(db.update(anyString(),any(Object[].class))).thenReturn(0);
        assertEquals("NOT_FOUND",assertThrows(BusinessException.class,()->service.user(UUID.randomUUID(),new CatalogService.UserBody("a@example.test","A",Role.VIEWER,"",true))).code());
    }
}
