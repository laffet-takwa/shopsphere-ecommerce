package com.shopsphere.demo;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Servlet equivalent of the reactive {@code JwtGatewayFilter}.
 *
 * <p>Spring Cloud Gateway runs on WebFlux, so its filter cannot be hosted in a servlet context. The
 * rules are ported one-for-one so the hosted demo enforces exactly what the gateway enforces in the
 * real topology: which paths are public, which writes are ADMIN-only, and that a caller can never
 * spoof the identity headers the services trust.
 *
 * <p>The servlet API cannot mutate request headers the way the reactive request builder can, so the
 * trusted identity is exposed through a wrapper. Any inbound {@code X-User-Id} or {@code X-User-Role}
 * is dropped rather than trusted, which is what stops a caller from claiming to be an administrator.
 */
@Component
public class DemoIdentityFilter extends OncePerRequestFilter {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String ROLE_HEADER = "X-User-Role";

    private final SecretKey key;

    public DemoIdentityFilter(@Value("${app.jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        HttpMethod method = HttpMethod.valueOf(request.getMethod());
        String path = request.getRequestURI();

        if (method == HttpMethod.OPTIONS || isPublic(method, path)) {
            chain.doFilter(request, response);
            return;
        }

        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            reject(response, HttpStatus.UNAUTHORIZED);
            return;
        }

        String role;
        long userId;
        try {
            var claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(authorization.substring(7)).getPayload();
            role = claims.get("role", String.class);
            Number userIdClaim = claims.get("userId", Number.class);
            if (userIdClaim == null || role == null) {
                reject(response, HttpStatus.UNAUTHORIZED);
                return;
            }
            userId = userIdClaim.longValue();
        } catch (RuntimeException exception) {
            reject(response, HttpStatus.UNAUTHORIZED);
            return;
        }

        if (requiresAdmin(method, path) && !"ADMIN".equals(role)) {
            reject(response, HttpStatus.FORBIDDEN);
            return;
        }

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(String.valueOf(userId), null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))));

        chain.doFilter(new IdentityRequestWrapper(request, Long.toString(userId), role), response);
    }

    private boolean isPublic(HttpMethod method, String path) {
        // The host polls the health endpoint before any credential exists, so it is public here in
        // the same way the gateway treats it.
        if (path.startsWith("/actuator/") || path.equals("/actuator/health")) {
            return true;
        }
        if (method == HttpMethod.POST
                && (path.equals("/api/auth/register") || path.equals("/api/auth/login"))) {
            return true;
        }
        return method == HttpMethod.GET
                && (path.equals("/api/products") || path.startsWith("/api/products/"));
    }

    private boolean requiresAdmin(HttpMethod method, String path) {
        boolean write = method != HttpMethod.GET && method != HttpMethod.HEAD && method != HttpMethod.OPTIONS;
        boolean shipping = path.startsWith("/api/orders/") && path.endsWith("/ship");
        return write && (path.startsWith("/api/products")
                || path.startsWith("/api/inventory")
                || shipping);
    }

    private void reject(HttpServletResponse response, HttpStatus status) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/problem+json");
        response.getWriter().write("{\"title\":\"" + status.getReasonPhrase() + "\",\"status\":"
                + status.value() + "}");
    }

    /** Presents the verified identity while hiding whatever the client claimed. */
    private static final class IdentityRequestWrapper extends HttpServletRequestWrapper {

        private final String userId;
        private final String role;

        private IdentityRequestWrapper(HttpServletRequest request, String userId, String role) {
            super(request);
            this.userId = userId;
            this.role = role;
        }

        @Override
        public String getHeader(String name) {
            if (USER_ID_HEADER.equalsIgnoreCase(name)) {
                return userId;
            }
            if (ROLE_HEADER.equalsIgnoreCase(name)) {
                return role;
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if (USER_ID_HEADER.equalsIgnoreCase(name) || ROLE_HEADER.equalsIgnoreCase(name)) {
                return Collections.enumeration(List.of(getHeader(name)));
            }
            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            List<String> names = new ArrayList<>();
            Enumeration<String> original = super.getHeaderNames();
            while (original != null && original.hasMoreElements()) {
                String name = original.nextElement();
                if (!USER_ID_HEADER.equalsIgnoreCase(name) && !ROLE_HEADER.equalsIgnoreCase(name)) {
                    names.add(name);
                }
            }
            names.add(USER_ID_HEADER);
            names.add(ROLE_HEADER);
            return Collections.enumeration(names);
        }
    }
}
