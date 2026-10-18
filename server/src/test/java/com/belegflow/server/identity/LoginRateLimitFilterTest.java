package com.belegflow.server.identity;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import java.time.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LoginRateLimitFilterTest {
    MockHttpServletRequest request(String method,String ip) {
        var r=new MockHttpServletRequest(method,"/api/login");r.setServletPath("/api/login");r.setRemoteAddr(ip);return r;
    }
    @Test void sixtyAttemptsAllowedThenThrottled() throws Exception {
        var f=new LoginRateLimitFilter(Clock.fixed(Instant.EPOCH,ZoneOffset.UTC));var passed=new AtomicInteger();
        for(int i=0;i<61;i++) {
            var response=new MockHttpServletResponse();f.doFilter(request("POST","127.0.0.1"),response,(q,s)->passed.incrementAndGet());
            if(i==60){assertEquals(429,response.getStatus());assertEquals("600",response.getHeader("Retry-After"));assertTrue(response.getContentAsString().contains("RATE_LIMITED"));}
        }
        assertEquals(60,passed.get());
    }
    @Test void newWindowResetsTheCounter() throws Exception {
        Clock clock=mock(Clock.class);when(clock.millis()).thenReturn(0L);var f=new LoginRateLimitFilter(clock);
        for(int i=0;i<61;i++)f.doFilter(request("POST","127.0.0.1"),new MockHttpServletResponse(),(q,s)->{});
        when(clock.millis()).thenReturn(600000L);var response=new MockHttpServletResponse();
        f.doFilter(request("POST","127.0.0.1"),response,(q,s)->s.getWriter().write("allowed"));assertEquals("allowed",response.getContentAsString());
    }
    @Test void forwardedHeaderDoesNotBypassLimit() throws Exception {
        var f=new LoginRateLimitFilter(Clock.fixed(Instant.EPOCH,ZoneOffset.UTC));
        for(int i=0;i<60;i++)f.doFilter(request("POST","127.0.0.1"),new MockHttpServletResponse(),(q,s)->{});
        var r=request("POST","127.0.0.1");r.addHeader("X-Forwarded-For","different");var response=new MockHttpServletResponse();
        f.doFilter(r,response,(q,s)->fail("Should not reach chain"));assertEquals(429,response.getStatus());
    }
    @Test void otherRequestsAndOtherClientsRemainIndependent() throws Exception {
        var f=new LoginRateLimitFilter(Clock.fixed(Instant.EPOCH,ZoneOffset.UTC));var count=new AtomicInteger();
        for(int i=0;i<70;i++)f.doFilter(request("GET","127.0.0.1"),new MockHttpServletResponse(),(q,s)->count.incrementAndGet());
        f.doFilter(request("POST","127.0.0.2"),new MockHttpServletResponse(),(q,s)->count.incrementAndGet());assertEquals(71,count.get());
    }
}
