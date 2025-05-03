package com.fisa.solra.domain.permission.service;


import com.fisa.solra.domain.permission.dto.PermissionRequestDto;
import com.fisa.solra.domain.permission.dto.PermissionResponseDto;
import com.fisa.solra.domain.permission.entity.Permission;
import com.fisa.solra.domain.permission.repository.PermissionRepository;
import com.fisa.solra.global.exception.BusinessException;
import com.fisa.solra.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;

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
        Permission entity = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PERMISSION_NOT_FOUND));

        String newDesc = requestDto.getDescription();
        //입력 유효성 검사
        if (newDesc == null || newDesc.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        entity.setDescription(newDesc);

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
        try {
            permissionRepository.deleteById(permissionId);
            permissionRepository.flush();
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new BusinessException(ErrorCode.PERMISSION_DELETE_FAILED);
        }
    }
}

