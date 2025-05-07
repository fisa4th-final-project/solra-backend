package com.fisa.solra.domain.department.service;

import com.fisa.solra.domain.department.dto.DepartmentRequestDto;
import com.fisa.solra.domain.department.dto.DepartmentResponseDto;
import com.fisa.solra.domain.department.entity.Department;
import com.fisa.solra.domain.department.repository.DepartmentRepository;
import com.fisa.solra.domain.organization.repository.OrganizationRepository;
import com.fisa.solra.global.exception.BusinessException;
import com.fisa.solra.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final OrganizationRepository organizationRepository;

    // 전체 부서 조회
    public List<DepartmentResponseDto> getAllDepartments() {
        List<DepartmentResponseDto> list = departmentRepository.findAll().stream()
                .map(dept -> DepartmentResponseDto.builder()
                        .deptId(dept.getDeptId())
                        .organizationId(dept.getOrganization().getOrgId())
                        .deptName(dept.getDeptName())
                        .build())
                .collect(Collectors.toList());

        if (list.isEmpty()) {
            throw new BusinessException(ErrorCode.DEPARTMENT_NOT_FOUND);
        }
        return list;
    }

    // 단일 부서 조회
    public DepartmentResponseDto getDepartmentById(Long deptId) {
        Department dept = departmentRepository.findById(deptId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEPARTMENT_NOT_FOUND));

        return DepartmentResponseDto.builder()
                .deptId(dept.getDeptId())
                .organizationId(dept.getOrganization().getOrgId())
                .deptName(dept.getDeptName())
                .build();
    }

    // 부서 생성
    @Transactional
    public DepartmentResponseDto createDepartment(DepartmentRequestDto requestDto) {
        Long orgId = requestDto.getOrganizationId();
        String name = requestDto.getDeptName();

        if (orgId == null || !organizationRepository.existsById(orgId)) {
            throw new BusinessException(ErrorCode.ORGANIZATION_NOT_FOUND);
        }
        if (name == null || name.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        name = name.trim();

        if (departmentRepository.existsByOrganizationOrgIdAndDeptName(orgId, name)) {
            throw new BusinessException(ErrorCode.DUPLICATED_DEPARTMENT_NAME);
        }

        try {
            Department dept = Department.builder()
                    .organization(organizationRepository.getReferenceById(orgId))
                    .deptName(name)
                    .build();
            Department saved = departmentRepository.save(dept);
            return DepartmentResponseDto.builder()
                    .deptId(saved.getDeptId())
                    .organizationId(saved.getOrganization().getOrgId())
                    .deptName(saved.getDeptName())
                    .build();
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.DEPARTMENT_CREATE_FAILED);
        }
    }

    // 부서명 수정
    @Transactional
    public DepartmentResponseDto updateDepartmentName(Long deptId, DepartmentRequestDto requestDto) {
        Department dept = departmentRepository.findById(deptId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEPARTMENT_NOT_FOUND));

        String newName = requestDto.getDeptName();
        if (newName == null || newName.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        newName = newName.trim();

        // 중복 검사: 같은 조직 내 동일한 이름이 이미 존재하는지 확인
        Long orgId = dept.getOrganization().getOrgId();
        boolean exists = departmentRepository.existsByOrganizationOrgIdAndDeptName(orgId, newName);
        if (exists && !dept.getDeptName().equals(newName)) {
            throw new BusinessException(ErrorCode.DUPLICATED_DEPARTMENT_NAME);
        }

        dept.setDeptName(newName);
        departmentRepository.flush();

        return DepartmentResponseDto.builder()
                .deptId(dept.getDeptId())
                .organizationId(dept.getOrganization().getOrgId())
                .deptName(dept.getDeptName())
                .build();
    }

    // 부서 삭제
    @Transactional
    public void deleteDepartment(Long deptId) {
        if (!departmentRepository.existsById(deptId)) {
            throw new BusinessException(ErrorCode.DEPARTMENT_NOT_FOUND);
        }
        departmentRepository.deleteById(deptId);
        departmentRepository.flush();
    }
}
