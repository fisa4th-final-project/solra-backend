package com.fisa.solra.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserUpdateRequestDto {

    @NotBlank(message = "이름은 필수입니다.")
    private String userName;

    @Email(message = "올바른 이메일 형식이어야 합니다.")
    private String email;
}
