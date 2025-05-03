package com.fisa.solra.domain.role.service;


import com.fisa.solra.domain.role.dto.RoleRequestDto;
import com.fisa.solra.domain.role.dto.RoleResponseDto;
import com.fisa.solra.domain.role.entity.Role;
import com.fisa.solra.domain.role.repository.RoleRepository;
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
public class RoleService {

    private final RoleRepository roleRepository;
    // 역할 생성
    @Transactional
    public RoleResponseDto createRole(RoleRequestDto requestDto) {
        String name = requestDto.getRoleName();
        String desc = requestDto.getDescription();
        if (name == null || name.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        name = name.trim();
        if (roleRepository.existsByRoleName(name)) {
            throw new BusinessException(ErrorCode.DUPLICATED_PERMISSION_NAME);
        }
        try {
            Role entity = Role.builder()
                    .roleName(name)
                    .description(desc)
                    .build();
            Role saved = roleRepository.save(entity);
            return RoleResponseDto.builder()
                    .roleId(saved.getRoleId())
                    .roleName(saved.getRoleName())
                    .description(saved.getDescription())
                    .build();
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    // 전체 역할 조회
    public List<RoleResponseDto> getAllRoles() {
        List<RoleResponseDto> list = roleRepository.findAll().stream()
                .map(entity -> RoleResponseDto.builder()
                        .roleId(entity.getRoleId())
                        .roleName(entity.getRoleName())
                        .description(entity.getDescription())
                        .build())
                .collect(Collectors.toList());
        if (list.isEmpty()) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        }
        return list;
    }

    // 단일 역할 조회
    public RoleResponseDto getRoleById(Long roleId) {
        Role entity = roleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND));
        return RoleResponseDto.builder()
                .roleId(entity.getRoleId())
                .roleName(entity.getRoleName())
                .description(entity.getDescription())
                .build();
    }

    // 역할 수정 (부분 업데이트 지원)
    @Transactional
    public RoleResponseDto updateRole(Long roleId, RoleRequestDto updateDto) {
        Role entity = roleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND));

        // description 변경
        if (updateDto.getDescription() != null) {
            entity.setDescription(updateDto.getDescription());
        }

        roleRepository.flush();

        return RoleResponseDto.builder()
                .roleId(entity.getRoleId())
                .roleName(entity.getRoleName())
                .description(entity.getDescription())
                .build();
    }
}
