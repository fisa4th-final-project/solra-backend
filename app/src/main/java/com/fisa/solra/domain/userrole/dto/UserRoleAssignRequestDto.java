package com.fisa.solra.domain.userrole.dto;

import lombok.*;

@Getter
@Setter
@Builder
public class UserRoleAssignRequestDto {
    private Long userId;
    private Long roleId;
}
