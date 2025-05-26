package com.fisa.solra.domain.permission.service;


import com.fisa.solra.domain.permission.dto.PermissionRequestDto;
import com.fisa.solra.domain.permission.dto.PermissionResponseDto;
import com.fisa.solra.domain.permission.entity.Permission;
import com.fisa.solra.domain.permission.repository.PermissionRepository;
import com.fisa.solra.global.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import com.fisa.solra.global.exception.BusinessException;
import com.fisa.solra.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

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
    //권한 생성
    @Transactional
    public PermissionResponseDto createPermission(PermissionRequestDto requestDto) {
        String name = requestDto.getPermissionName();
        String desc = requestDto.getDescription();

        // 1) 입력 유효성 검사
        if (name == null || name.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        name = name.trim();

        // 2) 중복 검사
        if (permissionRepository.existsByPermissionName(name)) {
            throw new BusinessException(ErrorCode.DUPLICATED_PERMISSION_NAME);
        }

        try {
            // 3) 권한 저장
            Permission entity = Permission.builder()
                    .permissionName(name)
                    .description(desc)
                    .build();
            Permission saved = permissionRepository.save(entity);
            return PermissionResponseDto.builder()
                    .permissionId(saved.getPermissionId())
                    .permissionName(saved.getPermissionName())
                    .description(saved.getDescription())
                    .build();
        } catch (DataIntegrityViolationException ex) {
            // DB 제약 위반 시
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    //권한 전체 조회
    public List<PermissionResponseDto> getAllPermissions() {
        List<PermissionResponseDto> list = permissionRepository.findAll().stream()
                .map(p -> PermissionResponseDto.builder()
                        .permissionId(p.getPermissionId())
                        .permissionName(p.getPermissionName())
                        .description(p.getDescription())
                        .build())
                .collect(Collectors.toList());
        if (list.isEmpty()) {
            throw new BusinessException(ErrorCode.PERMISSION_NOT_FOUND);
        }
        return list;
    }

    //권한 설명 수정
    @Transactional
    public PermissionResponseDto updatePermission(Long permissionId, PermissionRequestDto requestDto) {
        // 1) 존재 확인
        Permission entity = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PERMISSION_NOT_FOUND));

        // 2) 입력 검증
        String newDesc = requestDto.getDescription();
        if (newDesc == null || newDesc.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        newDesc = newDesc.trim();

        // 3) 변경된 설명인지 확인
        if (newDesc.equals(entity.getDescription())) {
            // 같은 설명이면 예외
            throw new BusinessException(ErrorCode.PERMISSION_ALREADY_ASSIGNED);
        }

        // 4) 업데이트
        entity.setDescription(newDesc);
        // @PreUpdate에 의해 updatedAt이 자동 갱신됩니다

        return PermissionResponseDto.builder()
                .permissionId(entity.getPermissionId())
                .permissionName(entity.getPermissionName())
                .description(entity.getDescription())
                .build();
    }

    //권한 삭제
    @Transactional
    public void deletePermission(Long permissionId) {
        if (!permissionRepository.existsById(permissionId)) {
            throw new BusinessException(ErrorCode.PERMISSION_NOT_FOUND);
        }
            permissionRepository.deleteById(permissionId);
            permissionRepository.flush();
    }
}

