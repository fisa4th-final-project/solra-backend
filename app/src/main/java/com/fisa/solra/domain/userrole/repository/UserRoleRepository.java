package com.fisa.solra.domain.userrole.repository;

import com.fisa.solra.domain.userrole.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {
}
