package com.fisa.solra.domain.user.dto;

import com.fisa.solra.domain.role.entity.Role;
import lombok.*;

import java.util.Set;

@Getter
@Builder
@AllArgsConstructor
public class UserLoginInfo {
    private Long userId;
    private Long orgId;
    private Long deptId;
    private Set<Role> roles;
}
