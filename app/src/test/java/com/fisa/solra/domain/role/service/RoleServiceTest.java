// src/test/java/com/fisa/solra/domain/role/service/RoleServiceTest.java
package com.fisa.solra.domain.role.service;

import com.fisa.solra.domain.role.dto.RoleRequestDto;
import com.fisa.solra.domain.role.dto.RoleResponseDto;
import com.fisa.solra.domain.role.entity.Role;
import com.fisa.solra.domain.role.repository.RoleRepository;
import com.fisa.solra.global.exception.BusinessException;
import com.fisa.solra.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.dao.DataIntegrityViolationException;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.DisplayName.class)
class RoleServiceTest {

    @InjectMocks
    private RoleService roleService;

    @Mock
    private RoleRepository roleRepository;

    // ==========================
    // TC_RS_01: createRole(...)
    // ==========================

    @Test
    @DisplayName("TC_RS_01_01: 역할 생성 → 성공")
    void createRole_success() {
        // given
        RoleRequestDto request = RoleRequestDto.builder()
                .roleName("ADMIN")
                .description("관리자 권한")
                .build();

        Role savedEntity = Role.builder()
                .roleId(100L)
                .roleName("ADMIN")
                .description("관리자 권한")
                .build();

        when(roleRepository.existsByRoleName("ADMIN")).thenReturn(false);
        when(roleRepository.save(any(Role.class))).thenReturn(savedEntity);

        // when
        RoleResponseDto dto = roleService.createRole(request);

        // then
        System.out.println("✅ TC_RS_01_01 - 생성된 Role ID: " + dto.getRoleId());
        assertEquals(100L, dto.getRoleId());
        assertEquals("ADMIN", dto.getRoleName());
        assertEquals("관리자 권한", dto.getDescription());
        verify(roleRepository).existsByRoleName("ADMIN");
        verify(roleRepository).save(any(Role.class));
    }

    @Test
    @DisplayName("TC_RS_01_02: roleName이 null 또는 빈 문자열 → INVALID_INPUT 예외")
    void createRole_invalidInput() {
        // case1: roleName == null
        RoleRequestDto req1 = RoleRequestDto.builder()
                .roleName(null)
                .description("설명")
                .build();

        BusinessException ex1 = assertThrows(
                BusinessException.class,
                () -> roleService.createRole(req1)
        );
        System.out.println("⚠️ TC_RS_01_02-1 - 예외 코드: " + ex1.getErrorCode());
        assertEquals(ErrorCode.INVALID_INPUT, ex1.getErrorCode());

        // case2: roleName == "   " (빈 문자열)
        RoleRequestDto req2 = RoleRequestDto.builder()
                .roleName("   ")
                .description("설명")
                .build();

        BusinessException ex2 = assertThrows(
                BusinessException.class,
                () -> roleService.createRole(req2)
        );
        System.out.println("⚠️ TC_RS_01_02-2 - 예외 코드: " + ex2.getErrorCode());
        assertEquals(ErrorCode.INVALID_INPUT, ex2.getErrorCode());
    }

