package com.fisa.solra.domain.cluster.controller;

import com.fisa.solra.domain.cluster.dto.ClusterRequestDto;
import com.fisa.solra.domain.cluster.dto.ClusterResponseDto;
import com.fisa.solra.domain.cluster.repository.ClusterRepository;
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

    private final ClusterRepository clusterRepository;
    private final ClusterService clusterService;

    // ✅ 클러스터 전체 조회
    @GetMapping
    public List<ClusterResponseDto> getAllClusters() {
        return clusterRepository.findAll().stream()
                .map(ClusterResponseDto::fromEntity)
                .collect(Collectors.toList());
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
