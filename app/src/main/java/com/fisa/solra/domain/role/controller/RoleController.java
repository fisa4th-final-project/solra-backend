package com.fisa.solra.domain.role.controller;

import com.fisa.solra.domain.role.dto.RoleRequestDto;
import com.fisa.solra.domain.role.dto.RoleResponseDto;
import com.fisa.solra.domain.role.service.RoleService;
import com.fisa.solra.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;


    // 역할 생성
    @PostMapping
    public ResponseEntity<ApiResponse<RoleResponseDto>> createRole(
            @RequestBody RoleRequestDto requestDto) {
        RoleResponseDto dto = roleService.createRole(requestDto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(dto, "역할 생성 성공"));
    }

    // 전체 역할 조회
    @GetMapping
    public ResponseEntity<ApiResponse<List<RoleResponseDto>>> getRoles() {
        List<RoleResponseDto> list = roleService.getAllRoles();
        return ResponseEntity.ok(ApiResponse.success(list, "역할 목록 조회 성공"));
    }

    // 단일 역할 조회
    @GetMapping("/{roleId}")
    public ResponseEntity<ApiResponse<RoleResponseDto>> getRole(
            @PathVariable Long roleId) {
        RoleResponseDto dto = roleService.getRoleById(roleId);
        return ResponseEntity.ok(ApiResponse.success(dto, "역할 조회 성공"));
    }

    // 역할 수정
    // 역할 수정 (설명만)
    @PatchMapping("/{roleId}")
    public ResponseEntity<ApiResponse<RoleResponseDto>> updateRole(
            @PathVariable Long roleId,
            @RequestBody RoleRequestDto requestDto) {
        RoleResponseDto dto = roleService.updateRole(roleId, requestDto);
        return ResponseEntity.ok(ApiResponse.success(dto, "역할 설명 수정 성공"));
    }

}
