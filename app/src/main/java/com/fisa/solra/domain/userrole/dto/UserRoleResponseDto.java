package com.fisa.solra.domain.userrole.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRoleResponseDto {
    private Long userId;
    private Long roleId;
    private String roleName;
}
