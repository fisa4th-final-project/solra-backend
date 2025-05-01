package com.fisa.solra.domain.userrole.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRoleResponseDto {
    private Long userRoleId;
    private Long userId;
    private Long roleId;
}
