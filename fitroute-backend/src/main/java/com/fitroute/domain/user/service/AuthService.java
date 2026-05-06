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

    /**
     * 회원가입: User + UserProfile 저장 후 토큰 반환.
     * 실패(중복 이메일 등) 시 예외를 던져 컨트롤러에서 처리.
     */
    @Transactional
    public SignupResponse signup(SignupRequest req) {
        String encryptedEmail = aes256Util.encrypt(req.getEmail());

        if (userRepository.existsByEncryptedEmail(encryptedEmail)) {
            throw new IllegalArgumentException(ErrorCode.DUPLICATE_EMAIL.getMessage());
        }

        User user = userRepository.save(User.builder()
                .encryptedEmail(encryptedEmail)
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
        String encryptedEmail = aes256Util.encrypt(req.getEmail());

        User user = userRepository.findByEncryptedEmail(encryptedEmail)
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

    @Transactional
    public void logout(Long userId) {
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + userId);
    }
}