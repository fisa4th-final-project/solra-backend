package com.fisa.solra.domain.role.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleRequestDto {
    private String roleName;
    private String description;
}

