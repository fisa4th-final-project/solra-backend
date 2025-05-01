package com.fisa.solra.domain.rolepermission.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RolePermissionRequestDto {
    private Long roleId;
    private Long permissionId;
}
