package com.fisa.solra.domain.permission.service;


import com.fisa.solra.domain.permission.repository.PermissionRepository;
import com.fisa.solra.global.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final JwtTokenProvider jwtTokenProvider;

    // 로그인 시 Spring Security에서 호출
    public List<GrantedAuthority> getAuthorities(Long userId) {
        List<String> permissionNames = permissionRepository.findAllPermissionNamesByUserId(userId);

        return permissionNames.stream()
                .map(SimpleGrantedAuthority::new)  // 예: "CLUSTER_CREATE"
                .collect(Collectors.toList());
    }

    // @PreAuthorize에서 사용
    public boolean hasPermission(String permission) {
        Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getCredentials() == null) return false;

        String token = auth.getCredentials().toString();
        String role = jwtTokenProvider.getRole(token);

        // ✅ ROOT는 무조건 통과
        if ("ROOT".equalsIgnoreCase(role)) {
            return true;
        }

        // ✅ 그 외는 권한 이름 매칭 검사
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(permission));
    }
}
