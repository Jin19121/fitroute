// global/jwt/JwtProvider.java
package com.fitroute.global.jwt;

import com.fitroute.global.exception.ErrorCode;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;          // SecretKey의 부모 타입
import java.util.Date;

@Component
public class JwtProvider {

    private final SecretKey secretKey;

    private static final long ACCESS_TOKEN_VALID_MS  = 1000L * 60 * 30;
    private static final long REFRESH_TOKEN_VALID_MS = 1000L * 60 * 60 * 24 * 7;

    public JwtProvider(@Value("${security.jwt.secret}") String secret) {
        // 0.12.x는 SecretKey 객체로 관리
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(Long userId) {
        return buildToken(userId, ACCESS_TOKEN_VALID_MS);
    }

    public String createRefreshToken(Long userId) {
        return buildToken(userId, REFRESH_TOKEN_VALID_MS);
    }

    public long getRefreshTokenValidMs() {
        return REFRESH_TOKEN_VALID_MS;
    }

    public Long getUserId(String token) {
        return Long.parseLong(getClaims(token).getSubject());
    }

    public boolean validate(String token) {
        try {
            getClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            throw new IllegalArgumentException(ErrorCode.EXPIRED_TOKEN.getMessage());
        } catch (JwtException | IllegalArgumentException e) {
            throw new IllegalArgumentException(ErrorCode.INVALID_TOKEN.getMessage());
        }
    }

    private Claims getClaims(String token) {
        // 0.12.x API
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private String buildToken(Long userId, long validMs) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + validMs))
                .signWith(secretKey)
                .compact();
    }
}