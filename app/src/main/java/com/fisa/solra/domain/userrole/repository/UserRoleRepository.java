package com.fisa.solra.domain.userrole.repository;

import com.fisa.solra.domain.role.entity.Role;
import com.fisa.solra.domain.user.entity.User;
import com.fisa.solra.domain.userrole.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {
    boolean existsByUserAndRole(User user, Role role);
}
