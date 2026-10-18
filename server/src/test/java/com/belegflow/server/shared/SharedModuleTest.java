package com.belegflow.server.shared;
import com.belegflow.domain.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class SharedModuleTest {
    @Test void apiNormalizationUsesExactDecimalsAndIsoDates(){
        var m=new HashMap<String,Object>();m.put("amount",new BigDecimal("123456789012.34"));m.put("date",java.sql.Date.valueOf("2026-10-18"));m.put("time",Timestamp.from(Instant.EPOCH));m.put("optional",null);
        var result=Sql.api(m);assertEquals("123456789012.34",result.get("amount"));assertEquals("2026-10-18",result.get("date"));assertEquals("1970-01-01T00:00:00Z",result.get("time"));assertTrue(result.containsKey("optional"));assertInstanceOf(BigDecimal.class,m.get("amount"));
    }
    @Test void rowHelpersHandleMissingNullableValues(){assertNull(Sql.uuid(Map.of(),"id"));assertEquals("",Sql.str(Map.of(),"name"));assertFalse(Sql.bool(Map.of(),"flag"));assertNull(Sql.instant(Map.of(),"time"));assertEquals(3,Sql.integer(Map.of("n",3L),"n"));}
    @Test void missingSingleRowIsNotFound(){assertEquals("NOT_FOUND",assertThrows(BusinessException.class,()->Sql.one(List.of())).code());}
    @Test void stringsAreTrimmedBoundedAndOptionallyRequired(){assertEquals("",Sql.text(null,20,false));assertEquals("a",Sql.text(" a ",1,true));assertThrows(BusinessException.class,()->Sql.text("aa",1,false));assertThrows(BusinessException.class,()->Sql.text(" ",1,true));}
    @Test void businessErrorStatusMatrix(){var handler=new ApiErrors();Map<String,Integer> cases=Map.of("AUTH_REQUIRED",401,"SESSION_REVOKED",401,"NOT_FOUND",404,"VERSION_CONFLICT",409,"IDEMPOTENCY_CONFLICT",409,"FORBIDDEN",403,"SELF_APPROVAL",403,"EVIDENCE_REQUIRED",422);cases.forEach((code,status)->{var r=handler.business(new BusinessException(code));assertEquals(status,r.getStatusCode().value());assertEquals(Map.of("code",code),r.getBody());});}
    @Test void invalidInputAndOversizeHaveDifferentStatuses(){var handler=new ApiErrors();assertEquals(400,handler.validation(new IllegalArgumentException("private data")).getStatusCode().value());assertEquals(413,handler.size(new RuntimeException()).getStatusCode().value());}
    @Test void sqlConflictDoesNotLeakDatabaseMessage(){var r=new ApiErrors().conflict(new DataIntegrityViolationException("private invoice"));assertEquals(409,r.getStatusCode().value());assertFalse(r.getBody().toString().contains("private invoice"));}
    @Test void unexpectedErrorReturnsOnlyReferenceAndGenericCode(){var r=new ApiErrors().unexpected(new RuntimeException("secret contents"));assertEquals(500,r.getStatusCode().value());var body=(Map<?,?>)r.getBody();assertEquals("UNEXPECTED_ERROR",body.get("code"));assertDoesNotThrow(()->UUID.fromString(body.get("reference").toString()));assertFalse(body.toString().contains("secret contents"));}
}
