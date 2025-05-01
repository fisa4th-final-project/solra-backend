package com.fisa.solra.domain.role.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleResponseDto {
    private Long roleId;
    private String roleName;
    private String description;
}
