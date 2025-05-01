package com.fisa.solra.domain.userrole.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRoleRequestDto {
    private Long userId;
    private Long roleId;
}
