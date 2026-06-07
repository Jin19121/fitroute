// domain/user/service/AuthService.java
package com.fitroute.domain.user.service;

import com.fitroute.domain.user.dto.*;
import com.fitroute.domain.user.entity.User;
import com.fitroute.domain.user.entity.UserProfile;
import com.fitroute.domain.user.repository.UserRepository;
import com.fitroute.domain.user.repository.UserProfileRepository;
import com.fitroute.global.enums.UserRole;
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
    public SignupResponse signup(SignupRequest req) {
        String plainEmail = req.getEmail().toLowerCase().trim();
        String emailHash = aes256Util.hash(plainEmail);

        if (userRepository.existsByEmailHash(emailHash)) {
            throw new IllegalArgumentException(ErrorCode.DUPLICATE_EMAIL.getMessage());
        }

        String encryptedEmail = aes256Util.encrypt(plainEmail);

        User user = userRepository.save(User.builder()
                .encryptedEmail(encryptedEmail)
                .emailHash(emailHash)
                .password(passwordEncoder.encode(req.getPassword()))
                .role(UserRole.USER)
                .build());

        userProfileRepository.save(UserProfile.builder()
                .user(user)
                .height(req.getHeight())
                .weight(req.getWeight())
                .targetWeight(req.getTargetWeight())
                .targetPeriod(req.getTargetPeriod())
                .gender(req.getGender())
                .birthDate(req.getBirthDate())
                .activityLevel(req.getActivityLevel())
                .goalType(req.getGoalType())
                .exerciseExperience(req.getExerciseExperience())
                .dietStyle(req.getDietStyle())
                .build());

        String accessToken = jwtProvider.createAccessToken(user.getId());
        String refreshToken = jwtProvider.createRefreshToken(user.getId());

        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + user.getId(),
                refreshToken,
                jwtProvider.getRefreshTokenValidMs(),
                TimeUnit.MILLISECONDS);

        return new SignupResponse(accessToken, refreshToken);
    }

    @Transactional
    public TokenResponse login(LoginRequest req) {
        String plainEmail = req.getEmail().toLowerCase().trim();
        String emailHash = aes256Util.hash(plainEmail);

        User user = userRepository.findByEmailHash(emailHash)
                .orElseThrow(() -> new IllegalArgumentException(ErrorCode.USER_NOT_FOUND.getMessage()));

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException(ErrorCode.INVALID_PASSWORD.getMessage());
        }

        String accessToken = jwtProvider.createAccessToken(user.getId());
        String refreshToken = jwtProvider.createRefreshToken(user.getId());

        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + user.getId(),
                refreshToken,
                jwtProvider.getRefreshTokenValidMs(),
                TimeUnit.MILLISECONDS);

        return new TokenResponse(accessToken, refreshToken);
    }

    public TokenResponse refresh(RefreshRequest req) {
        String refreshToken = req.getRefreshToken();
        jwtProvider.validate(refreshToken);

        Long userId = jwtProvider.getUserId(refreshToken);
        String redisKey = REFRESH_TOKEN_PREFIX + userId;

        String storedToken = redisTemplate.opsForValue().get(redisKey);
        if (storedToken == null || !storedToken.equals(refreshToken)) {
            throw new IllegalArgumentException(ErrorCode.REFRESH_TOKEN_MISMATCH.getMessage());
        }

        String newAccessToken = jwtProvider.createAccessToken(userId);
        String newRefreshToken = jwtProvider.createRefreshToken(userId);

        redisTemplate.opsForValue().set(
                redisKey,
                newRefreshToken,
                jwtProvider.getRefreshTokenValidMs(),
                TimeUnit.MILLISECONDS);

        return new TokenResponse(newAccessToken, newRefreshToken);
    }

    /**
     * 로그아웃
     * 1. Refresh Token 삭제 — 재발급 차단
     * 2. Access Token 블랙리스트 등록 — 만료 전 재사용 차단
     * accessToken이 null인 경우(헤더 누락 등)는 Refresh Token만 삭제하고 정상 처리
     */
    @Transactional
    public void logout(Long userId, String accessToken) {
        // 1. Refresh Token 삭제
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + userId);

        // 2. Access Token 블랙리스트 등록
        if (accessToken != null) {
            long remainingMs = jwtProvider.getRemainingMs(accessToken);
            if (remainingMs > 0) {
                redisTemplate.opsForValue().set(
                        "BL:" + accessToken,
                        "logout",
                        remainingMs,
                        TimeUnit.MILLISECONDS);
            }
        }
    }
}