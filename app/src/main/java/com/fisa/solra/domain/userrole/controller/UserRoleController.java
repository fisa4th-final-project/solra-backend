package com.fisa.solra.domain.userrole.controller;

import com.fisa.solra.domain.userrole.dto.UserRoleRequestDto;
import com.fisa.solra.domain.userrole.dto.UserRoleResponseDto;
import com.fisa.solra.domain.userrole.service.UserRoleService;
import com.fisa.solra.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/user-roles")
@RequiredArgsConstructor
public class UserRoleController {

    private final UserRoleService userRoleService;

    // 사용자 역할 부여
    @PostMapping
    @PreAuthorize("@permissionService.checkPermission('USER_ROLE_ASSIGN')")
    public ApiResponse<UserRoleResponseDto> assignRoleToUser(
            @RequestBody UserRoleRequestDto request) {
        UserRoleResponseDto responseDto = userRoleService.assignRole(request.getUserId(), request.getRoleId());
        return ApiResponse.success(responseDto, "사용자 역할 부여 성공");
    }

    // 사용자 역할 해제
    @DeleteMapping
    @PreAuthorize("@permissionService.checkPermission('USER_ROLE_REVOKE')")
    public ApiResponse<UserRoleResponseDto> removeUserRole(@RequestBody UserRoleRequestDto requestDto) {
        UserRoleResponseDto removed = userRoleService.removeUserRole(requestDto);
        return ApiResponse.success(removed, "사용자 역할 해제 성공");
    }

    // 사용자 역할 목록 조회
    @GetMapping("/{userId}")
    @PreAuthorize("@permissionService.checkPermission('USER_ROLE_READ')")
    public ApiResponse<List<UserRoleResponseDto>> getUerRoles(@PathVariable Long userId){
        List<UserRoleResponseDto> userRoles = userRoleService.getRolesByUserId(userId);
        return ApiResponse.success(userRoles, "사용자 역할 목록 조회 성공");

    }


}
