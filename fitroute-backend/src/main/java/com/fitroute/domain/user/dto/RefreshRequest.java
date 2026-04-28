// domain/user/dto/RefreshRequest.java
package com.fitroute.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@NoArgsConstructor
public class RefreshRequest {

    @NotBlank(message = "Refresh Token은 필수입니다.")
    private String refreshToken;
}