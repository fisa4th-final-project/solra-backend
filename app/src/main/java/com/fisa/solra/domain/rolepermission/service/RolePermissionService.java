package com.fisa.solra.domain.rolepermission.service;

import com.fisa.solra.domain.permission.entity.Permission;
import com.fisa.solra.domain.permission.repository.PermissionRepository;
import com.fisa.solra.domain.role.entity.Role;
import com.fisa.solra.domain.role.repository.RoleRepository;
import com.fisa.solra.domain.rolepermission.dto.RolePermissionRequestDto;
import com.fisa.solra.domain.rolepermission.dto.RolePermissionResponseDto;
import com.fisa.solra.domain.rolepermission.entity.RolePermission;
import com.fisa.solra.domain.rolepermission.repository.RolePermissionRepository;
import com.fisa.solra.global.exception.BusinessException;
import com.fisa.solra.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RolePermissionService {

    private final RolePermissionRepository rolePermissionRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Transactional
    public RolePermissionResponseDto assignPermission(RolePermissionRequestDto req) {
        Long roleId = req.getRoleId();
        Long permId = req.getPermissionId();

        // 1) 존재 확인
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND));
        Permission perm = permissionRepository.findById(permId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PERMISSION_NOT_FOUND));

        // 2) 중복 확인
        if (rolePermissionRepository.existsByRoleRoleIdAndPermissionPermissionId(roleId, permId)) {
            throw new BusinessException(ErrorCode.ROLE_ALREADY_ASSIGNED);
        }

        // 3) 저장
        RolePermission rp = rolePermissionRepository.save(
                RolePermission.builder()
                        .role(role)
                        .permission(perm)
                        .build()
        );

        return RolePermissionResponseDto.builder()
                .rolePermissionId(rp.getRolePermissionId())
                .roleId(roleId)
                .permissionId(permId)
                .build();
    }

}
