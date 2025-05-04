package com.fisa.solra.domain.userpermission.repository;

import com.fisa.solra.domain.permission.entity.Permission;
import com.fisa.solra.domain.user.entity.User;
import com.fisa.solra.domain.userpermission.entity.UserPermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserPermissionRepository extends JpaRepository<UserPermission, Long> {
    boolean existsByUserAndPermission(User user, Permission permission);
    List<UserPermission> findAllByUser(User user);

    Optional<UserPermission> findByUserAndPermission(User user, Permission permission);
}
