package com.fisa.solra.global.security;

import com.fisa.solra.domain.permission.service.PermissionService;
import com.fisa.solra.global.jwt.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtSessionAuthenticationFilter extends OncePerRequestFilter {

    private JwtTokenProvider jwtProvider;
    private final PermissionService permissionService;

    public JwtSessionAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                          PermissionService permissionService) {
        this.jwtProvider = jwtTokenProvider;
        this.permissionService = permissionService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String token = resolveToken(request);
        if (token != null && jwtProvider.validateToken(token)) {
            // JWT → 사용자 정보 복원
            Long userId = jwtProvider.getUserId(token);
            Long orgId = jwtProvider.getOrgId(token);
            Long deptId = jwtProvider.getDeptId(token);
            List<String> roles = jwtProvider.getRoles(token);
            List<GrantedAuthority> authorities = permissionService.getAuthorities(userId);

            // ✅ 모든 인자 전달
            UserPrincipal principal = new UserPrincipal(
                    userId,
                    orgId,
                    deptId,
                    roles,
                    authorities
            );

            // ✅ Authentication 생성
            Authentication auth =
                    new UsernamePasswordAuthenticationToken(principal, null, authorities);

            // ✅ SecurityContextHolder 등록
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }
    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
