package com.fisa.solra.domain.rolepermission.repository;


import com.fisa.solra.domain.rolepermission.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {
    boolean existsByRoleRoleIdAndPermissionPermissionId(Long roleId, Long permissionId);
    // 특정 역할의 권한 목록 조회
    List<RolePermission> findByRoleRoleId(Long roleId);
}
