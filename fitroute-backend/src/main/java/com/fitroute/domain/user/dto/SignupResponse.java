// domain/user/dto/SignupResponse.java
package com.fitroute.domain.user.dto;

import lombok.*;

@Getter
@AllArgsConstructor
public class SignupResponse {
    private String accessToken;
    private String refreshToken;
    // 클라이언트가 바로 /plans/today/generate 를 호출할 수 있도록 토큰 반환
}