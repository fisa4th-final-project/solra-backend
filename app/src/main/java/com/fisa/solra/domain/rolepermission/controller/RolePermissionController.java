package com.fisa.solra.domain.rolepermission.controller;

import com.fisa.solra.domain.permission.dto.PermissionResponseDto;
import com.fisa.solra.domain.rolepermission.dto.RolePermissionRequestDto;
import com.fisa.solra.domain.rolepermission.dto.RolePermissionResponseDto;
import com.fisa.solra.domain.rolepermission.service.RolePermissionService;
import com.fisa.solra.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/role-permissions")
@RequiredArgsConstructor
public class RolePermissionController {

    private final RolePermissionService rolePermissionService;

    //역할에 권한 추가
    @PostMapping
    public ResponseEntity<ApiResponse<RolePermissionResponseDto>> assignPermission(
            @RequestBody RolePermissionRequestDto requestDto) {
        RolePermissionResponseDto dto = rolePermissionService.assignPermission(requestDto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(dto, "권한이 역할에 성공적으로 부여되었습니다."));
    }

    //특정 역할에 부여된 권한 조회
    @GetMapping("/{roleId}/permissions")
    public ResponseEntity<ApiResponse<List<PermissionResponseDto>>> getRolePermissions(
            @PathVariable Long roleId) {
        List<PermissionResponseDto> list = rolePermissionService.getPermissionsByRole(roleId);
        return ResponseEntity.ok(ApiResponse.success(list, "역할 권한 목록 조회 성공"));
    }
}
