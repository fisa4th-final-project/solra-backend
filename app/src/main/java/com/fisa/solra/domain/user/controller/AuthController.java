package com.fisa.solra.domain.user.controller;

import com.fisa.solra.domain.permission.service.PermissionService;
import com.fisa.solra.domain.role.entity.Role;
import com.fisa.solra.domain.user.dto.LoginRequestDto;
import com.fisa.solra.domain.user.dto.UserLoginInfo;
import com.fisa.solra.domain.user.dto.UserResponseDto;
import com.fisa.solra.domain.user.service.UserService;
import com.fisa.solra.global.security.UserPrincipal;
import com.fisa.solra.global.exception.BusinessException;
import com.fisa.solra.global.exception.ErrorCode;
import com.fisa.solra.global.jwt.JwtTokenProvider;
import com.fisa.solra.global.response.ApiResponse;
import com.fisa.solra.global.util.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PermissionService permissionService;

    @PostMapping("/login")
    public ApiResponse<UserLoginInfo> login(
            @RequestBody @Valid LoginRequestDto request
    ) {
        // 사용자 검증 + 로그인 정보 조회
        UserLoginInfo loginInfo = userService.login(request.getUserLoginId(), request.getPassword());

        List<String> roleNames = loginInfo.getRoles().stream()
                .map(Role::getRoleName)
                .collect(Collectors.toList());

        // JWT 발급
        String token = jwtTokenProvider.generateToken(
                loginInfo.getUserId(),
                loginInfo.getOrgId(),
                loginInfo.getDeptId(),
                roleNames
        );

        return ApiResponse.success(
                UserLoginInfo.builder()
                        .userId(loginInfo.getUserId())
                        .orgId(loginInfo.getOrgId())
                        .deptId(loginInfo.getDeptId())
                        .roles(loginInfo.getRoles())
                        .token(token)
                        .build(),
                "로그인 성공"
        );
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()") // 로그인된 사용자만 접근 가능
    public ApiResponse<UserResponseDto> getMyInfo() {
        Long userId = SecurityUtil.getUserId();
        UserResponseDto userResponseDto = userService.getUserById(userId);
        return ApiResponse.success(userResponseDto, "사용자 정보 조회 성공");
    }


    // 사용자 로그아웃
    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request, HttpServletResponse response){

        // 현재 인증된 사용자 정보 제거
        SecurityContextHolder.clearContext();

        // 세션 무효화
        request.getSession(false).invalidate();

        // (선택) JWT 쿠키 만료 처리
        // Cookie cookie = new Cookie("Authorization", null);
        // cookie.setMaxAge(0);
        // cookie.setPath("/");
        // response.addCookie(cookie);

        return ApiResponse.success(null, "로그아웃 성공");
    }
}
