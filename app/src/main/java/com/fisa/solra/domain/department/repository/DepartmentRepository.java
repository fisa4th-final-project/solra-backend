package com.fisa.solra.domain.department.repository;


import com.fisa.solra.domain.department.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
}
