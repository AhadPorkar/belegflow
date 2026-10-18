package com.belegflow.server.identity;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.http.HttpMethod;
import java.util.Locale;
@Configuration public class SecurityConfiguration {
    @Bean PasswordEncoder passwordEncoder() {
        return Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }
    @Bean UserDetailsService users(JdbcTemplate db) {
        return email -> {
            var rows=db.queryForList("SELECT email,password_hash,role,active,auth_version FROM app_user WHERE email=?",email.strip().toLowerCase(Locale.ROOT));
            if(rows.isEmpty()) throw new UsernameNotFoundException("Invalid credentials");
            var u=rows.getFirst();
            return new SecurityPrincipal(u.get("email").toString(),u.get("password_hash").toString(),u.get("role").toString(),Boolean.TRUE.equals(u.get("active")),((Number)u.get("auth_version")).intValue());
        };
    }
    @Bean SecurityFilterChain security(HttpSecurity http) throws Exception {
        // The anonymous /api/csrf endpoint returns the session-bound token as JSON.
        // The SPA sends X-CSRF-TOKEN for every unsafe request, including login/logout.
        http.csrf(c->c.csrfTokenRepository(new HttpSessionCsrfTokenRepository()) .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()));
        http.authorizeHttpRequests(a->a .requestMatchers("/api/csrf","/api/public/config","/actuator/health").permitAll() .requestMatchers("/api/**").authenticated() .requestMatchers("/actuator/**").denyAll() .requestMatchers(HttpMethod.GET,"/","/index.html","/assets/**","/favicon.svg","/error").permitAll() .anyRequest().denyAll());
        http.formLogin(f->f.loginProcessingUrl("/api/login") .successHandler((request,response,authentication)->response.setStatus(204)) .failureHandler((request,response,exception)-> {
            response.setStatus(401); response.setContentType("application/json"); response.getWriter().write("{\"code\":\"INVALID_CREDENTIALS\"}");
        }).permitAll());
        http.logout(l->l.logoutUrl("/api/logout").invalidateHttpSession(true).clearAuthentication(true).deleteCookies("JSESSIONID") .logoutSuccessHandler((q,s,a)->s.setStatus(204)));
        http.exceptionHandling(e->e.authenticationEntryPoint((q,s,x)-> {
            s.setStatus(401);s.setContentType("application/json");s.getWriter().write("{\"code\":\"AUTH_REQUIRED\"}");
        }) .accessDeniedHandler((q,s,x)-> {
            s.setStatus(403);s.setContentType("application/json");s.getWriter().write("{\"code\":\"FORBIDDEN_OR_CSRF\"}");
        }));
        http.headers(h->h.contentSecurityPolicy(c->c.policyDirectives("default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data: blob:; connect-src 'self'; object-src 'none'; base-uri 'none'; frame-ancestors 'none'; form-action 'self'")));
        return http.build();
    }
}
