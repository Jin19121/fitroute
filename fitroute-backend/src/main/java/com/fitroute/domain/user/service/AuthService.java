// domain/user/service/AuthService.java
package com.fitroute.domain.user.service;

import com.fitroute.domain.user.dto.*;
import com.fitroute.domain.user.entity.*;
import com.fitroute.domain.user.repository.*;
import com.fitroute.global.exception.ErrorCode;
import com.fitroute.global.jwt.JwtProvider;
import com.fitroute.global.util.Aes256Util;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private static final String REFRESH_TOKEN_PREFIX = "RT:";

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final Aes256Util aes256Util;
    private final JwtProvider jwtProvider;
    private final RedisTemplate<String, String> redisTemplate;

    @Transactional
    public void signup(SignupRequest req) {
        String encryptedEmail = aes256Util.encrypt(req.getEmail());

        if (userRepository.existsByEmail(encryptedEmail)) {
            throw new IllegalArgumentException(ErrorCode.DUPLICATE_EMAIL.getMessage());
        }

        User user = userRepository.save(User.builder()
                .email(encryptedEmail)
                .password(passwordEncoder.encode(req.getPassword()))
                .build());

        userProfileRepository.save(UserProfile.builder()
                .user(user)
                .height(req.getHeight())
                .weight(req.getWeight())
                .targetWeight(req.getTargetWeight())
                .targetPeriod(req.getTargetPeriod())
                .build());
    }

    @Transactional
    public TokenResponse login(LoginRequest req) {
        String encryptedEmail = aes256Util.encrypt(req.getEmail());

        User user = userRepository.findByEmail(encryptedEmail)
                .orElseThrow(() -> new IllegalArgumentException(ErrorCode.USER_NOT_FOUND.getMessage()));

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException(ErrorCode.INVALID_PASSWORD.getMessage());
        }

        String accessToken  = jwtProvider.createAccessToken(user.getId());
        String refreshToken = jwtProvider.createRefreshToken(user.getId());

        // Redis에 Refresh Token 저장 (TTL = 7일)
        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + user.getId(),
                refreshToken,
                jwtProvider.getRefreshTokenValidMs(),
                TimeUnit.MILLISECONDS
        );

        return new TokenResponse(accessToken, refreshToken);
    }

    // Access Token 재발급
    public TokenResponse refresh(RefreshRequest req) {
        String refreshToken = req.getRefreshToken();

        // 1. Refresh Token 자체 유효성 검사
        jwtProvider.validate(refreshToken);

        Long userId = jwtProvider.getUserId(refreshToken);
        String redisKey = REFRESH_TOKEN_PREFIX + userId;

        // 2. Redis에 저장된 RT와 비교
        String storedToken = redisTemplate.opsForValue().get(redisKey);
        if (storedToken == null || !storedToken.equals(refreshToken)) {
            throw new IllegalArgumentException(ErrorCode.REFRESH_TOKEN_MISMATCH.getMessage());
        }

        // 3. 새 토큰 발급 및 Redis 갱신 (Refresh Token Rotation)
        String newAccessToken  = jwtProvider.createAccessToken(userId);
        String newRefreshToken = jwtProvider.createRefreshToken(userId);

        redisTemplate.opsForValue().set(
                redisKey,
                newRefreshToken,
                jwtProvider.getRefreshTokenValidMs(),
                TimeUnit.MILLISECONDS
        );

        return new TokenResponse(newAccessToken, newRefreshToken);
    }

    // 로그아웃: Redis에서 RT 삭제
    @Transactional
    public void logout(Long userId) {
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + userId);
    }
}