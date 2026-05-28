package com.skyfl.pfm.auth.service;

import com.skyfl.pfm.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final Key signingKey;
    private final long accessTokenMinutes;

    public JwtService(@Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-minutes}") long accessTokenMinutes) {
        byte[] keyBytes = secret.length() >= 32 ? secret.getBytes() : Decoders.BASE64.decode(secret);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenMinutes = accessTokenMinutes;
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .claims(Map.of(
                        "email", user.getEmail(),
                        "role", user.getRole(),
                        "jti", UUID.randomUUID().toString()
                ))
                .subject(user.getId().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(Duration.ofMinutes(accessTokenMinutes))))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public UUID extractUserId(String token) {
        try {
            return UUID.fromString(parseClaims(token).getSubject());
        } catch (Exception ex) {
            return null;
        }
    }

    public String extractJti(String token) {
        try {
            return parseClaims(token).get("jti", String.class);
        } catch (Exception ex) {
            return null;
        }
    }

    public long getAccessTokenSeconds() {
        return accessTokenMinutes * 60;
    }

    public boolean isTokenValid(String token, User user) {
        UUID userId = extractUserId(token);
        return userId != null && userId.equals(user.getId()) && !parseClaims(token).getExpiration().before(new Date());
    }

    private Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(Keys.hmacShaKeyFor(signingKey.getEncoded())).build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
