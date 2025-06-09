package com.budget.config;

import com.budget.utilities.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, 
                                @NonNull HttpServletResponse response, 
                                @NonNull FilterChain chain)
            throws ServletException, IOException {
        String requestPath = request.getRequestURI();
        String method = request.getMethod();
        logger.debug("Processing request: {} {}", method, requestPath);

        if (isPublicEndpoint(requestPath)) {
            logger.debug("Skipping authentication for public endpoint: {}", requestPath);
            chain.doFilter(request, response);
            return;
        }

        AuthenticationResult authResult = processAuthentication(request, requestPath);
        
        if (!authResult.isSuccess()) {
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, authResult.getErrorMessage());
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isPublicEndpoint(String requestPath) {
        return requestPath.startsWith("/login") || 
            requestPath.startsWith("/register") || 
            requestPath.startsWith("/public/");
    }

    private AuthenticationResult processAuthentication(HttpServletRequest request, String requestPath) {
        String jwt = extractJwtFromRequest(request);
        
        if (jwt == null) {
            logger.debug("No valid Bearer token found in request to {}", requestPath);
            return AuthenticationResult.success(); // No token is acceptable for some endpoints
        }

        return authenticateWithJwt(jwt, request, requestPath);
    }

    private String extractJwtFromRequest(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");
        
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        
        return null;
    }

    private AuthenticationResult authenticateWithJwt(String jwt, HttpServletRequest request, String requestPath) {
        String username = extractUsernameFromJwt(jwt, requestPath);
        
        if (username == null) {
            return AuthenticationResult.failure("Invalid JWT token");
        }

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            return AuthenticationResult.success(); // Already authenticated
        }

        return validateAndSetAuthentication(jwt, username, request, requestPath);
    }

    private String extractUsernameFromJwt(String jwt, String requestPath) {
        try {
            return jwtUtil.extractUsername(jwt);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid JWT token in request to {}: {}", requestPath, e.getMessage());
            return null;
        }
    }

    private AuthenticationResult validateAndSetAuthentication(String jwt, String username, 
                                                            HttpServletRequest request, String requestPath) {
        try {
            UserDetails userDetails = loadUserDetails(username, requestPath);
            
            if (userDetails == null) {
                return AuthenticationResult.failure("User not found");
            }

            if (!jwtUtil.validateToken(jwt, userDetails)) {
                logger.warn("JWT token validation failed for user {} in request to {}", username, requestPath);
                return AuthenticationResult.failure("Invalid or expired JWT token");
            }

            setSecurityContext(userDetails, request);
            logger.info("Successfully authenticated user {} for request to {}", username, requestPath);
            
            return AuthenticationResult.success();
            
        } catch (Exception e) {
            logger.error("Authentication error for user {} in request to {}: {}", username, requestPath, e.getMessage());
            return AuthenticationResult.failure("Authentication error");
        }
    }

    private UserDetails loadUserDetails(String username, String requestPath) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        
        if (userDetails == null) {
            logger.warn("User not found for username: {} in request to {}", username, requestPath);
        }
        
        return userDetails;
    }

    private void setSecurityContext(UserDetails userDetails, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }

    private void sendErrorResponse(HttpServletResponse response, int status, String errorMessage) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("error", errorMessage);
        errorResponse.put("timestamp", java.time.LocalDateTime.now().toString());
        response.getWriter().write(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(errorResponse));
    }
}