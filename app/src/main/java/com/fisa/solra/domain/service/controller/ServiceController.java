package com.fisa.solra.domain.service.controller;

import com.fisa.solra.domain.service.dto.ServiceRequestDto;
import com.fisa.solra.domain.service.dto.ServiceResponseDto;
import com.fisa.solra.domain.service.service.ServiceService;
import com.fisa.solra.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/namespaces/{namespace}/services")
@RequiredArgsConstructor
public class ServiceController {

    private final ServiceService serviceService;

    /**
     * ✅ 네임스페이스 내 전체 서비스 목록 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ServiceResponseDto>>> list(@PathVariable String namespace) {
        return ResponseEntity.ok(
                ApiResponse.success(serviceService.getServices(namespace), "서비스 리스트 조회에 성공했습니다.")
        );
    }

    // ✅ 단일 서비스 상세 조회
    @GetMapping("/{name}")
    public ResponseEntity<ApiResponse<ServiceResponseDto>> get(@PathVariable String namespace, @PathVariable String name) {
        return ResponseEntity.ok(
                ApiResponse.success(serviceService.getService(namespace, name), "서비스 상세 조회에 성공했습니다.")
        );
    }

    // ✅ 서비스 생성
    @PostMapping
    public ResponseEntity<ApiResponse<ServiceResponseDto>> create(@PathVariable String namespace, @Valid @RequestBody ServiceRequestDto req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(serviceService.createService(namespace, req), "서비스 생성에 성공했습니다."));
    }

    // ✅ 서비스 수정
    @PatchMapping("/{name}")
    public ResponseEntity<ApiResponse<ServiceResponseDto>> update(@PathVariable String namespace, @PathVariable String name, @Valid @RequestBody ServiceRequestDto req) {
        return ResponseEntity.ok(
                ApiResponse.success(serviceService.updateService(namespace, name, req), "서비스 수정에 성공했습니다.")
        );
    }
}