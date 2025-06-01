package com.fisa.solra.domain.rolepermission.service;

import com.fisa.solra.domain.permission.dto.PermissionResponseDto;
import com.fisa.solra.domain.permission.entity.Permission;
import com.fisa.solra.domain.permission.repository.PermissionRepository;
import com.fisa.solra.domain.role.entity.Role;
import com.fisa.solra.domain.role.repository.RoleRepository;
import com.fisa.solra.domain.rolepermission.dto.RolePermissionRequestDto;
import com.fisa.solra.domain.rolepermission.dto.RolePermissionResponseDto;
import com.fisa.solra.domain.rolepermission.entity.RolePermission;
import com.fisa.solra.domain.rolepermission.repository.RolePermissionRepository;
import com.fisa.solra.global.exception.BusinessException;
import com.fisa.solra.global.exception.ErrorCode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.DisplayName.class)
class RolePermissionServiceTest {

    @InjectMocks
    private RolePermissionService rolePermissionService;

    @Mock
    private RolePermissionRepository rolePermissionRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    // TC_RP_01_01: assignPermission → 정상 할당
    @Test
    @DisplayName("TC_RP_01_01: assignPermission → 정상 할당")
    void assignPermission_success() {
        Long roleId = 10L;
        List<Long> permIds = List.of(100L, 200L);
        RolePermissionRequestDto req = RolePermissionRequestDto.builder()
                .roleId(roleId)
                .permissionIds(permIds)
                .build();

        // 1) Role 존재
        Role role = Role.builder().roleId(roleId).roleName("ROLE_A").build();
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

        // 2) Permission 존재
        Permission p1 = Permission.builder()
                .permissionId(100L).permissionName("P100").description("desc100").build();
        Permission p2 = Permission.builder()
                .permissionId(200L).permissionName("P200").description("desc200").build();
        when(permissionRepository.findById(100L)).thenReturn(Optional.of(p1));
        when(permissionRepository.findById(200L)).thenReturn(Optional.of(p2));

        // 3) 중복이 아님
        when(rolePermissionRepository.existsByRoleRoleIdAndPermissionPermissionId(roleId, 100L))
                .thenReturn(false);
        when(rolePermissionRepository.existsByRoleRoleIdAndPermissionPermissionId(roleId, 200L))
                .thenReturn(false);

        // 4) 저장 결과 stub (연속 호출에 대해 순차적으로 반환)
        RolePermission rp1 = RolePermission.builder()
                .rolePermissionId(1000L)
                .role(role)
                .permission(p1)
                .build();
        RolePermission rp2 = RolePermission.builder()
                .rolePermissionId(1001L)
                .role(role)
                .permission(p2)
                .build();
        when(rolePermissionRepository.save(any(RolePermission.class)))
                .thenReturn(rp1)
                .thenReturn(rp2);

        // 5) 실제 호출
        List<RolePermissionResponseDto> result = rolePermissionService.assignPermission(req);

        // 6) 검증
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(r -> r.getPermissionId().equals(100L)));
        assertTrue(result.stream().anyMatch(r -> r.getPermissionId().equals(200L)));

