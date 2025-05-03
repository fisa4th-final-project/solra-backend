package com.fisa.solra.domain.userrole.controller;

import com.fisa.solra.domain.userrole.dto.UserRoleRequestDto;
import com.fisa.solra.domain.userrole.dto.UserRoleResponseDto;
import com.fisa.solra.domain.userrole.service.UserRoleService;
import com.fisa.solra.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user-roles")
@RequiredArgsConstructor
public class UserRoleController {

    private final UserRoleService userRoleService;

    // 사용자 역할 부여
    @PostMapping
    public ApiResponse<UserRoleResponseDto> assignRoleToUser(
            @RequestBody UserRoleRequestDto request) {
        UserRoleResponseDto responseDto = userRoleService.assignRole(request.getUserId(), request.getRoleId());
        return ApiResponse.success(responseDto, "사용자 역할 부여 성공");
    }

    // 사용자 역할 해제
    @DeleteMapping
    public ApiResponse<UserRoleResponseDto> removeUserRole(@RequestBody UserRoleRequestDto requestDto) {
        UserRoleResponseDto removed = userRoleService.removeUserRole(requestDto);
        return ApiResponse.success(removed, "사용자 역할 해제 성공");
    }



}
