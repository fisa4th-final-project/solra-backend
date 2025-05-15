package com.fisa.solra.domain.namespace.controller;

import com.fisa.solra.domain.namespace.dto.NamespaceRequestDto;
import com.fisa.solra.domain.namespace.dto.NamespaceResponseDto;
import com.fisa.solra.domain.namespace.service.NamespaceService;
import com.fisa.solra.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/namespaces")
@RequiredArgsConstructor
public class NamespaceController {

    private final NamespaceService namespaceService;

    // ✅ 네임스페이스 전체 조회
    @GetMapping
    public ResponseEntity<ApiResponse<List<NamespaceResponseDto>>> getNamespaces() {
        return ResponseEntity.ok(
                ApiResponse.success(namespaceService.getNamespaces(), "네임스페이스 리스트 조회에 성공했습니다.")
        );
    }
    // ✅ 단일 네임스페이스 상세 조회
    @GetMapping("/{name}")
    public ResponseEntity<ApiResponse<NamespaceResponseDto>> getNamespace(@PathVariable String name) {
        return ResponseEntity.ok(
                ApiResponse.success(namespaceService.getNamespace(name), "네임스페이스 상세 조회에 성공했습니다.")
        );
    }
    // ✅ 네임스페이스 생성
    @PostMapping
    public ResponseEntity<ApiResponse<NamespaceResponseDto>> createNamespace(@RequestBody NamespaceRequestDto dto) {
        return ResponseEntity.ok(
                ApiResponse.success(namespaceService.createNamespace(dto), "네임스페이스 생성에 성공했습니다.")
        );
    }

    // ✅ 네임스페이스 수정
    @PatchMapping("/{name}")
    public ResponseEntity<ApiResponse<NamespaceResponseDto>> updateNamespace(@PathVariable String name, @RequestBody NamespaceRequestDto dto) {
        return ResponseEntity.ok(
                ApiResponse.success(namespaceService.updateNamespace(name, dto), "네임스페이스 수정에 성공했습니다.")
        );
    }
}