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
@RequestMapping("/api/namespaces/{namespace}/pods")
@RequiredArgsConstructor
public class PodController {

    private final PodService podService;

    // ✅ 파드 전체 조회
    @GetMapping
    public ResponseEntity<ApiResponse<List<PodResponseDto>>> listPods(@PathVariable String namespace) {
        List<PodResponseDto> pods = podService.getPods(namespace);
        return ResponseEntity.ok(ApiResponse.success(pods, "파드 목록 조회 성공"));
    }
}
