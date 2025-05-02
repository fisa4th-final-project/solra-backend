package com.fisa.solra.domain.userpermission.repository;

import com.fisa.solra.domain.userpermission.entity.UserPermission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPermissionRepository extends JpaRepository<UserPermission, Long> {
}
