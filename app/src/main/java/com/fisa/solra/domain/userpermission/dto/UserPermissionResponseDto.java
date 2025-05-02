package com.fisa.solra.domain.userpermission.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPermissionResponseDto {
    private Long userId;
    private Long permissionId;
    private Long userPermissionId;
}
