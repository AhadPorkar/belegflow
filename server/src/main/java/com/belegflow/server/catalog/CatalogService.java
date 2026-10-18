package com.belegflow.server.catalog;
import com.belegflow.domain.*;
import com.belegflow.server.audit.AuditService;
import com.belegflow.server.identity.IdentityService;
import com.belegflow.server.shared.Sql;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.time.Instant;
import java.sql.Timestamp;
@Service public class CatalogService {
    private final JdbcTemplate db;
    private final IdentityService identity;
    private final AuditService audit;
    private final PasswordEncoder passwords;
    public CatalogService(JdbcTemplate db,IdentityService identity,AuditService audit,PasswordEncoder passwords) {
        this.db=db;
        this.identity=identity;
        this.audit=audit;
        this.passwords=passwords;
    }
    public Map<String,Object> all() {
        var a=identity.actor();
        var t=a.tenantId();
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("suppliers",Sql.api(db.queryForList("SELECT id,name,email,vat_id,active FROM supplier WHERE tenant_id=? ORDER BY name",t)));
        result.put("projects",Sql.api(db.queryForList("SELECT p.id,p.code,p.name,p.manager_id,p.evidence_required,p.active,u.display_name AS manager_name FROM project p JOIN app_user u ON u.tenant_id=p.tenant_id AND u.id=p.manager_id WHERE p.tenant_id=? ORDER BY p.name",t)));
        result.put("cost_centers",Sql.api(db.queryForList("SELECT id,code,name,active FROM cost_center WHERE tenant_id=? ORDER BY code",t)));
        result.put("users",Sql.api(db.queryForList("SELECT id,email,display_name,role,active FROM app_user WHERE tenant_id=? ORDER BY display_name",t)));
        result.put("delegations",Sql.api(db.queryForList("SELECT d.id,d.owner_id,d.delegate_id,d.valid_from,d.valid_until,d.active,u.display_name AS owner_name,v.display_name AS delegate_name FROM delegation d JOIN app_user u ON u.tenant_id=d.tenant_id AND u.id=d.owner_id JOIN app_user v ON v.tenant_id=d.tenant_id AND v.id=d.delegate_id WHERE d.tenant_id=? ORDER BY d.valid_from DESC",t)));
        result.put("threshold",db.queryForObject("SELECT second_approval_threshold FROM organization WHERE id=?",String.class,t));
        return result;
    }
    public record SupplierBody(String name,String email,String vat_id,boolean active) {
    }
    @Transactional public UUID supplier(UUID id,SupplierBody b) {
        var a=identity.actor();
        a.finance();
        String name=Sql.text(b.name(),200,true),email=Sql.text(b.email(),254,false),vat=Sql.text(b.vat_id(),40,false);
        String normalized=DuplicateFingerprint.normalize(name);
        BusinessException.require(!normalized.isBlank(),"INVALID_INPUT");
        if(id==null) {
            id=UUID.randomUUID();
            db.update("INSERT INTO supplier(id,tenant_id,name,normalized_name,email,vat_id,active) VALUES(?,?,?,?,?,?,?)",id,a.tenantId(),name,normalized,email,vat,b.active());
        } else BusinessException.require(db.update("UPDATE supplier SET name=?,normalized_name=?,email=?,vat_id=?,active=? WHERE tenant_id=? AND id=?",name,normalized,email,vat,b.active(),a.tenantId(),id)==1,"NOT_FOUND");
        audit.append(a,null,"SUPPLIER_SAVED",id.toString());
        return id;
    }
    public record ProjectBody(String code,String name,UUID manager_id,boolean evidence_required) {
    }
    @Transactional public UUID project(ProjectBody b) {
        var a=identity.actor();
        a.finance();
        identity.approverExists(a.tenantId(),b.manager_id());
        UUID id=UUID.randomUUID();
        db.update("INSERT INTO project(id,tenant_id,code,name,manager_id,evidence_required) VALUES (?,?,?,?,?,?)",id,a.tenantId(),Sql.text(b.code(),40,true),Sql.text(b.name(),160,true),b.manager_id(),b.evidence_required());
        audit.append(a,null,"PROJECT_CREATED",id.toString());
        return id;
    }
    public record CostCenterBody(String code,String name) {
    }
    @Transactional public UUID costCenter(CostCenterBody b) {
        var a=identity.actor();
        a.finance();
        UUID id=UUID.randomUUID();
        db.update("INSERT INTO cost_center(id,tenant_id,code,name) VALUES (?,?,?,?)",id,a.tenantId(),Sql.text(b.code(),40,true),Sql.text(b.name(),160,true));
        audit.append(a,null,"COST_CENTER_CREATED",id.toString());
        return id;
    }
    public record RuleBody(String threshold) {
    }
    @Transactional public void rules(RuleBody b) {
        var a=identity.actor();
        a.admin();
        var threshold=MoneyRules.amount(b.threshold());
        BusinessException.require(threshold.signum()>0,"INVALID_AMOUNT");
        db.update("UPDATE organization SET second_approval_threshold=? WHERE id=?",threshold,a.tenantId());
        audit.append(a,null,"APPROVAL_RULE_CHANGED",threshold.toPlainString());
    }
    public record DelegationBody(UUID owner_id,UUID delegate_id,Instant valid_from,Instant valid_until) {
    }
    @Transactional public UUID delegate(DelegationBody b) {
        var a=identity.actor();
        a.admin();
        DelegationPolicy.validate(b.owner_id(),b.delegate_id(),b.valid_from(),b.valid_until());
        identity.approverExists(a.tenantId(),b.owner_id());
        identity.approverExists(a.tenantId(),b.delegate_id());
        UUID id=UUID.randomUUID();
        db.update("INSERT INTO delegation(id,tenant_id,owner_id,delegate_id,valid_from,valid_until) VALUES(?,?,?,?,?,?)",id,a.tenantId(),b.owner_id(),b.delegate_id(),Timestamp.from(b.valid_from()),Timestamp.from(b.valid_until()));
        audit.append(a,null,"DELEGATION_CREATED",id.toString());
        return id;
    }
    @Transactional public void revoke(UUID id) {
        var a=identity.actor();
        a.admin();
        BusinessException.require(db.update("UPDATE delegation SET active=false WHERE tenant_id=? AND id=?",a.tenantId(),id)==1,"NOT_FOUND");
        audit.append(a,null,"DELEGATION_REVOKED",id.toString());
    }
    public record UserBody(String email,String display_name,Role role,String password,boolean active) {
    }
    @Transactional public UUID user(UUID id,UserBody b) {
        var a=identity.actor();
        a.admin();
        db.queryForObject("SELECT id FROM organization WHERE id=? FOR UPDATE",UUID.class,a.tenantId());
        String email=Sql.text(b.email(),254,true).toLowerCase(Locale.ROOT),name=Sql.text(b.display_name(),120,true);
        BusinessException.require(email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+") && b.role()!=null,"INVALID_INPUT");
        if(id!=null && id.equals(a.userId())) BusinessException.require(b.active() && b.role()==Role.ADMIN,"CANNOT_DISABLE_SELF");
        String password=b.password()==null?"":b.password();
        if(id==null || !password.isEmpty()) BusinessException.require(password.length()>=14 && password.length()<=128,"WEAK_PASSWORD");
        if(id==null) {
            id=UUID.randomUUID();
            db.update("INSERT INTO app_user(id,tenant_id,email,display_name,role,password_hash,active) VALUES(?,?,?,?,?,?,?)",id,a.tenantId(),email,name,b.role().name(),passwords.encode(password),b.active());
        } else {
            BusinessException.require(db.update("UPDATE app_user SET email=?,display_name=?,role=?,active=?,auth_version=auth_version+1 WHERE tenant_id=? AND id=?",email,name,b.role().name(),b.active(),a.tenantId(),id)==1,"NOT_FOUND");
            if(!password.isEmpty()) db.update("UPDATE app_user SET password_hash=? WHERE tenant_id=? AND id=?",passwords.encode(password),a.tenantId(),id);
        }
        audit.append(a,null,"USER_SAVED",id.toString());
        return id;
    }
}
