package com.fisa.solra.domain.rolepermission.repository;


import com.fisa.solra.domain.rolepermission.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {
    boolean existsByRoleRoleIdAndPermissionPermissionId(Long roleId, Long permissionId);

}
