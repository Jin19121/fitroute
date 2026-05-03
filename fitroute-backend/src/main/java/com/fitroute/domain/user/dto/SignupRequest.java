// domain/user/dto/SignupRequest.java
package com.fitroute.domain.user.dto;

import com.fitroute.global.enums.ActivityLevel;
import com.fitroute.global.enums.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class SignupRequest {

    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    private String email;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
    private String password;

    @NotNull(message = "키는 필수입니다.")
    private Float height;

    @NotNull(message = "현재 체중은 필수입니다.")
    private Float weight;

    @NotNull(message = "목표 체중은 필수입니다.")
    private Float targetWeight;

    @NotNull(message = "목표 기간은 필수입니다.")
    private Integer targetPeriod;

    @NotNull(message = "성별은 필수입니다.")
    private Gender gender;

    @NotNull(message = "생년월일은 필수입니다.")
    @Past(message = "생년월일은 과거 날짜여야 합니다.")
    private LocalDate birthDate;

    @NotNull(message = "활동 수준은 필수입니다.")
    private ActivityLevel activityLevel;
}
