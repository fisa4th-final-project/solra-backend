package com.fisa.solra.domain.cluster.controller;

import com.fisa.solra.domain.cluster.dto.ClusterRequestDto;
import com.fisa.solra.domain.cluster.dto.ClusterResponseDto;
import com.fisa.solra.domain.cluster.service.ClusterService;
import com.fisa.solra.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<ApiResponse<List<ClusterResponseDto>>> list() {
        List<ClusterResponseDto> all = clusterService.getClusters();
        return ResponseEntity.ok(ApiResponse.success(all, "클러스터 목록 조회에 성공했습니다."));
    }
    // ✅ 단일 조회
    @GetMapping("/{clusterId}")
    public ResponseEntity<ApiResponse<ClusterResponseDto>> get(
            @PathVariable Long clusterId) {
        ClusterResponseDto dto = clusterService.getCluster(clusterId);
        return ResponseEntity.ok(ApiResponse.success(dto, "클러스터 조회에 성공했습니다."));
    }

    // ✅ 클러스터 등록
    @PostMapping
    public ResponseEntity<ApiResponse<ClusterResponseDto>> createCluster(
            @Valid @RequestBody ClusterRequestDto dto) {
        ClusterResponseDto created = clusterService.createCluster(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "클러스터 생성에 성공했습니다."));
    }
}
