package com.fisa.solra.domain.user.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserCreateRequestDto {

    @NotBlank(message = "로그인 ID는 필수입니다.")
    private String userLoginId;

    @NotBlank(message = "비밀번호는 필수입니다.")
    private String password;

    @NotBlank(message = "이름은 필수입니다.")
    private String userName;

    @Email
    private String email;

    private Long orgId;

    private Long deptId;
}
