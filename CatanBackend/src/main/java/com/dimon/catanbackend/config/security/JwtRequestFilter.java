package com.dimon.catanbackend.config.security;

import com.dimon.catanbackend.service.UserService;
import com.dimon.catanbackend.utils.JwtTokenUtils;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.SignatureException;
/**
 * Filter class responsible for handling JWT authentication in incoming HTTP requests.
 * This filter extracts the JWT token from the `Authorization` header of the request, validates it,
 * and sets the authenticated user in the Spring Security context if the token is valid.
 *
 * The filter is a part of Spring Security's filter chain and extends {@link OncePerRequestFilter},
 * ensuring that it is executed once per request.
 *
 * Annotations used:
 * - {@link Component} to mark this class as a Spring-managed bean.
 * - {@link RequiredArgsConstructor} to automatically generate a constructor for final fields using Lombok.
 * - {@link Slf4j} to provide a logger for this class using Lombok.
 *
 * Dependencies:
 * - {@link JwtTokenUtils} for extracting and validating JWT tokens.
 * - {@link UserService} for loading user details based on the username extracted from the token.
 *
 * Methods:
 * - {@code doFilterInternal}: Extracts the JWT token, validates it, and sets the authentication in the
 *   {@link SecurityContextHolder} if the token is valid.
 *
 * Key logic:
 * - Extracts the JWT token from the `Authorization` header if it starts with "Bearer".
 * - Uses the {@link JwtTokenUtils} to extract the username from the token and validate it.
 * - Loads the user details from the {@link UserService} and sets the authentication in the security context.
 * - Handles exceptions such as expired or malformed JWT tokens and logs the details.
 *
 * Example usage:
 * <pre>
 * {@code
 * // The filter automatically runs as part of the Spring Security filter chain
 * }
 * </pre>
 *
 * Exception handling:
 * - Catches and logs {@link ExpiredJwtException}, {@link MalformedJwtException}, and other JWT-related exceptions.
 *
 * @see OncePerRequestFilter
 * @see JwtTokenUtils
 * @see UserService
 * @see SecurityContextHolder
 * @see UsernamePasswordAuthenticationToken
 * @see WebAuthenticationDetailsSource
 *
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtRequestFilter extends OncePerRequestFilter {
    private final JwtTokenUtils jwtTokenUtils;
    private final UserService userService;

    /**
     * Extracts the JWT token from the request, validates it, and sets the authentication in the security context.
     *
     * @param request the {@link HttpServletRequest} object
     * @param response the {@link HttpServletResponse} object
     * @param filterChain the {@link FilterChain} to pass the request and response to the next filter
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs during the filtering process
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        String email = null;
        String jwt = null;

        if(authHeader != null && authHeader.startsWith("Bearer ")){
            jwt = authHeader.substring(7);
            log.debug("Extracted JWT Token: {}", jwt);
            try {
                email = jwtTokenUtils.getUsername(jwt);
            } catch (ExpiredJwtException e) {
                log.debug("JWT Token has expired: {}", e.getMessage());
            } catch (MalformedJwtException e) {
                log.debug("Invalid JWT Token: {}", e.getMessage());
            }
        }

        log.debug("Before filter: email={}, jwt={}", email, jwt);

        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userService.loadUserByUsername(email);

            if (jwtTokenUtils.validateToken(jwt, userDetails)) {
                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        }
        filterChain.doFilter(request, response);
    }
}
