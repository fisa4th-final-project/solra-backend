package com.fisa.solra.domain.organization.controller;

import com.fisa.solra.domain.organization.dto.OrganizationRequestDto;
import com.fisa.solra.domain.organization.dto.OrganizationResponseDto;
import com.fisa.solra.domain.organization.service.OrganizationService;
import com.fisa.solra.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    // 조직 생성
    @PostMapping
    public ResponseEntity<ApiResponse<OrganizationResponseDto>> createOrganization(
            @RequestBody OrganizationRequestDto requestDto,
            @RequestHeader("Authorization") String authorizationHeader) {
        // Authorization: Bearer {token}
        String token = authorizationHeader.replace("Bearer ", "");
        String role = jwtTokenProvider.getRole(token);
        OrganizationResponseDto dto = organizationService.createOrganization(
                requestDto.getOrgName(), role);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(dto, "조직 생성 성공"));
    }
    // 전체 조회
    @GetMapping
    public ResponseEntity<ApiResponse<List<OrganizationResponseDto>>> getAllOrganizations() {
        List<OrganizationResponseDto> list = organizationService.getAllOrganizations();
        return ResponseEntity.ok(ApiResponse.success(list, "조직 목록 조회 성공")
        );
    }

    // 단일 조직 조회
    @GetMapping("/{orgId}")
    public ResponseEntity<ApiResponse<OrganizationResponseDto>> getOrganization(
            @PathVariable Long orgId) {
        OrganizationResponseDto dto = organizationService.getOrganizationById(orgId);
        return ResponseEntity
                .ok(ApiResponse.success(dto, "조직 조회 성공"));
    }
    // 조직명 수정
    @PatchMapping("/{orgId}")
    public ResponseEntity<ApiResponse<OrganizationResponseDto>> updateOrganization(
            @PathVariable Long orgId,
            @RequestBody OrganizationRequestDto requestDto) {
        OrganizationResponseDto dto = organizationService.updateOrganizationName(
                orgId, requestDto.getOrgName());
        return ResponseEntity.ok(ApiResponse.success(dto, "조직명 수정 성공"));
    }

    // 조직 삭제
    @DeleteMapping("/{orgId}")
    public ResponseEntity<ApiResponse<Void>> deleteOrganization(
            @PathVariable Long orgId) {
        organizationService.deleteOrganization(orgId);
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .body(ApiResponse.success(null, "조직 삭제 성공"));
    }

}
