package com.fisa.solra.domain.userrole.dto;

import lombok.*;

@Getter
@Setter
@Builder
public class UserRoleRequestDto {
    private Long userId;
    private Long roleId;
}
