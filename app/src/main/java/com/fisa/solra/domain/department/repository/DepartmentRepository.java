package com.fisa.solra.domain.department.repository;


import com.fisa.solra.domain.department.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    // 특정 조직 ID에 해당하는 부서 목록 조회
    List<Department> findByOrganization_OrgId(Long orgId);
    boolean existsByOrganizationOrgIdAndDeptName(Long orgId, String deptName);



}
