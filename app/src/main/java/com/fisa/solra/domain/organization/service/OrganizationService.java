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
        organizationRepository.deleteById(orgId);
        organizationRepository.flush();
    }
}
