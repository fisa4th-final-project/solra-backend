package com.fisa.solra.domain.permission.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PermissionResponseDto {
    private Long permissionId;
    private String permissionName;
    private String description;
}
