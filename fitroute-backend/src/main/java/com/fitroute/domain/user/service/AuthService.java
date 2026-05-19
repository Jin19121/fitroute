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

import java.util.List;
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
     *
     * 이메일 중복 체크:
     * GCM 암호화는 동일 평문이라도 IV가 달라 암호문이 매번 다르므로
     * encryptedEmail 컬럼으로 직접 비교가 불가능하다.
     * 따라서 전체 사용자의 encryptedEmail을 복호화해서 비교한다.
     * (사용자 수가 적은 MVP 단계에서는 허용 가능, 추후 email_hash 컬럼 추가로 개선 예정)
     */
    @Transactional
    public SignupResponse signup(SignupRequest req) {
        String plainEmail = req.getEmail().toLowerCase().trim();

        boolean isDuplicate = userRepository.findAll().stream()
                .anyMatch(u -> {
                    try {
                        return plainEmail.equals(aes256Util.decrypt(u.getEncryptedEmail()));
                    } catch (Exception e) {
                        return false;
                    }
                });

        if (isDuplicate) {
            throw new IllegalArgumentException(ErrorCode.DUPLICATE_EMAIL.getMessage());
        }

        String encryptedEmail = aes256Util.encrypt(plainEmail);

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

    /**
     * 로그인:
     * GCM 암호화 특성상 encryptedEmail로 DB 조회가 불가능하므로
     * 전체 사용자 조회 후 복호화하여 이메일을 비교한다.
     * (추후 email_hash 컬럼 추가로 개선 예정)
     */
    @Transactional
    public TokenResponse login(LoginRequest req) {
        String plainEmail = req.getEmail().toLowerCase().trim();

        List<User> allUsers = userRepository.findAll();
        User user = allUsers.stream()
                .filter(u -> {
                    try {
                        return plainEmail.equals(aes256Util.decrypt(u.getEncryptedEmail()));
                    } catch (Exception e) {
                        return false;
                    }
                })
                .findFirst()
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