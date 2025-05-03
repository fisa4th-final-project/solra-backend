package com.fisa.solra.domain.permission.controller;

import com.fisa.solra.domain.permission.dto.PermissionRequestDto;
import com.fisa.solra.domain.permission.dto.PermissionResponseDto;
import com.fisa.solra.domain.permission.service.PermissionService;
import com.fisa.solra.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    //권한 생성
    @PostMapping
    public ResponseEntity<ApiResponse<PermissionResponseDto>> createPermission(
            @RequestBody PermissionRequestDto requestDto) {
        PermissionResponseDto dto = permissionService.createPermission(requestDto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(dto, "권한 생성 성공"));
    }

    //권한 전체 조회
    @GetMapping
    public ResponseEntity<ApiResponse<List<PermissionResponseDto>>> getPermissions() {
        List<PermissionResponseDto> list = permissionService.getAllPermissions();
        return ResponseEntity.ok(ApiResponse.success(list, "권한 목록 조회 성공"));
    }

    @PatchMapping("/{permissionId}")
    public ResponseEntity<ApiResponse<PermissionResponseDto>> updatePermission(
            @PathVariable Long permissionId,
            @RequestBody PermissionRequestDto requestDto) {
        PermissionResponseDto dto = permissionService.updatePermission(permissionId, requestDto);
        return ResponseEntity.ok(ApiResponse.success(dto, "권한 설명 수정 성공"));
    }
}
