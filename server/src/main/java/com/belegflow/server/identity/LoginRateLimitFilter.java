package com.belegflow.server.identity;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;
/** Bounded single-instance login throttling. Configure edge limits for distributed deployment. */
@Component @Order(-110) public class LoginRateLimitFilter extends OncePerRequestFilter {
    private record Bucket(long window,int attempts) {
    }
    private final ConcurrentHashMap<String,Bucket> buckets=new ConcurrentHashMap<>();
    private final Clock clock;
    public LoginRateLimitFilter(Clock clock) {
        this.clock=clock;
    }
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException {
        if("POST".equals(request.getMethod())&&"/api/login".equals(request.getServletPath())) {
            long window=clock.millis()/600000L;
            if(buckets.size()>10000)buckets.entrySet().removeIf(e->e.getValue().window()<window);
            if(buckets.size()>10000) {
                response.sendError(429);
                return;
            }
            // Never trust a user-supplied X-Forwarded-For header here.
            Bucket b=buckets.compute(request.getRemoteAddr(),(k,old)->old==null||old.window()!=window?new Bucket(window,1):new Bucket(window,old.attempts()+1));
            if(b.attempts()>60) {
                response.setStatus(429);
                response.setHeader("Retry-After","600");
                response.setContentType("application/json");
                response.getWriter().write("{\"code\":\"RATE_LIMITED\"}");
                return;
            }
        }
        chain.doFilter(request,response);
    }
}
