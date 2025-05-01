package com.fisa.solra.domain.user.controller;

import com.fisa.solra.domain.user.dto.LoginRequestDto;
import com.fisa.solra.domain.user.dto.UserLoginInfo;
import com.fisa.solra.domain.user.service.UserService;
import com.fisa.solra.global.jwt.JwtTokenProvider;
import com.fisa.solra.global.response.ApiResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/login")
    public ApiResponse<Void> login(
            @RequestBody @Valid LoginRequestDto request,
            HttpSession session
    ) {
        // 사용자 검증 + 로그인 정보 조회
        UserLoginInfo loginInfo = userService.login(request.getUserLoginId(), request.getPassword());

        // JWT 생성
        String token = jwtTokenProvider.generateToken(
                loginInfo.getUserId(),
                loginInfo.getOrgId(),
                loginInfo.getDeptId(),
                loginInfo.getRole()
        );

        // 세션에 저장
        session.setAttribute("jwtToken", token);

        return ApiResponse.success(null, "로그인 성공");
    }
}
