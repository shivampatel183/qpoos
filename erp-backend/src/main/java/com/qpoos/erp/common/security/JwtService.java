package com.qpoos.erp.common.security;

import com.qpoos.erp.user.domain.UserEntity;
import com.qpoos.erp.auth.config.AuthProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final AuthProperties properties;
    private final Clock clock;
    private SecretKey signingKey;

    @PostConstruct
    public void init() {
        this.signingKey = Keys.hmacShaKeyFor(properties.getJwtSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(com.qpoos.erp.user.domain.UserEntity user, UUID companyId) {
        Instant now = clock.instant();
        Instant expireAt = now.plusSeconds(properties.accessTokenSeconds());

        var builder = Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole())
                .claim("emailVerified", Boolean.TRUE.equals(user.getEmailVerified()))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expireAt))
                .signWith(signingKey);

        if (companyId != null) {
            builder.claim("companyId", companyId.toString());
        }

        return builder.compact();
    }

    /** Overload for initial login where companyId isn't chosen yet. */
    public String createAccessToken(com.qpoos.erp.user.domain.UserEntity user) {
        return createAccessToken(user, null);
    }

    /** Parses and validates the cryptographic signature. Call this once per request. */
    public Claims validateAndGetClaims(String token) {
        return Jwts.parser()
                .clock(() -> Date.from(clock.instant()))
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // Static claim extractors.

    public static UUID getUserId(Claims claims) {
        return UUID.fromString(claims.getSubject());
    }

    public static UUID getCompanyId(Claims claims) {
        String companyIdStr = claims.get("companyId", String.class);
        return companyIdStr != null ? UUID.fromString(companyIdStr) : null;
    }

    public static String getEmail(Claims claims) {
        return claims.get("email", String.class);
    }

    public static String getRole(Claims claims) {
        return claims.get("role", String.class);
    }
}
