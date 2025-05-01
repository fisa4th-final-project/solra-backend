package com.fisa.solra.domain.user.controller;

import com.fisa.solra.domain.user.dto.UserCreateRequestDto;
import com.fisa.solra.domain.user.dto.UserResponseDto;
import com.fisa.solra.domain.user.service.UserService;
import com.fisa.solra.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ApiResponse<UserResponseDto> createUser(@RequestBody @Valid UserCreateRequestDto request){
        UserResponseDto created = userService.createUser(request);
        return ApiResponse.success(created, "사용자 생성 성공");
    }
}
