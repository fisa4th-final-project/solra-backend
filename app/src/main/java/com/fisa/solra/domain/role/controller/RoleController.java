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
}
