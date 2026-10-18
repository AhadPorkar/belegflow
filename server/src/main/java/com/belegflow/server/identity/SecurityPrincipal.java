package com.belegflow.server.identity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.authority.AuthorityUtils;
public final class SecurityPrincipal extends User {
    private final int authVersion;
    public SecurityPrincipal(String email,String hash,String role,boolean active,int authVersion) {
        super(email,hash,active,true,true,true,AuthorityUtils.createAuthorityList("ROLE_"+role));
        this.authVersion=authVersion;
    }
    public int authVersion() {
        return authVersion;
    }
}
