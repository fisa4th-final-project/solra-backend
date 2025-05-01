package com.fisa.solra.domain.user.dto;

import lombok.*;

@Getter
@Builder
@AllArgsConstructor
public class UserLoginInfo {
    private Long userId;
    private Long orgId;
    private Long deptId;
    private String role;
}
