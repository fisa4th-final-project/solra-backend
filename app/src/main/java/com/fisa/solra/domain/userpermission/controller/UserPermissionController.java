package com.fisa.solra.domain.userpermission.controller;

import com.fisa.solra.domain.permission.dto.PermissionResponseDto;
import com.fisa.solra.domain.userpermission.dto.UserPermissionRequestDto;
import com.fisa.solra.domain.userpermission.dto.UserPermissionResponseDto;
import com.fisa.solra.domain.userpermission.service.UserPermissionService;
import com.fisa.solra.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user-permissions")
@RequiredArgsConstructor
public class UserPermissionController {

    private final UserPermissionService userPermissionService;

    // 사용자에게 권한 부여
    @PostMapping
    public ApiResponse<UserPermissionResponseDto> assignPermission(@RequestBody UserPermissionRequestDto requestDto) {
        UserPermissionResponseDto userPermissionResponseDto = userPermissionService.assignPermission(requestDto);
        return ApiResponse.success(userPermissionResponseDto, "사용자에게 권한이 성공적으로 부여되었습니다.");
    }

}
