package com.fisa.solra.domain.user.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDto {

    private Long userId;
    private String userName;
    private String userLoginId;
    private String email;
    private Long departmentId;
    private Long organizationId;
}
