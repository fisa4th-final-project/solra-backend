package com.fisa.solra.domain.userrole.repository;

import com.fisa.solra.domain.role.entity.Role;
import com.fisa.solra.domain.user.entity.User;
import com.fisa.solra.domain.userrole.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {
    boolean existsByUserAndRole(User user, Role role);

    Optional<UserRole> findByUserAndRole(User user, Role role);

    List<UserRole> findAllByUser(User user);
}
