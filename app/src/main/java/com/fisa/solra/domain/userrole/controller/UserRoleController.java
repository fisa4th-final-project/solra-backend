package com.fisa.solra.domain.userrole.controller;

import com.fisa.solra.domain.user.dto.UserResponseDto;
import com.fisa.solra.domain.userrole.dto.UserRoleAssignRequestDto;
import com.fisa.solra.domain.userrole.dto.UserRoleResponseDto;
import com.fisa.solra.domain.userrole.entity.UserRole;
import com.fisa.solra.domain.userrole.service.UserRoleService;
import com.fisa.solra.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user-roles")
@RequiredArgsConstructor
public class UserRoleController {

    private final UserRoleService userRoleService;

    @PostMapping
    public ApiResponse<UserRoleResponseDto> assignRoleToUser(
            @RequestBody UserRoleAssignRequestDto request) {
        UserRoleResponseDto responseDto = userRoleService.assignRole(request.getUserId(), request.getRoleId());
        return ApiResponse.success(responseDto, "사용자 역할 부여 성공");
    }

}
