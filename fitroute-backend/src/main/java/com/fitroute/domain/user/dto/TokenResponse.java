// domain/user/dto/TokenResponse.java
package com.fitroute.domain.user.dto;

import lombok.*;

@Getter
@AllArgsConstructor
public class TokenResponse {
    private String accessToken;
    private String refreshToken; // 다음 단계에서 Redis 연동
}