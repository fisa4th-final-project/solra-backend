// src/main/java/com/fisa/solra/domain/organization/service/OrganizationService.java
package com.fisa.solra.domain.organization.service;

import com.fisa.solra.domain.organization.dto.OrganizationResponseDto;
import com.fisa.solra.domain.organization.entity.Organization;
import com.fisa.solra.domain.organization.repository.OrganizationRepository;
import com.fisa.solra.domain.permission.service.PermissionService;
import com.fisa.solra.global.exception.BusinessException;
import com.fisa.solra.global.exception.ErrorCode;
import com.fisa.solra.global.util.SecurityUtil;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Builder
@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final PermissionService permissionService;

    // 조직 생성 (ROOT 권한 필요)
    @Transactional
    public OrganizationResponseDto createOrganization(String orgName) {

        // 이름 유효성 검증
        if (orgName == null || orgName.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        orgName = orgName.trim();

        // 중복 검사
        if (organizationRepository.existsByOrgName(orgName)) {
            throw new BusinessException(ErrorCode.DUPLICATED_ORGANIZATION_NAME);
        }

        try {
            Organization org = Organization.builder()
                    .orgName(orgName)
                    .build();
            Organization saved = organizationRepository.save(org);
            return OrganizationResponseDto.builder()
                    .orgId(saved.getOrgId())
                    .orgName(saved.getOrgName())
                    .build();
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.ORGANIZATION_CREATE_FAILED);
        }
    }
    //조직 전체 조회
    public List<OrganizationResponseDto> getAllOrganizations() {
        // 1) 엔티티 리스트 먼저 조회
        List<Organization> entities = organizationRepository.findAll();
        if (entities.isEmpty()) {
            throw new BusinessException(ErrorCode.ORGANIZATION_NOT_FOUND);
        }

        // 2) DTO로 변환
        return entities.stream()
                .map(org -> OrganizationResponseDto.builder()
                        .orgId(org.getOrgId())
                        .orgName(org.getOrgName())
                        .build())
                .collect(Collectors.toList());
    }

    //조직 상세 조회
    public OrganizationResponseDto getOrganizationById(Long id) {
        // 1) 권한 검사 (예외 기반)
        permissionService.checkPermission("ORG_READ");

        // 2) 대상 조직 조회
        Organization organization  = organizationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORGANIZATION_NOT_FOUND));

        // 3) 조직 일치 여부 확인 (ROOT는 우회)
        if (!SecurityUtil.hasRole("ROOT") &&
                !Objects.equals(SecurityUtil.getOrgId(), organization.getOrgId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        // 4) 응답 반환
        return OrganizationResponseDto.builder()
                .orgId(organization .getOrgId())
                .orgName(organization .getOrgName())
                .build();
    }
    // 조직명 수정
    @Transactional
    public OrganizationResponseDto updateOrganizationName(Long orgId, String newName) {
        // 1) 권한 검사
        permissionService.checkPermission("ORG_UPDATE");

        // 2) 조직 조회
        Organization organization = organizationRepository.findById(orgId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORGANIZATION_NOT_FOUND));

        // 3) 조직 일치 확인 (ROOT는 우회)
        if (!SecurityUtil.hasRole("ROOT") &&
                !Objects.equals(SecurityUtil.getOrgId(), organization.getOrgId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        // 4) 이름 변경
        organization.setOrgName(newName);

        // 5) 응답 반환
        return OrganizationResponseDto.builder()
                .orgId(organization.getOrgId())
                .orgName(organization.getOrgName())
                .build();
    }
    //조직 삭제
    @Transactional
    public void deleteOrganization(Long orgId) {
        // 1) 권한 검사
        permissionService.checkPermission("ORG_DELETE");

        // 2) 조직 조회
        Organization organization = organizationRepository.findById(orgId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORGANIZATION_NOT_FOUND));

        // 3) 조직 일치 확인 (ROOT는 우회)
        if (!SecurityUtil.hasRole("ROOT") &&
                !Objects.equals(SecurityUtil.getOrgId(), organization.getOrgId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

/*        // 4) 자식 리소스 존재 여부 확인 (부서 또는 사용자)
        if (organization.hasChildren()) { // 예: 부서 또는 사용자 존재 여부 확인
            throw new BusinessException(ErrorCode.ORGANIZATION_DELETE_CONFLICT);
        }*/

        // 4) 삭제 처리
        organizationRepository.deleteById(orgId);
    }
}
