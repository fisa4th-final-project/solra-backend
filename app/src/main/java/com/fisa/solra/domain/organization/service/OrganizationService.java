// src/main/java/com/fisa/solra/domain/organization/service/OrganizationService.java
package com.fisa.solra.domain.organization.service;

import com.fisa.solra.domain.organization.dto.OrganizationResponseDto;
import com.fisa.solra.domain.organization.entity.Organization;
import com.fisa.solra.domain.organization.repository.OrganizationRepository;
import com.fisa.solra.global.exception.BusinessException;
import com.fisa.solra.global.exception.ErrorCode;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Builder
@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;

    // 조직 생성 (ROOT 권한 필요)
    @Transactional
    public OrganizationResponseDto createOrganization(String orgName, String role) {
        if (!"ROOT".equals(role)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        Organization org = Organization.builder()
                .orgName(orgName)
                .build();
        Organization saved = organizationRepository.save(org);
        return OrganizationResponseDto.builder()
                .orgId(saved.getOrgId())
                .orgName(saved.getOrgName())
                .build();
    }
    //조직 전체 조회
    public List<OrganizationResponseDto> getAllOrganizations() {
        return organizationRepository.findAll().stream()
                .map(org -> OrganizationResponseDto.builder()
                        .orgId(org.getOrgId())
                        .orgName(org.getOrgName())
                        .build())
                .collect(Collectors.toList());
    }

    //조직 상세 조회
    public OrganizationResponseDto getOrganizationById(Long id) {
        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORGANIZATION_NOT_FOUND));

        return OrganizationResponseDto.builder()
                .orgId(org.getOrgId())
                .orgName(org.getOrgName())
                .build();
    }
    // 조직명 수정
    @Transactional
    public OrganizationResponseDto updateOrganizationName(Long orgId, String newName) {
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORGANIZATION_NOT_FOUND));
        org.setOrgName(newName);
        organizationRepository.flush();
        return OrganizationResponseDto.builder()
                .orgId(org.getOrgId())
                .orgName(org.getOrgName())
                .build();
    }
    //조직 삭제
    @Transactional
    public void deleteOrganization(Long orgId) {
        if (!organizationRepository.existsById(orgId)) {
            throw new BusinessException(ErrorCode.ORGANIZATION_NOT_FOUND);
        }
        try {
            organizationRepository.deleteById(orgId);
            organizationRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.ORGANIZATION_DELETE_FAILED);
        }
    }
}
