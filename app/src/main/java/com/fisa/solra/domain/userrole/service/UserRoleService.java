package com.fisa.solra.domain.userrole.service;

import com.fisa.solra.domain.role.entity.Role;
import com.fisa.solra.domain.role.repository.RoleRepository;
import com.fisa.solra.domain.user.dto.UserResponseDto;
import com.fisa.solra.domain.user.entity.User;
import com.fisa.solra.domain.user.repository.UserRepository;
import com.fisa.solra.domain.userrole.dto.UserRoleRequestDto;
import com.fisa.solra.domain.userrole.dto.UserRoleResponseDto;
import com.fisa.solra.domain.userrole.entity.UserRole;
import com.fisa.solra.domain.userrole.repository.UserRoleRepository;
import com.fisa.solra.global.exception.BusinessException;
import com.fisa.solra.global.exception.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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
                .build();

        userRoleRepository.save(userRole);

        return UserRoleResponseDto.builder()
                .userId(user.getUserId())
                .roleId(role.getRoleId())
                .roleName(role.getRoleName())
                .build();
    }

    // 사용자 역할 해제 서비스 로직
    @Transactional
    public UserRoleResponseDto removeUserRole(UserRoleRequestDto requestDto) {
        User user = userRepository.findById(requestDto.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Role role = roleRepository.findById(requestDto.getRoleId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND));

        UserRole userRole = userRoleRepository.findByUserAndRole(user, role)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND)); // or ROLE_NOT_ASSIGNED

        userRoleRepository.delete(userRole);

        return UserRoleResponseDto.builder()
                .userId(user.getUserId())
                .roleId(role.getRoleId())
                .roleName(role.getRoleName())
                .build();
    }

    // 사용자 ID로 역할 목록 조회
    public List<UserRoleResponseDto> getRolesByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        List<UserRole> userRoles = userRoleRepository.findAllByUser(user);

        return userRoles.stream()
                .map(ur -> {
                    Role role = ur.getRole();
                    return UserRoleResponseDto.builder()
                            .userId(user.getUserId())
                            .roleId(role.getRoleId())
                            .roleName(role.getRoleName())
                            .build();
                })
                .collect(Collectors.toList());
    }
}
