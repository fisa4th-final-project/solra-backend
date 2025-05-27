package com.fisa.solra.domain.cluster.controller;

import com.fisa.solra.domain.cluster.dto.ClusterRequestDto;
import com.fisa.solra.domain.cluster.dto.ClusterResponseDto;
import com.fisa.solra.domain.cluster.service.ClusterService;
import com.fisa.solra.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/clusters")
public class ClusterController {


    private final ClusterService clusterService;

    // ✅ 전체 조회
    @GetMapping
    @PreAuthorize("@permissionService.checkPermission('CLUSTER_READ')")
    public ResponseEntity<ApiResponse<List<ClusterResponseDto>>> list(
            @RequestParam(required = false) Long orgId
    ) {
        List<ClusterResponseDto> all = clusterService.getClusters(orgId);
        return ResponseEntity.ok(ApiResponse.success(all, "클러스터 목록 조회에 성공했습니다."));
    }
    // ✅ 단일 조회
    @GetMapping("/{clusterId}")
    @PreAuthorize("@permissionService.checkPermission('CLUSTER_READ')")
    public ResponseEntity<ApiResponse<ClusterResponseDto>> get(
            @PathVariable Long clusterId) {
        ClusterResponseDto dto = clusterService.getCluster(clusterId);
        return ResponseEntity.ok(ApiResponse.success(dto, "클러스터 조회에 성공했습니다."));
    }

    // ✅ 클러스터 등록
    @PostMapping
    @PreAuthorize("@permissionService.checkPermission('CLUSTER_CREATE')")
    public ResponseEntity<ApiResponse<ClusterResponseDto>> createCluster(
            @Valid @RequestBody ClusterRequestDto dto) {
        ClusterResponseDto created = clusterService.createCluster(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "클러스터 생성에 성공했습니다."));
    }
    // ✅ 클러스터 수정
    @PatchMapping("/{clusterId}")
    @PreAuthorize("@permissionService.checkPermission('CLUSTER_UPDATE')")
    public ResponseEntity<ApiResponse<ClusterResponseDto>> updateCluster(
            @PathVariable Long clusterId,
            @RequestBody ClusterRequestDto dto) {

        ClusterResponseDto updated = clusterService.updateCluster(clusterId, dto);
        return ResponseEntity.ok(
                ApiResponse.success(updated, "클러스터 수정에 성공했습니다.")
        );
    }

    // ✅ 클러스터 삭제
    @DeleteMapping("/{clusterId}")
    @PreAuthorize("@permissionService.checkPermission('CLUSTER_DELETE')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long clusterId) {
        clusterService.delete(clusterId);
        return ResponseEntity.ok(ApiResponse.success(null, "클러스터 삭제에 성공했습니다."));
    }

    // 클러스터 연결 테스트 API
    @PostMapping("/{clusterId}/test-connection")
    @PreAuthorize("@permissionService.checkPermission('CLUSTER_READ')")
    public ResponseEntity<ApiResponse<String>> testClusterConnection(@PathVariable Long clusterId) {
        clusterService.testConnection(clusterId);
        return ResponseEntity.ok(ApiResponse.success(null, "클러스터 연결 테스트 성공"));
    }

}
