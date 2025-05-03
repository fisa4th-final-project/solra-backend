package com.fisa.solra.domain.permission.repository;

import com.fisa.solra.domain.permission.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
    @Query("""
        SELECT DISTINCT p.permissionName
        FROM UserRole ur
        JOIN ur.role r
        JOIN RolePermission rp ON rp.role = r
        JOIN Permission p ON p = rp.permission
        WHERE ur.user.userId = :userId
        UNION
        SELECT p.permissionName
        FROM UserPermission up
        JOIN up.permission p
        WHERE up.user.userId = :userId
    """)
    List<String> findAllPermissionNamesByUserId(@Param("userId") Long userId);
}

