package com.melita.OrderProcessingService.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final String secret;

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    public JwtAuthenticationFilter(@Value("${jwt.secret}") String secret) {
        this.secret = secret;
    }

    @Override
    public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = request.getHeader("Authorization");

        if (token == null || !token.startsWith("Bearer ")) {
            logger.warn("No valid Authorization header found");
            chain.doFilter(request, response);
            return;
        }

        token = token.substring(7);
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String user = claims.getSubject();
            if (user == null) {
                logger.warn("Token parsed but no subject found");
                chain.doFilter(request, response);
                return;
            }

            request.setAttribute("user", user);
            logger.info("Authenticated user: {}", user);
        } catch (ExpiredJwtException e) {
            logger.error("JWT token is expired", e);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token expired");
            return;
        } catch (MalformedJwtException e) {
            logger.error("JWT token is malformed", e);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Malformed token");
            return;
        } catch (JwtException e) {
            logger.error("Invalid JWT token", e);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
            return;
        } catch (Exception e) {
            logger.error("Unexpected error during token validation", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Token validation error");
            return;
        }
        chain.doFilter(request, response);
    }
}
