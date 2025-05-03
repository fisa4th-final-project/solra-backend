package com.fisa.solra.domain.user.controller;

import com.fisa.solra.domain.user.dto.UserCreateRequestDto;
import com.fisa.solra.domain.user.dto.UserResponseDto;
import com.fisa.solra.domain.user.dto.UserUpdateRequestDto;
import com.fisa.solra.domain.user.service.UserService;
import com.fisa.solra.global.auth.AuthUtil;
import com.fisa.solra.global.jwt.JwtTokenProvider;
import com.fisa.solra.global.response.ApiResponse;
import jakarta.persistence.PrePersist;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    // 사용자 생성
    @PostMapping
    public ApiResponse<UserResponseDto> createUser(@RequestBody @Valid UserCreateRequestDto request){
        UserResponseDto userResponseDto = userService.createUser(request);
        return ApiResponse.success(userResponseDto, "사용자 생성 성공");
    }

    // 사용자 정보 수정
    @PreAuthorize("@permissionService.hasPermission('USER_CREATE')")
    @PatchMapping("/{userId}")
    public ApiResponse<UserResponseDto> updateUser(
            @PathVariable Long userId,
            @RequestBody @Valid UserUpdateRequestDto request,
            HttpSession session){

        // ROOT 권한 체크
        AuthUtil.assertRoot(session, jwtTokenProvider);

        UserResponseDto userResponseDto = userService.updateUser(userId, request);
        return ApiResponse.success(userResponseDto, "사용자 정보 수정 완료");
    }

}
