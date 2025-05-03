package com.fisa.solra.domain.user.controller;

import com.fisa.solra.domain.permission.entity.Permission;
import com.fisa.solra.domain.permission.service.PermissionService;
import com.fisa.solra.domain.user.dto.LoginRequestDto;
import com.fisa.solra.domain.user.dto.UserLoginInfo;
import com.fisa.solra.domain.user.dto.UserResponseDto;
import com.fisa.solra.domain.user.service.UserService;
import com.fisa.solra.global.exception.BusinessException;
import com.fisa.solra.global.exception.ErrorCode;
import com.fisa.solra.global.jwt.JwtTokenProvider;
import com.fisa.solra.global.response.ApiResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PermissionService permissionService;

    @PostMapping("/login")
    public ApiResponse<UserLoginInfo> login(
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

        // Spring Security 인증 객체 등록
        List<GrantedAuthority> authorities = permissionService.getAuthorities(loginInfo.getUserId());
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(loginInfo.getUserId(), null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 세션에 저장
        session.setAttribute("jwtToken", token);

        return ApiResponse.success(loginInfo, "로그인 성공");
    }

    @GetMapping("/me")
    public ApiResponse<UserResponseDto> getMyInfo(HttpSession session){
        String token = (String) session.getAttribute("jwtToken");
        if(token == null){
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }

        Long userId = jwtTokenProvider.getUserId(token);

        UserResponseDto userResponseDto = userService.getUserById(userId);
        return ApiResponse.success(userResponseDto, "사용자 정보 조회 성공");
    }
}
