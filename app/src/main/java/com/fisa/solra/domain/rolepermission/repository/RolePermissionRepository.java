package com.fisa.solra.domain.rolepermission.repository;


import com.fisa.solra.domain.rolepermission.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {
    boolean existsByRoleRoleIdAndPermissionPermissionId(Long roleId, Long permissionId);
    // 특정 역할의 권한 목록 조회
    List<RolePermission> findByRoleRoleId(Long roleId);

    //특정 역할-권한 삭제
    @Modifying
    @Transactional
    @Query("DELETE FROM RolePermission rp WHERE rp.role.roleId = :roleId AND rp.permission.permissionId = :permissionId")
    void deleteByRoleRoleIdAndPermissionPermissionId(Long roleId, Long permissionId);
}
