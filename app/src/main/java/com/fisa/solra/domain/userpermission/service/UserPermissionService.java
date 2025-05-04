package com.fisa.solra.domain.userpermission.service;

import com.fisa.solra.domain.permission.dto.PermissionResponseDto;
import com.fisa.solra.domain.permission.entity.Permission;
import com.fisa.solra.domain.permission.repository.PermissionRepository;
import com.fisa.solra.domain.user.entity.User;
import com.fisa.solra.domain.user.repository.UserRepository;
import com.fisa.solra.domain.userpermission.dto.UserPermissionRequestDto;
import com.fisa.solra.domain.userpermission.dto.UserPermissionResponseDto;
import com.fisa.solra.domain.userpermission.entity.UserPermission;
import com.fisa.solra.domain.userpermission.repository.UserPermissionRepository;
import com.fisa.solra.global.exception.BusinessException;
import com.fisa.solra.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserPermissionService {

    private final UserPermissionRepository userRoleRepository;

    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;
    private final UserPermissionRepository userPermissionRepository;

    // 사용자에게 권한 부여
    @Transactional
    public UserPermissionResponseDto assignPermission(UserPermissionRequestDto requestDto) {
        Long userId = requestDto.getUserId();
        Long permissionId = requestDto.getPermissionId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PERMISSION_NOT_FOUND));

        // 이미 가지고 있는 권한
        boolean exists = userPermissionRepository.existsByUserAndPermission(user, permission);
        if (exists) {
            throw new BusinessException(ErrorCode.USER_ALREADY_HAS_PERMISSION);
        }

        UserPermission userPermission = new UserPermission(user, permission);
        userPermissionRepository.save(userPermission);

        return new UserPermissionResponseDto(user.getUserId(), permission.getPermissionId());
    }

    // 사용자 권한 목록 조회
    @Transactional(readOnly = true)
    public List<PermissionResponseDto> getUserPermissions(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        List<UserPermission> userPermissions = userPermissionRepository.findAllByUser(user);

        return userPermissions.stream()
                .map(up -> new PermissionResponseDto(
                        up.getPermission().getPermissionId(),
                        up.getPermission().getPermissionName(),
                        up.getPermission().getDescription()
                ))
                .toList();
    }

}
