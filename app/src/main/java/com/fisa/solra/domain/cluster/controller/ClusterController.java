package com.fisa.solra.domain.cluster.controller;

import com.fisa.solra.domain.cluster.dto.ClusterResponseDto;
import com.fisa.solra.domain.cluster.service.ClusterService;
import com.fisa.solra.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/clusters")
public class ClusterController {

    private final ClusterService clusterService;

    // ✅ 전체 클러스터 목록 조회
    @GetMapping
    public ResponseEntity<ApiResponse<List<ClusterResponseDto>>> listClusters() {
        List<ClusterResponseDto> result = clusterService.getAllClusterInfos();
        return ResponseEntity.ok(ApiResponse.success(result, "클러스터 전체 조회 성공"));
    }
}
