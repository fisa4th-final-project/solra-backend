package com.fisa.solra.domain.userrole.service;

import com.fisa.solra.domain.role.entity.Role;
import com.fisa.solra.domain.role.repository.RoleRepository;
import com.fisa.solra.domain.user.dto.UserResponseDto;
import com.fisa.solra.domain.user.entity.User;
import com.fisa.solra.domain.user.repository.UserRepository;
import com.fisa.solra.domain.userrole.dto.UserRoleResponseDto;
import com.fisa.solra.domain.userrole.entity.UserRole;
import com.fisa.solra.domain.userrole.repository.UserRoleRepository;
import com.fisa.solra.global.exception.BusinessException;
import com.fisa.solra.global.exception.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserRoleService {

    private final UserRoleRepository userRoleRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    // 사용자에게 역할 부여
    public UserRoleResponseDto assignRole(Long userId, Long roleId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND));

        boolean exists = userRoleRepository.existsByUserAndRole(user, role);
        if (exists) {
            throw new BusinessException(ErrorCode.ROLE_ALREADY_ASSIGNED);
        }

        LocalDateTime now = LocalDateTime.now();

        UserRole userRole = UserRole.builder()
                .user(user)
                .role(role)
                .createdAt(now)
                .updatedAt(now)
                .build();

        userRoleRepository.save(userRole);

        return UserRoleResponseDto.builder()
                .userId(user.getUserId())
                .roleId(role.getRoleId())
                .roleName(role.getRoleName())
                .createdAt(userRole.getCreatedAt())
                .updatedAt(userRole.getUpdatedAt())
                .build();
    }
}
