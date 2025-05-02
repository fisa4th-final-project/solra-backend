package com.fisa.solra.domain.userpermission.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPermissionRequestDto {
    private Long userId;
    private Long permissionId;
}
