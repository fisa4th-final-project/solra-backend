package com.fisa.solra.domain.permission.service;


import com.fisa.solra.domain.permission.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;

    // 로그인 시 Spring Security에서 호출
    public List<GrantedAuthority> getAuthorities(Long userId) {
        List<String> permissionNames = permissionRepository.findAllPermissionNamesByUserId(userId);

        return permissionNames.stream()
                .map(SimpleGrantedAuthority::new)  // 예: "CLUSTER_CREATE"
                .collect(Collectors.toList());
    }

    // @PreAuthorize에서 사용
    public boolean hasPermission(String permission) {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(permission));
    }
}