        // 결과 출력
        System.out.println("✅ TC_RP_01_01 - 생성된 RolePermission 수: " + result.size());
    }

    // TC_RP_01_02: assignPermission → Role 없음 예외
    @Test
    @DisplayName("TC_RP_01_02: assignPermission → Role 없음 예외")
    void assignPermission_roleNotFound_throws() {
        Long roleId = 20L;
        List<Long> permIds = List.of(300L);
        RolePermissionRequestDto req = RolePermissionRequestDto.builder()
                .roleId(roleId)
                .permissionIds(permIds)
                .build();

        when(roleRepository.findById(roleId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> rolePermissionService.assignPermission(req)
        );
        assertEquals(ErrorCode.ROLE_NOT_FOUND, ex.getErrorCode());

        // 결과 출력
        System.out.println("⚠️ TC_RP_01_02 - 예외 코드: " + ex.getErrorCode());
    }

    // TC_RP_01_03: assignPermission → Permission 없음 예외
    @Test
    @DisplayName("TC_RP_01_03: assignPermission → Permission 없음 예외")
    void assignPermission_permissionNotFound_throws() {
        Long roleId = 30L;
        List<Long> permIds = List.of(400L, 500L);
        RolePermissionRequestDto req = RolePermissionRequestDto.builder()
                .roleId(roleId)
                .permissionIds(permIds)
                .build();

        Role role = Role.builder().roleId(roleId).roleName("ROLE_B").build();
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

        when(permissionRepository.findById(400L))
                .thenReturn(Optional.empty());

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> rolePermissionService.assignPermission(req)
        );
        assertEquals(ErrorCode.PERMISSION_NOT_FOUND, ex.getErrorCode());

        // 결과 출력
        System.out.println("⚠️ TC_RP_01_03 - 예외 코드: " + ex.getErrorCode());
    }

    // TC_RP_01_04: assignPermission → 중복 권한 스킵
    @Test
    @DisplayName("TC_RP_01_04: assignPermission → 중복 권한 스킵")
    void assignPermission_skipDuplicates() {
        Long roleId = 40L;
        List<Long> permIds = List.of(600L);
        RolePermissionRequestDto req = RolePermissionRequestDto.builder()
                .roleId(roleId)
                .permissionIds(permIds)
                .build();

        Role role = Role.builder().roleId(roleId).roleName("ROLE_C").build();
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

        Permission p = Permission.builder()
                .permissionId(600L).permissionName("P600").description("desc600").build();
        when(permissionRepository.findById(600L)).thenReturn(Optional.of(p));

        // 이미 존재한다고 반환
        when(rolePermissionRepository.existsByRoleRoleIdAndPermissionPermissionId(roleId, 600L))
                .thenReturn(true);

        // 실제 호출
        List<RolePermissionResponseDto> result = rolePermissionService.assignPermission(req);

        // 중복이므로 빈 리스트
        assertTrue(result.isEmpty());

        // 결과 출력
        System.out.println("✅ TC_RP_01_04 - 생성된 RolePermission 수: " + result.size());
    }

    // TC_RP_02_01: getPermissionsByRole → 정상 조회
    @Test
    @DisplayName("TC_RP_02_01: getPermissionsByRole → 정상 조회")
    void getPermissionsByRole_success() {
        Long roleId = 50L;
        Role role = Role.builder().roleId(roleId).roleName("ROLE_D").build();
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

        // 매핑된 엔티티들 준비
        Permission p1 = Permission.builder()
                .permissionId(700L).permissionName("P700").description("d700").build();
        Permission p2 = Permission.builder()
                .permissionId(800L).permissionName("P800").description("d800").build();
        RolePermission rp1 = RolePermission.builder()
                .rolePermissionId(2000L)
                .role(role)
                .permission(p1)
                .build();
        RolePermission rp2 = RolePermission.builder()
                .rolePermissionId(2001L)
                .role(role)
                .permission(p2)
                .build();
        when(rolePermissionRepository.findByRoleRoleId(roleId))
                .thenReturn(List.of(rp1, rp2));

        // 실제 호출
        List<PermissionResponseDto> result = rolePermissionService.getPermissionsByRole(roleId);

        // 검증
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(r -> r.getPermissionId().equals(700L)));
        assertTrue(result.stream().anyMatch(r -> r.getPermissionId().equals(800L)));

        // 결과 출력
        System.out.println("✅ TC_RP_02_01 - 조회된 권한 수: " + result.size());
    }

    // TC_RP_02_02: getPermissionsByRole → Role 없음 예외
    @Test
    @DisplayName("TC_RP_02_02: getPermissionsByRole → Role 없음 예외")
    void getPermissionsByRole_roleNotFound_throws() {
        Long roleId = 60L;
        when(roleRepository.findById(roleId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> rolePermissionService.getPermissionsByRole(roleId)
        );
        assertEquals(ErrorCode.ROLE_NOT_FOUND, ex.getErrorCode());

        // 결과 출력
        System.out.println("⚠️ TC_RP_02_02 - 예외 코드: " + ex.getErrorCode());
    }

    // TC_RP_03_01: removePermissions → 정상 삭제
    @Test
    @DisplayName("TC_RP_03_01: removePermissions → 정상 삭제")
    void removePermissions_success() {
        Long roleId = 70L;
        List<Long> permIds = List.of(900L, 1000L);
        RolePermissionRequestDto req = RolePermissionRequestDto.builder()
                .roleId(roleId)
                .permissionIds(permIds)
                .build();

        // 하나는 존재, 하나는 존재하지 않음 (삭제 후에도 에러 없음)
        when(rolePermissionRepository.existsByRoleRoleIdAndPermissionPermissionId(roleId, 900L))
                .thenReturn(true);
        when(rolePermissionRepository.existsByRoleRoleIdAndPermissionPermissionId(roleId, 1000L))
                .thenReturn(false);

        // 삭제 동작은 void → verify로 검증
        doNothing().when(rolePermissionRepository)
                .deleteByRoleRoleIdAndPermissionPermissionId(roleId, 900L);

        // 실제 호출
        assertDoesNotThrow(() -> rolePermissionService.removePermissions(req));

        // deleteById 및 flush 호출 검증
        verify(rolePermissionRepository, times(1))
                .deleteByRoleRoleIdAndPermissionPermissionId(roleId, 900L);
        verify(rolePermissionRepository, times(1)).flush();

        // 결과 출력
        System.out.println("✅ TC_RP_03_01 - removePermissions 호출됨");
    }

    // TC_RP_03_02: removePermissions → permIds 비어있음 예외
    @Test
    @DisplayName("TC_RP_03_02: removePermissions → permIds 비어있음 예외")
    void removePermissions_emptyList_throws() {
        RolePermissionRequestDto req = RolePermissionRequestDto.builder()
                .roleId(80L)
                .permissionIds(Collections.emptyList())
                .build();

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> rolePermissionService.removePermissions(req)
        );
        assertEquals(ErrorCode.PERMISSION_NOT_FOUND, ex.getErrorCode());

        // 결과 출력
        System.out.println("⚠️ TC_RP_03_02 - 예외 코드: " + ex.getErrorCode());
    }

    // TC_RP_03_03: removePermissions → 매핑 없는 권한 넘어왔을 때 정상 처리
    @Test
    @DisplayName("TC_RP_03_03: removePermissions → 매핑 없는 권한 넘어왔을 때 정상 처리")
    void removePermissions_noMapping_skip() {
        Long roleId = 90L;
        List<Long> permIds = List.of(1100L);
        RolePermissionRequestDto req = RolePermissionRequestDto.builder()
                .roleId(roleId)
                .permissionIds(permIds)
                .build();

        // 존재하지 않음 → skip
        when(rolePermissionRepository.existsByRoleRoleIdAndPermissionPermissionId(roleId, 1100L))
                .thenReturn(false);

        // 실제 호출
        assertDoesNotThrow(() -> rolePermissionService.removePermissions(req));

        // flush 호출 검증
        verify(rolePermissionRepository, times(1)).flush();

        // 결과 출력
        System.out.println("✅ TC_RP_03_03 - 매핑 없는 권한은 스킵됨");
    }
}
