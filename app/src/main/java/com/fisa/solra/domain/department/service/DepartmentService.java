package com.fisa.solra.domain.department.service;

import com.fisa.solra.domain.department.dto.DepartmentRequestDto;
import com.fisa.solra.domain.department.dto.DepartmentResponseDto;
import com.fisa.solra.domain.department.entity.Department;
import com.fisa.solra.domain.department.repository.DepartmentRepository;
import com.fisa.solra.domain.organization.repository.OrganizationRepository;
import com.fisa.solra.domain.permission.service.PermissionService;
import com.fisa.solra.global.exception.BusinessException;
import com.fisa.solra.global.exception.ErrorCode;
import com.fisa.solra.global.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final OrganizationRepository organizationRepository;
    private final PermissionService permissionService;

    // 전체 부서 조회
    public List<DepartmentResponseDto> getAllDepartments(Long orgId) {
        // 1) 권한 검사
        permissionService.checkPermission("DEPARTMENT_READ");

        // 2) 조직 필터 유무에 따라 조회
        List<Department> departments = (orgId != null)
                ? departmentRepository.findByOrganization_OrgId(orgId)
                : departmentRepository.findAll();

        // 3) 부서 리스트 반환
        List<DepartmentResponseDto> list = departments.stream()
                .map(dept -> DepartmentResponseDto.builder()
                        .orgId(dept.getOrganization().getOrgId())  // 조직 ID
                        .orgName(dept.getOrganization().getOrgName()) // 조직 이름
                        .deptId(dept.getDeptId())                        // 부서 ID
                        .deptName(dept.getDeptName())                  // 부서 이름
                        .build())
                .collect(Collectors.toList());

        if (list.isEmpty()) {
            throw new BusinessException(ErrorCode.DEPARTMENT_NOT_FOUND);
        }
        return list;
    }

    // 단일 부서 조회
    public DepartmentResponseDto getDepartmentById(Long deptId) {
        // 1) 권한 검사
        permissionService.checkPermission("DEPARTMENT_READ");

        // 2) 부서 조회
        Department dept = departmentRepository.findById(deptId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEPARTMENT_NOT_FOUND));

        // 3) 조직 일치 검사 (ROOT는 우회)
        if (!SecurityUtil.hasRole("ROOT") &&
                !Objects.equals(dept.getOrganization().getOrgId(), SecurityUtil.getOrgId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        // 4) 응답 생성
        return DepartmentResponseDto.builder()
                .deptId(dept.getDeptId())
                .orgId(dept.getOrganization().getOrgId())
                .orgName(dept.getOrganization().getOrgName())
                .deptName(dept.getDeptName())
                .build();
    }

    // 부서 생성
    @Transactional
    public DepartmentResponseDto createDepartment(DepartmentRequestDto requestDto) {
        Long orgId = requestDto.getOrganizationId();
        String name = requestDto.getDeptName();

        // 1) 권한 검사
        permissionService.checkPermission("DEPARTMENT_CREATE");

        // 2) 조직 존재 여부 확인
        if (orgId == null || !organizationRepository.existsById(orgId)) {
            throw new BusinessException(ErrorCode.ORGANIZATION_NOT_FOUND);
        }

        // 3) 조직 일치 검사 (ROOT는 우회)
        if (!SecurityUtil.hasRole("ROOT") &&
                !Objects.equals(orgId, SecurityUtil.getOrgId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        // 4) 부서명 유효성 검사
        if (name == null || name.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        name = name.trim();

        // 5) 중복 이름 검사 (같은 조직 내에서만 중복 확인)
        if (departmentRepository.existsByOrganizationOrgIdAndDeptName(orgId, name)) {
            throw new BusinessException(ErrorCode.DUPLICATED_DEPARTMENT_NAME);
        }

        // 6) 부서 생성 및 저장
        try {
            Department dept = Department.builder()
                    .organization(organizationRepository.getReferenceById(orgId))
                    .deptName(name)
                    .build();
            Department saved = departmentRepository.save(dept);
            return DepartmentResponseDto.builder()
                    .deptId(saved.getDeptId())
                    .orgId(saved.getOrganization().getOrgId())
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
                .orgId(dept.getOrganization().getOrgId())
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
