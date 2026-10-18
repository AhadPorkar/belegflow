package com.belegflow.server.identity;
import com.belegflow.domain.*;
import com.belegflow.server.shared.Sql;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.util.*;
/** Re-read membership on every request, so a disabled account or changed role is effective immediately. */
@Service public class IdentityService {
    private final JdbcTemplate db;
    public IdentityService(JdbcTemplate db) {
        this.db=db;
    }
    public Actor actor() {
        var authentication=SecurityContextHolder.getContext().getAuthentication();
        BusinessException.require(authentication!=null && authentication.isAuthenticated(),"FORBIDDEN");
        var rows=db.queryForList("SELECT id,tenant_id,display_name,role,auth_version FROM app_user WHERE email=? AND active=true",authentication.getName());
        BusinessException.require(!rows.isEmpty(),"FORBIDDEN");
        var r=rows.getFirst();
        if(authentication.getPrincipal() instanceof SecurityPrincipal p) BusinessException.require(p.authVersion()==Sql.integer(r,"auth_version"),"SESSION_REVOKED");
        return new Actor(Sql.uuid(r,"tenant_id"),Sql.uuid(r,"id"),Sql.str(r,"display_name"),Role.valueOf(Sql.str(r,"role")));
    }
    public void approverExists(UUID tenant,UUID id) {
        BusinessException.require(id!=null && Boolean.TRUE.equals(db.queryForObject("SELECT EXISTS(SELECT 1 FROM app_user WHERE tenant_id=? AND id=? AND active=true AND role IN ('ADMIN','APPROVER'))",Boolean.class,tenant,id)),"INVALID_APPROVER");
    }
}