    @Test
    @DisplayName("TC_RS_01_03: 중복된 roleName → DUPLICATED_PERMISSION_NAME 예외")
    void createRole_duplicateName() {
        // given
        RoleRequestDto request = RoleRequestDto.builder()
                .roleName("USER")
                .description("일반 사용자")
                .build();

        when(roleRepository.existsByRoleName("USER")).thenReturn(true);

        // when & then
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> roleService.createRole(request)
        );
        System.out.println("⚠️ TC_RS_01_03 - 예외 코드: " + ex.getErrorCode());
        assertEquals(ErrorCode.DUPLICATED_PERMISSION_NAME, ex.getErrorCode());
        verify(roleRepository).existsByRoleName("USER");
    }

    @Test
    @DisplayName("TC_RS_01_04: DataIntegrityViolationException 발생 → INTERNAL_SERVER_ERROR 예외")
    void createRole_dataIntegrityViolation() {
        // given
        RoleRequestDto request = RoleRequestDto.builder()
                .roleName("MANAGER")
                .description("매니저 권한")
                .build();

        when(roleRepository.existsByRoleName("MANAGER")).thenReturn(false);
        when(roleRepository.save(any(Role.class)))
                .thenThrow(new DataIntegrityViolationException("DB error"));

        // when & then
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> roleService.createRole(request)
        );
        System.out.println("⚠️ TC_RS_01_04 - 예외 코드: " + ex.getErrorCode());
        assertEquals(ErrorCode.INTERNAL_SERVER_ERROR, ex.getErrorCode());
        verify(roleRepository).existsByRoleName("MANAGER");
        verify(roleRepository).save(any(Role.class));
    }

    // ================================
    // TC_RS_02: getAllRoles()
    // ================================

    @Test
    @DisplayName("TC_RS_02_01: 저장된 Role이 있을 때 전체 조회 → 성공")
    void getAllRoles_success() {
        // given
        Role r1 = Role.builder().roleId(1L).roleName("A").description("descA").build();
        Role r2 = Role.builder().roleId(2L).roleName("B").description("descB").build();

        when(roleRepository.findAll()).thenReturn(List.of(r1, r2));

        // when
        List<RoleResponseDto> list = roleService.getAllRoles();

        // then
        System.out.println("✅ TC_RS_02_01 - 조회된 Role 수: " + list.size());
        assertEquals(2, list.size());
        assertEquals("A", list.get(0).getRoleName());
        assertEquals("B", list.get(1).getRoleName());
        verify(roleRepository).findAll();
    }

    @Test
    @DisplayName("TC_RS_02_02: 저장된 Role이 없을 때 → ROLE_NOT_FOUND 예외")
    void getAllRoles_notFound() {
        // given
        when(roleRepository.findAll()).thenReturn(Collections.emptyList());

        // when & then
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> roleService.getAllRoles()
        );
        System.out.println("⚠️ TC_RS_02_02 - 예외 코드: " + ex.getErrorCode());
        assertEquals(ErrorCode.ROLE_NOT_FOUND, ex.getErrorCode());
        verify(roleRepository).findAll();
    }

    // ================================
    // TC_RS_03: getRoleById(Long roleId)
    // ================================

    @Test
    @DisplayName("TC_RS_03_01: 존재하는 Role 조회 → 성공")
    void getRoleById_success() {
        // given
        Role entity = Role.builder()
                .roleId(50L)
                .roleName("EDITOR")
                .description("편집 권한")
                .build();

        when(roleRepository.findById(50L)).thenReturn(Optional.of(entity));

        // when
        RoleResponseDto dto = roleService.getRoleById(50L);

        // then
        System.out.println("✅ TC_RS_03_01 - 조회된 RoleName: " + dto.getRoleName());
        assertEquals("EDITOR", dto.getRoleName());
        assertEquals("편집 권한", dto.getDescription());
        verify(roleRepository).findById(50L);
    }

    @Test
    @DisplayName("TC_RS_03_02: 존재하지 않는 Role 조회 → ROLE_NOT_FOUND 예외")
    void getRoleById_notFound() {
        // given
        when(roleRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> roleService.getRoleById(999L)
        );
        System.out.println("⚠️ TC_RS_03_02 - 예외 코드: " + ex.getErrorCode());
        assertEquals(ErrorCode.ROLE_NOT_FOUND, ex.getErrorCode());
        verify(roleRepository).findById(999L);
    }

    // ===============================================
    // TC_RS_04: updateRole(Long roleId, RoleRequestDto)
    // ===============================================

    @Test
    @DisplayName("TC_RS_04_01: 역할 설명만 부분 수정 → 성공")
    void updateRole_success() {
        // given
        RoleRequestDto updateDto = RoleRequestDto.builder()
                .roleName(null)        // roleName 변경 없이 description만 변경
                .description("새 설명")
                .build();

        Role existing = Role.builder()
                .roleId(200L)
                .roleName("TEST")
                .description("기존 설명")
                .build();

        when(roleRepository.findById(200L)).thenReturn(Optional.of(existing));
        doNothing().when(roleRepository).flush();

        // when
        RoleResponseDto dto = roleService.updateRole(200L, updateDto);

        // then
        System.out.println("✅ TC_RS_04_01 - 수정된 설명: " + dto.getDescription());
        assertEquals("새 설명", dto.getDescription());
        assertEquals("TEST", dto.getRoleName());
        verify(roleRepository).findById(200L);
        verify(roleRepository).flush();
    }

    @Test
    @DisplayName("TC_RS_04_02: 수정 대상 Role이 없을 때 → ROLE_NOT_FOUND 예외")
    void updateRole_notFound() {
        // given
        RoleRequestDto updateDto = RoleRequestDto.builder()
                .roleName(null)
                .description("설명")
                .build();

        when(roleRepository.findById(300L)).thenReturn(Optional.empty());

        // when & then
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> roleService.updateRole(300L, updateDto)
        );
        System.out.println("⚠️ TC_RS_04_02 - 예외 코드: " + ex.getErrorCode());
        assertEquals(ErrorCode.ROLE_NOT_FOUND, ex.getErrorCode());
        verify(roleRepository).findById(300L);
    }

    // ========================================
    // TC_RS_05: deleteRole(Long roleId)
    // ========================================

    @Test
    @DisplayName("TC_RS_05_01: Role 삭제 → 성공")
    void deleteRole_success() {
        // given
        when(roleRepository.existsById(15L)).thenReturn(true);
        doNothing().when(roleRepository).deleteById(15L);
        doNothing().when(roleRepository).flush();

        // when
        roleService.deleteRole(15L);

        // then
        System.out.println("✅ TC_RS_05_01 - Role 삭제 성공 (ID: 15)");
        verify(roleRepository).existsById(15L);
        verify(roleRepository).deleteById(15L);
        verify(roleRepository).flush();
    }

    @Test
    @DisplayName("TC_RS_05_02: 삭제 대상 Role이 없을 때 → ROLE_NOT_FOUND 예외")
    void deleteRole_notFound() {
        // given
        when(roleRepository.existsById(999L)).thenReturn(false);

        // when & then
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> roleService.deleteRole(999L)
        );
        System.out.println("⚠️ TC_RS_05_02 - 예외 코드: " + ex.getErrorCode());
        assertEquals(ErrorCode.ROLE_NOT_FOUND, ex.getErrorCode());
        verify(roleRepository).existsById(999L);
    }
}
