package com.fisa.solra.domain.rolepermission.service;

import com.fisa.solra.domain.permission.dto.PermissionResponseDto;
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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RolePermissionService {

    private final RolePermissionRepository rolePermissionRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    //역할에 권한 추가
    @Transactional
    public List<RolePermissionResponseDto> assignPermission(RolePermissionRequestDto req) {
        Long roleId = req.getRoleId();
        List<Long> permissionIds = req.getPermissionIds();

        // 1) 존재 확인
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND));

        List<RolePermissionResponseDto> resultList = new ArrayList<>();

        for (Long permId : permissionIds) {
            // 2) 권한 존재 확인
            Permission perm = permissionRepository.findById(permId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.PERMISSION_NOT_FOUND));

            // 3) 중복 확인
            boolean exists = rolePermissionRepository.existsByRoleRoleIdAndPermissionPermissionId(roleId, permId);
            if (exists) continue;  // 이미 할당된 권한은 건너뜀

            // 4) 저장
            RolePermission rp = rolePermissionRepository.save(
                    RolePermission.builder()
                            .role(role)
                            .permission(perm)
                            .build()
            );

            resultList.add(RolePermissionResponseDto.builder()
                    .rolePermissionId(rp.getRolePermissionId())
                    .roleId(roleId)
                    .permissionId(permId)
                    .build());
        }

        return resultList;
    }

    //특정 역할에 부여된 권한 조회
    public List<PermissionResponseDto> getPermissionsByRole(Long roleId) {
        // 역할 존재 확인
        roleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND));

        // 매핑 테이블에서 조회 후 PermissionResponseDto로 변환
        return rolePermissionRepository.findByRoleRoleId(roleId).stream()
                .map(rp -> PermissionResponseDto.builder()
                        .permissionId(rp.getPermission().getPermissionId())
                        .permissionName(rp.getPermission().getPermissionName())
                        .description(rp.getPermission().getDescription())
                        .build())
                .collect(Collectors.toList());
    }

    //특정 역할에 부여된 권한 삭제
    @Transactional
    public void removePermission(RolePermissionRequestDto req) {
        Long roleId = req.getRoleId();
        Long permId = req.getPermissionId();

        // 매핑 존재 확인
        if (!rolePermissionRepository.existsByRoleRoleIdAndPermissionPermissionId(roleId, permId)) {
            throw new BusinessException(ErrorCode.ROLE_PERMISSION_NOT_FOUND);
        }

        // join테이블 레코드 삭제
        rolePermissionRepository.deleteByRoleRoleIdAndPermissionPermissionId(roleId, permId);
        rolePermissionRepository.flush();
    }
}
