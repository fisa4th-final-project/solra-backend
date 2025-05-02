package com.fisa.solra.domain.department.repository;


import com.fisa.solra.domain.department.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    List<Department> findByOrganizationOrgId(Long orgId);
    boolean existsByOrganizationOrgIdAndDeptName(Long orgId, String deptName);

}
