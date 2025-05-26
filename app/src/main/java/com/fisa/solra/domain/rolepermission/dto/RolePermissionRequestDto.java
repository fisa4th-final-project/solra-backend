package com.fisa.solra.domain.rolepermission.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RolePermissionRequestDto {
    private Long roleId;
    private List<Long> permissionIds;
}
