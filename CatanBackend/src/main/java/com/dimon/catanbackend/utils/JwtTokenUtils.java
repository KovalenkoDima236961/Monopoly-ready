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

@Component
public class JwtTokenUtils {
    @Value("${jwt.secret}")
    private String secret;

    // Adjusted to convert ISO-8601 duration string to a Duration object
    @Value("${jwt.lifetime}")
    private Duration jwtLifetime;

    private SecretKey secretKey;

    private SecretKey getSecretKey() {
        if (this.secretKey == null) {
            this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        }
        return this.secretKey;
    }

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

    public String getUsername(String token) {
        return getAllClaimsFromToken(token).getSubject();
    }

    public List<String> getRoles(String token) {
        return getAllClaimsFromToken(token).get("roles", List.class);
    }

    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parser()
                .setSigningKey(getSecretKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        final String username = getUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        final Date expiration = getAllClaimsFromToken(token).getExpiration();
        return expiration.before(new Date());
    }
}
