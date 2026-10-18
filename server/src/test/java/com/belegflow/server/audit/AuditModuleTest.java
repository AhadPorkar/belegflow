package com.belegflow.server.audit;
import com.belegflow.domain.*;
import com.belegflow.server.support.ServiceFixture;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuditModuleTest extends ServiceFixture {
    final String query="SELECT e.id,e.invoice_id,e.actor_name,e.event_type,e.detail,e.created_at,i.invoice_number FROM audit_event e LEFT JOIN invoice i ON i.tenant_id=e.tenant_id AND i.id=e.invoice_id WHERE e.tenant_id=? AND (?=0 OR e.id<?) ORDER BY e.id DESC LIMIT 100";
    @Test void emptyAuditHasZeroNextCursor(){when(db.queryForList(query,tenant,0L,0L)).thenReturn(List.of());assertEquals(0,new AuditController(identity,db).events(0).get("next"));}
    @Test void nextCursorIsLastReturnedEventNotFirst(){when(db.queryForList(query,tenant,90L,90L)).thenReturn(List.of(Map.of("id",89L,"event_type","A"),Map.of("id",50L,"event_type","B")));assertEquals(50L,new AuditController(identity,db).events(90).get("next"));}
    @Test void negativeCursorRejected(){assertThrows(BusinessException.class,()->new AuditController(identity,db).events(-1));verifyNoInteractions(db);}
    @Test void nullDetailBecomesEmptyTextAndActorIsPreserved(){new AuditService(db).append(actor,invoiceId,"EVENT",null);assertEquals("",arg("INSERT INTO audit_event",6));assertEquals(actorId,arg("INSERT INTO audit_event",3));}
}
