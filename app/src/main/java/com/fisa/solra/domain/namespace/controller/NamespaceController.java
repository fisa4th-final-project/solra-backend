package com.fisa.solra.domain.namespace.controller;

import com.fisa.solra.domain.namespace.dto.NamespaceResponseDto;
import com.fisa.solra.domain.namespace.service.NamespaceService;
import com.fisa.solra.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}