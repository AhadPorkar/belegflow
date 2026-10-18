package com.belegflow.server.bootstrap;
import com.belegflow.domain.*;
import com.belegflow.server.documents.*;
import com.belegflow.server.support.ServiceFixture;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BootstrapModuleTest extends ServiceFixture {
    BootstrapData service(boolean demo,String email,String password,String org){return new BootstrapData(db,encoder,mock(InvoiceImportService.class),mock(DocumentService.class),audit,clock,demo,email,password,org);}
    @Test void existingOrganizationsNeverGetReseeded(){when(db.queryForObject("SELECT count(*) FROM organization",Long.class)).thenReturn(1L);service(true,"","","").run(null);assertTrue(writes().isEmpty());verifyNoInteractions(encoder,audit);}
    @Test void productionRequiresExplicitBootstrapCredentials(){when(db.queryForObject("SELECT count(*) FROM organization",Long.class)).thenReturn(0L);assertEquals("BOOTSTRAP_CONFIGURATION_REQUIRED",assertThrows(BusinessException.class,()->service(false,"","","").run(null)).code());assertTrue(writes().isEmpty());}
    @Test void productionRejectsPlaceholderPassword(){when(db.queryForObject("SELECT count(*) FROM organization",Long.class)).thenReturn(0L);assertThrows(BusinessException.class,()->service(false,"admin@example.test","change-me-very-long-password","Company").run(null));assertTrue(writes().isEmpty());}
    @Test void productionCreatesOnlyOrganizationAndHashedAdministrator(){
        when(db.queryForObject("SELECT count(*) FROM organization",Long.class)).thenReturn(0L);when(encoder.encode("a-real-long-unique-secret")).thenReturn("hash");
        service(false,"ADMIN@example.test","a-real-long-unique-secret","Example GmbH").run(null);assertEquals(2,writes().size());assertEquals("admin@example.test",arg("INSERT INTO app_user",3));assertEquals("hash",arg("INSERT INTO app_user",6));verify(audit).append(any(Actor.class),isNull(),eq("ORGANIZATION_CREATED"),eq("Example GmbH"));
    }
}
