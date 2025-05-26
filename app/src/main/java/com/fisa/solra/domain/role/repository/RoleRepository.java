package com.fisa.solra.domain.role.repository;


import com.fisa.solra.domain.role.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
    boolean existsByRoleName(String roleName);
}
