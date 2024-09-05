package com.dimon.catanbackend.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
/**
 * Utility class for managing JWT token creation, validation, and extraction of claims.
 * This class uses the `io.jsonwebtoken` library to generate and parse JSON Web Tokens (JWT).
 *
 * It supports creating JWT tokens with roles and expiration time, validating tokens,
 * and extracting claims such as username and roles from the token.
 *
 * Annotations used:
 * - {@link Component} to mark this class as a Spring-managed bean for dependency injection.
 * - {@link Value} to inject configuration properties such as the JWT secret and token lifetime.
 *
 * Configuration properties:
 * - {@code jwt.secret}: The secret key used to sign JWT tokens (injected via application properties).
 * - {@code jwt.lifetime}: The duration (ISO-8601 format) for which the JWT token is valid.
 *
 * Methods:
 * - {@code generateToken}: Generates a JWT token for the given {@link UserDetails} object.
 * - {@code getUsername}: Extracts the username (subject) from a given token.
 * - {@code getRoles}: Extracts the roles from a given token.
 * - {@code validateToken}: Validates if the token is valid and not expired for the given {@link UserDetails}.
 * - {@code isTokenExpired}: Checks if a given token is expired.
 * - {@code getAllClaimsFromToken}: Extracts all claims from a given token.
 *
 * Example usage:
 * <pre>
 * {@code
 * String token = jwtTokenUtils.generateToken(userDetails);
 * boolean isValid = jwtTokenUtils.validateToken(token, userDetails);
 * String username = jwtTokenUtils.getUsername(token);
 * List<String> roles = jwtTokenUtils.getRoles(token);
 * }
 * </pre>
 *
 * Dependencies:
 * - Uses the `io.jsonwebtoken.Jwts` class to build, parse, and validate JWT tokens.
 * - Secret key for signing the tokens is derived from the injected secret.
 *
 * @see UserDetails
 * @see Claims
 * @see Jwts
 * @see SignatureAlgorithm
 *
 */
@Component
public class JwtTokenUtils {
    @Value("${jwt.secret}")
    private String secret;

    // Adjusted to convert ISO-8601 duration string to a Duration object
    @Value("${jwt.lifetime}")
    private Duration jwtLifetime;

    private SecretKey secretKey;

    /**
     * Retrieves the secret key used for signing and verifying JWT tokens.
     * Lazily initializes the secret key using the injected secret.
     *
     * @return the {@link SecretKey} used for JWT signing
     */
    private SecretKey getSecretKey() {
        if (this.secretKey == null) {
            this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        }
        return this.secretKey;
    }

    /**
     * Generates a JWT token for the given {@link UserDetails} object. The token includes claims for the
     * username (subject), roles, issue date, and expiration time.
     *
     * @param userDetails the {@link UserDetails} object containing the user's details
     * @return the generated JWT token as a string
     */
    public String generateToken(UserDetails userDetails) {
        Date issuedDate = new Date();
        long expMillis = issuedDate.getTime() + jwtLifetime.toMillis();
        long expSeconds = expMillis / 1000;

        var builder = Jwts.builder()
                .claim("sub", userDetails.getUsername())
                .claim("iat", issuedDate.getTime() / 1000)
                .claim("exp", expSeconds)
                .signWith(getSecretKey(), SignatureAlgorithm.HS256);

        // Get roles from userDetails and add to claims
        List<String> rolesList = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        builder.claim("roles", rolesList);

        return builder.compact();
    }

    /**
     * Extracts the username (subject) from the JWT token.
     *
     * @param token the JWT token
     * @return the username (subject) extracted from the token
     */
    public String getUsername(String token) {
        return getAllClaimsFromToken(token).getSubject();
    }

    /**
     * Extracts the roles from the JWT token.
     *
     * @param token the JWT token
     * @return a list of roles extracted from the token
     */
    public List<String> getRoles(String token) {
        return getAllClaimsFromToken(token).get("roles", List.class);
    }

    /**
     * Extracts all claims from the JWT token.
     *
     * @param token the JWT token
     * @return the {@link Claims} object containing all claims from the token
     */
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parser()
                .setSigningKey(getSecretKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Validates the JWT token by checking if the username matches the provided {@link UserDetails}
     * and if the token is not expired.
     *
     * @param token the JWT token to validate
     * @param userDetails the {@link UserDetails} to validate against
     * @return {@code true} if the token is valid, {@code false} otherwise
     */
    public boolean validateToken(String token, UserDetails userDetails) {
        final String username = getUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    /**
     * Checks if the JWT token has expired.
     *
     * @param token the JWT token to check
     * @return {@code true} if the token is expired, {@code false} otherwise
     */
    private boolean isTokenExpired(String token) {
        final Date expiration = getAllClaimsFromToken(token).getExpiration();
        return expiration.before(new Date());
    }
}
