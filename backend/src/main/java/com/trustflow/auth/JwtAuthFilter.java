package com.trustflow.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/** Step 1 of the pipeline: validate the bearer token and establish the identity (username + roles). */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwt;

    public JwtAuthFilter(JwtService jwt) {
        this.jwt = jwt;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                Claims c = jwt.parse(header.substring(7));
                if (JwtService.ACCESS.equals(c.get("tt", String.class))) {
                    List<?> roles = c.get("roles", List.class);
                    var authorities = roles == null ? List.<SimpleGrantedAuthority>of()
                            : roles.stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r)).toList();
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(c.getSubject(), null, authorities));
                }
            } catch (JwtException | IllegalArgumentException ex) {
                SecurityContextHolder.clearContext(); // invalid or expired: falls through to the 401 entry point
            }
        }
        chain.doFilter(req, res);
    }
}
