package com.belegflow.server.identity;
import com.belegflow.server.shared.Sql;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Value;
import java.util.*;
@RestController @RequestMapping("/api") public class SessionController {
    private final IdentityService identity;
    private final JdbcTemplate db;
    private final boolean demo;
    public SessionController(IdentityService identity,JdbcTemplate db,@Value("${bf.demo-enabled}") boolean demo) {
        this.identity=identity;
        this.db=db;
        this.demo=demo;
    }
    @GetMapping("/csrf") public Map<String,String> csrf(CsrfToken token) {
        return Map.of("token",token.getToken(),"headerName",token.getHeaderName());
    }
    @GetMapping("/public/config") public Map<String,Object> config() {
        return Map.of("demo",demo,"version","0.2.1","defaultLocale","en");
    }
    @GetMapping("/session") public Map<String,Object> session() {
        var a=identity.actor();
        var o=db.queryForMap("SELECT name,second_approval_threshold FROM organization WHERE id=?",a.tenantId());
        var u=db.queryForMap("SELECT email,locale FROM app_user WHERE tenant_id=? AND id=?",a.tenantId(),a.userId());
        return Map.of("id",a.userId(),"tenant_id",a.tenantId(),"name",a.name(),"role",a.role(),"organization",o.get("name"),"email",u.get("email"),"locale",u.get("locale"),"threshold",o.get("second_approval_threshold").toString(),"demo",demo);
    }
    public record LocaleBody(String locale) {
    }
    @PutMapping("/session/locale") public void locale(@RequestBody LocaleBody body) {
        com.belegflow.domain.BusinessException.require(body.locale()!=null && Set.of("en","de").contains(body.locale()),"INVALID_INPUT");
        var a=identity.actor();
        db.update("UPDATE app_user SET locale=? WHERE tenant_id=? AND id=?",body.locale(),a.tenantId(),a.userId());
    }
}
