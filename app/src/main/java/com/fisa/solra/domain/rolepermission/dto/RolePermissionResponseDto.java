package com.fisa.solra.domain.rolepermission.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RolePermissionResponseDto {
    private Long rolePermissionId;
    private Long roleId;
    private Long permissionId;
}
