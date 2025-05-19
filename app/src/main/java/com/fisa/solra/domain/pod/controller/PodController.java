package com.fisa.solra.domain.pod.controller;

import com.fisa.solra.domain.pod.dto.PodResponseDto;
import com.fisa.solra.domain.pod.service.PodService;
import com.fisa.solra.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/clusters/{clusterId}/namespaces/{namespace}/pods")
@RequiredArgsConstructor
public class PodController {

    private final PodService podService;

    // ✅ 파드 전체 조회
    @GetMapping
    public ResponseEntity<ApiResponse<List<PodResponseDto>>> listPods(
            @PathVariable Long clusterId,
            @PathVariable String namespace) {

        List<PodResponseDto> pods = podService.getPods(clusterId, namespace);
        return ResponseEntity.ok(
                ApiResponse.success(pods, "파드 목록 조회 성공")
        );
    }

    // ✅ 단일 파드 조회
    @GetMapping("/{name}")
    public ResponseEntity<ApiResponse<PodResponseDto>> getPod(
            @PathVariable Long clusterId,
            @PathVariable String namespace,
            @PathVariable String name) {

        PodResponseDto pod = podService.getPod(clusterId, namespace, name);
        return ResponseEntity.ok(
                ApiResponse.success(pod, "파드 상세 조회 성공")
        );
    }
}