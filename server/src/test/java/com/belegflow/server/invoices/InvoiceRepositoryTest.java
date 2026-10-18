package com.belegflow.server.invoices;
import com.belegflow.domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InvoiceRepositoryTest {
    final JdbcTemplate db=mock(JdbcTemplate.class);final InvoiceRepository repo=new InvoiceRepository(db);
    final UUID tenant=UUID.randomUUID(),id=UUID.randomUUID(),owner=UUID.randomUUID();
    @Test void lockedLookupScopesAndLocksOnlyInvoice(){when(db.queryForList(InvoiceRepository.SELECT+" WHERE i.tenant_id=? AND i.id=? FOR UPDATE OF i",tenant,id)).thenReturn(List.of(Map.of("id",id)));assertEquals(id,repo.get(tenant,id,true).get("id"));}
    @Test void missingReadLookupDoesNotRevealForeignEntity(){assertEquals("NOT_FOUND",assertThrows(BusinessException.class,()->repo.get(tenant,id,false)).code());}
    @Test void absentDelegationOwnerNeverQueriesDatabase(){assertFalse(repo.delegate(new Actor(tenant,id,"A",Role.ADMIN),null,Instant.EPOCH));verifyNoInteractions(db);}
    @Test void delegationUsesHalfOpenTimeRange(){var a=new Actor(tenant,id,"A",Role.ADMIN);Instant now=Instant.parse("2026-10-18T12:00:00Z");String sql="SELECT EXISTS(SELECT 1 FROM delegation WHERE tenant_id=? AND owner_id=? AND delegate_id=? AND active=true AND valid_from<=? AND valid_until>?)";var t=java.sql.Timestamp.from(now);when(db.queryForObject(sql,Boolean.class,tenant,owner,id,t,t)).thenReturn(true);assertTrue(repo.delegate(a,owner,now));}
    @Test void questionsAndEvidenceCountAreTenantScoped(){when(db.queryForObject("SELECT count(*) FROM discussion WHERE tenant_id=? AND invoice_id=? AND is_question=true AND resolved_at IS NULL",Integer.class,tenant,id)).thenReturn(2);when(db.queryForObject("SELECT count(*) FROM document WHERE tenant_id=? AND invoice_id=? AND kind='EVIDENCE'",Integer.class,tenant,id)).thenReturn(3);assertEquals(2,repo.questions(tenant,id));assertEquals(3,repo.evidenceCount(tenant,id));}
    @Test void staleVersionIsConflict(){assertDoesNotThrow(()->repo.version(Map.of("version",3),3));assertEquals("VERSION_CONFLICT",assertThrows(BusinessException.class,()->repo.version(Map.of("version",3),2)).code());}
}
