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
import com.fisa.solra.global.util.SecurityUtil;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserRoleService {

    private final UserRoleRepository userRoleRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    // 사용자에게 역할 부여
    @Transactional
    public UserRoleResponseDto assignRole(Long userId, Long roleId) {
        // 1) 대상 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 2) 조직 일치 검사 (ROOT는 우회)
        if (!SecurityUtil.hasRole("ROOT") &&
                !Objects.equals(user.getOrganization().getOrgId(), SecurityUtil.getOrgId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        // 3) 중복 역할 검사
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND));
        boolean exists = userRoleRepository.existsByUserAndRole(user, role);
        if (exists) {
            throw new BusinessException(ErrorCode.ROLE_ALREADY_ASSIGNED);
        }

        // 4) 역할 부여
        UserRole userRole = UserRole.builder()
                .user(user)
                .role(role)
                .build();
        userRoleRepository.save(userRole);

        // 5) 응답 반환
        return UserRoleResponseDto.builder()
                .userId(user.getUserId())
                .roleId(role.getRoleId())
                .roleName(role.getRoleName())
                .build();
    }

    // 사용자 역할 해제 서비스 로직
    @Transactional
    public UserRoleResponseDto removeUserRole(UserRoleRequestDto requestDto) {
        // 1) 대상 사용자 조회
        User user = userRepository.findById(requestDto.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 2) 조직 일치 검사 (ROOT 우회)
        if (!SecurityUtil.hasRole("ROOT") &&
                !Objects.equals(user.getOrganization().getOrgId(), SecurityUtil.getOrgId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        // 3) 대상 역할 조회
        Role role = roleRepository.findById(requestDto.getRoleId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND));

        // 4) 매핑 여부 확인
        UserRole userRole = userRoleRepository.findByUserAndRole(user, role)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND)); // or ROLE_NOT_ASSIGNED

        // 5) 제거
        userRoleRepository.delete(userRole);

        // 6) 응답 반환
        return UserRoleResponseDto.builder()
                .userId(user.getUserId())
                .roleId(role.getRoleId())
                .roleName(role.getRoleName())
                .build();
    }

    // 사용자 ID로 역할 목록 조회
    public List<UserRoleResponseDto> getRolesByUserId(Long userId) {
        // 1) 대상 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 2) 조직 일치 검사 (ROOT 우회)
        if (!SecurityUtil.hasRole("ROOT") &&
                !Objects.equals(user.getOrganization().getOrgId(), SecurityUtil.getOrgId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        // 3) 역할 조회
        List<UserRole> userRoles = userRoleRepository.findAllByUser(user);

        // 4) 응답 반환
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
