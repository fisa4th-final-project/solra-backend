package com.fisa.solra.domain.rolepermission.controller;

import com.fisa.solra.domain.rolepermission.dto.RolePermissionRequestDto;
import com.fisa.solra.domain.rolepermission.dto.RolePermissionResponseDto;
import com.fisa.solra.domain.rolepermission.service.RolePermissionService;
import com.fisa.solra.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
