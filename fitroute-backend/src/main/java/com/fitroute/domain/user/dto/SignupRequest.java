// domain/user/dto/SignupRequest.java
package com.fitroute.domain.user.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@NoArgsConstructor
public class SignupRequest {

    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    private String email;

    @NotBlank
    @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
    private String password;

    private Float height;
    private Float weight;
    private Float targetWeight;
    private Integer targetPeriod;
}


