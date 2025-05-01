package com.fisa.solra.domain.user.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRequestDto {

    private String userName;
    private String userLoginId;
    private String email;
    private String password;
    private Long departmentId;
    private Long organizationId;
}