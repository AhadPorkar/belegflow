package com.belegflow.server.identity;

import com.belegflow.domain.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IdentityModuleTest {
    final JdbcTemplate db=mock(JdbcTemplate.class);
    final UUID tenant=UUID.randomUUID(), id=UUID.randomUUID();
    final String email="admin@example.test";
    final String lookup="SELECT id,tenant_id,display_name,role,auth_version FROM app_user WHERE email=? AND active=true";
    @AfterEach void cleanup(){ SecurityContextHolder.clearContext(); }
    void authenticate(int version) {
        var p=new SecurityPrincipal(email,"hash","ADMIN",true,version);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(p,"unused",p.getAuthorities()));
    }
    Map<String,Object> membership(String role,int version){ return Map.of("id",id,"tenant_id",tenant,"display_name","Alex","role",role,"auth_version",version); }
    @Test void freshMembershipOverridesTheRoleInTheSession() {
        authenticate(3);when(db.queryForList(lookup,email)).thenReturn(List.of(membership("VIEWER",3)));
        var a=new IdentityService(db).actor();assertEquals(Role.VIEWER,a.role());assertEquals(tenant,a.tenantId());
    }
    @Test void changedAuthenticationVersionRevokesExistingSession() {
        authenticate(2);when(db.queryForList(lookup,email)).thenReturn(List.of(membership("ADMIN",3)));
        assertEquals("SESSION_REVOKED",assertThrows(BusinessException.class,()->new IdentityService(db).actor()).code());
    }
    @Test void disabledOrRemovedMembershipIsDenied() {
        authenticate(2);when(db.queryForList(lookup,email)).thenReturn(List.of());
        assertEquals("FORBIDDEN",assertThrows(BusinessException.class,()->new IdentityService(db).actor()).code());
    }
    @Test void unauthenticatedTokenNeverQueriesMembership() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(email,"wrong"));
        assertThrows(BusinessException.class,()->new IdentityService(db).actor());verifyNoInteractions(db);
    }
    @Test void approverLookupIsActiveRoleAndTenantScoped() {
        String sql="SELECT EXISTS(SELECT 1 FROM app_user WHERE tenant_id=? AND id=? AND active=true AND role IN ('ADMIN','APPROVER'))";
        when(db.queryForObject(sql,Boolean.class,tenant,id)).thenReturn(true);
        new IdentityService(db).approverExists(tenant,id);verify(db).queryForObject(sql,Boolean.class,tenant,id);
    }
    @Test void unknownApproverIsRejected() {
        assertEquals("INVALID_APPROVER",assertThrows(BusinessException.class,()->new IdentityService(db).approverExists(tenant,id)).code());
    }
    @Test void userLookupNormalizesEmailAndKeepsDisabledStatus() {
        String sql="SELECT email,password_hash,role,active,auth_version FROM app_user WHERE email=?";
        when(db.queryForList(sql,email)).thenReturn(List.of(Map.of("email",email,"password_hash","hash","role","ADMIN","active",false,"auth_version",7)));
        var result=new SecurityConfiguration().users(db).loadUserByUsername("  ADMIN@example.test  ");
        assertFalse(result.isEnabled());assertEquals(7,((SecurityPrincipal)result).authVersion());
        assertTrue(result.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_ADMIN")));
    }
    @Test void unknownLoginUsesGenericCredentialsFailure(){
        assertThrows(UsernameNotFoundException.class,()->new SecurityConfiguration().users(db).loadUserByUsername(email));
    }
    @Test void passwordEncoderDoesNotStorePlaintextAndVerifiesCorrectly() {
        var encoder=new SecurityConfiguration().passwordEncoder();String hash=encoder.encode("long-example-password");
        assertNotEquals("long-example-password",hash);assertTrue(encoder.matches("long-example-password",hash));assertFalse(encoder.matches("wrong",hash));
    }
    @Test void publicConfigurationDoesNotAccessDatabase(){
        var c=new SessionController(mock(IdentityService.class),db,false);
        assertEquals("en",c.config().get("defaultLocale"));assertEquals(false,c.config().get("demo"));verifyNoInteractions(db);
    }
    @Test void csrfResponseIncludesTokenAndHeaderName(){
        var c=new SessionController(mock(IdentityService.class),db,false);
        assertEquals(Map.of("token","token-value","headerName","X-CSRF-TOKEN"),c.csrf(new DefaultCsrfToken("X-CSRF-TOKEN","_csrf","token-value")));
    }
    @Test void invalidLocaleRejectedBeforeMembershipLookup(){
        var identity=mock(IdentityService.class);var c=new SessionController(identity,db,false);
        assertThrows(BusinessException.class,()->c.locale(new SessionController.LocaleBody("fr")));verifyNoInteractions(identity,db);
    }
    @Test void localeUpdateUsesTenantAndUser(){
        var identity=mock(IdentityService.class);when(identity.actor()).thenReturn(new Actor(tenant,id,"Alex",Role.ADMIN));
        new SessionController(identity,db,false).locale(new SessionController.LocaleBody("de"));
        verify(db).update("UPDATE app_user SET locale=? WHERE tenant_id=? AND id=?","de",tenant,id);
    }
    @Test void sessionReturnsPublicMembershipNotPasswordHash(){
        var identity=mock(IdentityService.class);when(identity.actor()).thenReturn(new Actor(tenant,id,"Alex",Role.ADMIN));
        when(db.queryForMap("SELECT name,second_approval_threshold FROM organization WHERE id=?",tenant)).thenReturn(Map.of("name","Example GmbH","second_approval_threshold",new java.math.BigDecimal("10000.00")));
        when(db.queryForMap("SELECT email,locale FROM app_user WHERE tenant_id=? AND id=?",tenant,id)).thenReturn(Map.of("email",email,"locale","en"));
        var result=new SessionController(identity,db,false).session();assertEquals("10000.00",result.get("threshold"));assertFalse(result.containsKey("password_hash"));assertEquals(id,result.get("id"));
    }
}
