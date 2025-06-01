package com.fisa.solra.domain.userpermission.service;

import com.fisa.solra.domain.permission.dto.PermissionResponseDto;
import com.fisa.solra.domain.permission.entity.Permission;
import com.fisa.solra.domain.permission.repository.PermissionRepository;
import com.fisa.solra.domain.user.entity.User;
import com.fisa.solra.domain.user.repository.UserRepository;
import com.fisa.solra.domain.userpermission.dto.UserPermissionRequestDto;
import com.fisa.solra.domain.userpermission.dto.UserPermissionResponseDto;
import com.fisa.solra.domain.userpermission.entity.UserPermission;
import com.fisa.solra.domain.userpermission.repository.UserPermissionRepository;
import com.fisa.solra.global.exception.BusinessException;
import com.fisa.solra.global.exception.ErrorCode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.DisplayName.class)
class UserPermissionServiceTest {

    @InjectMocks
    private UserPermissionService userPermissionService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private UserPermissionRepository userPermissionRepository;

    // TC_UP_01_01: assignPermission → 성공
    @Test
    @DisplayName("TC_UP_01_01: assignPermission → 성공")
    void assignPermission_success() {
        // 1) userRepository.findById 및 permissionRepository.findById stub
        User user = mock(User.class);
        Permission permission = mock(Permission.class);
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(permissionRepository.findById(10L)).thenReturn(Optional.of(permission));

        // 2) user.getUserId(), permission.getPermissionId() stub
        when(user.getUserId()).thenReturn(5L);
        when(permission.getPermissionId()).thenReturn(10L);

        // 3) 이미 권한 없음
        when(userPermissionRepository.existsByUserAndPermission(user, permission))
                .thenReturn(false);

        // 4) 실제 호출
        UserPermissionRequestDto req = new UserPermissionRequestDto(5L, 10L);
        UserPermissionResponseDto res = userPermissionService.assignPermission(req);

        // 5) 검증: 응답 DTO에 userId=5, permissionId=10
        assertEquals(5L, res.getUserId());
        assertEquals(10L, res.getPermissionId());

        // 결과 출력
        System.out.println("✅ TC_UP_01_01 - 생성된 UserPermission: userId=" + res.getUserId()
                + ", permissionId=" + res.getPermissionId());
    }

    // TC_UP_01_02: assignPermission → 존재하지 않는 userId 예외 (USER_NOT_FOUND)
    @Test
    @DisplayName("TC_UP_01_02: assignPermission → 존재하지 않는 userId 예외 (USER_NOT_FOUND)")
    void assignPermission_userNotFound_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> userPermissionService.assignPermission(new UserPermissionRequestDto(99L, 1L))
        );
        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());

        // 결과 출력
        System.out.println("⚠️ TC_UP_01_02 - 예외 코드: " + ex.getErrorCode());
    }

    // TC_UP_01_03: assignPermission → 존재하지 않는 permissionId 예외 (PERMISSION_NOT_FOUND)
    @Test
    @DisplayName("TC_UP_01_03: assignPermission → 존재하지 않는 permissionId 예외 (PERMISSION_NOT_FOUND)")
    void assignPermission_permissionNotFound_throws() {
        User user = mock(User.class);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(permissionRepository.findById(42L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> userPermissionService.assignPermission(new UserPermissionRequestDto(7L, 42L))
        );
        assertEquals(ErrorCode.PERMISSION_NOT_FOUND, ex.getErrorCode());

        // 결과 출력
        System.out.println("⚠️ TC_UP_01_03 - 예외 코드: " + ex.getErrorCode());
    }

    // TC_UP_01_04: assignPermission → 이미 권한 보유 시 예외 (USER_ALREADY_HAS_PERMISSION)
    @Test
    @DisplayName("TC_UP_01_04: assignPermission → 이미 권한 보유 시 예외 (USER_ALREADY_HAS_PERMISSION)")
    void assignPermission_alreadyHasPermission_throws() {
        User user = mock(User.class);
        Permission permission = mock(Permission.class);
        when(userRepository.findById(3L)).thenReturn(Optional.of(user));
        when(permissionRepository.findById(8L)).thenReturn(Optional.of(permission));
        when(userPermissionRepository.existsByUserAndPermission(user, permission))
                .thenReturn(true);

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> userPermissionService.assignPermission(new UserPermissionRequestDto(3L, 8L))
        );
        assertEquals(ErrorCode.USER_ALREADY_HAS_PERMISSION, ex.getErrorCode());

        // 결과 출력
        System.out.println("⚠️ TC_UP_01_04 - 예외 코드: " + ex.getErrorCode());
    }

    // TC_UP_02_01: getUserPermissions → 성공
    @Test
    @DisplayName("TC_UP_02_01: getUserPermissions → 성공")
    void getUserPermissions_success() {
        User user = mock(User.class);
        Permission p1 = mock(Permission.class);
        when(p1.getPermissionId()).thenReturn(100L);
        when(p1.getPermissionName()).thenReturn("PERM_X");
        when(p1.getDescription()).thenReturn("descX");
        Permission p2 = mock(Permission.class);
        when(p2.getPermissionId()).thenReturn(200L);
        when(p2.getPermissionName()).thenReturn("PERM_Y");
        when(p2.getDescription()).thenReturn("descY");

        UserPermission up1 = mock(UserPermission.class);
        when(up1.getPermission()).thenReturn(p1);
        UserPermission up2 = mock(UserPermission.class);
        when(up2.getPermission()).thenReturn(p2);

        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(userPermissionRepository.findAllByUser(user))
                .thenReturn(List.of(up1, up2));

        // 실제 호출
        List<PermissionResponseDto> list = userPermissionService.getUserPermissions(2L);

        // 검증: size 2, 각 DTO 권한 ID, 이름, 설명 확인
        assertEquals(2, list.size());
        assertEquals(100L, list.get(0).getPermissionId());
        assertEquals("PERM_X", list.get(0).getPermissionName());
        assertEquals("descX", list.get(0).getDescription());

        assertEquals(200L, list.get(1).getPermissionId());
        assertEquals("PERM_Y", list.get(1).getPermissionName());
        assertEquals("descY", list.get(1).getDescription());

        // 결과 출력
        System.out.println("✅ TC_UP_02_01 - 조회된 권한 수: " + list.size());
    }

    // TC_UP_02_02: getUserPermissions → 존재하지 않는 userId 예외 (USER_NOT_FOUND)
    @Test
    @DisplayName("TC_UP_02_02: getUserPermissions → 존재하지 않는 userId 예외 (USER_NOT_FOUND)")
    void getUserPermissions_userNotFound_throws() {
        when(userRepository.findById(55L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> userPermissionService.getUserPermissions(55L)
        );
        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());

        // 결과 출력
        System.out.println("⚠️ TC_UP_02_02 - 예외 코드: " + ex.getErrorCode());
    }

    // TC_UP_03_01: removePermission → 성공
    @Test
    @DisplayName("TC_UP_03_01: removePermission → 성공")
    void removePermission_success() {
        User user = mock(User.class);
        Permission permission = mock(Permission.class);
        UserPermission up = mock(UserPermission.class);

        when(userRepository.findById(9L)).thenReturn(Optional.of(user));
        when(permissionRepository.findById(15L)).thenReturn(Optional.of(permission));
        when(userPermissionRepository.findByUserAndPermission(user, permission))
                .thenReturn(Optional.of(up));

        // 실제 호출
        UserPermissionResponseDto res = userPermissionService.removePermission(
                new UserPermissionRequestDto(9L, 15L)
        );

        // 검증: 응답 DTO userId=9, permissionId=15
        assertEquals(9L, res.getUserId());
        assertEquals(15L, res.getPermissionId());

        // 결과 출력
        System.out.println("✅ TC_UP_03_01 - 제거된 UserPermission: userId=" + res.getUserId()
                + ", permissionId=" + res.getPermissionId());
    }

    // TC_UP_03_02: removePermission → 존재하지 않는 userId 예외 (USER_NOT_FOUND)
    @Test
    @DisplayName("TC_UP_03_02: removePermission → 존재하지 않는 userId 예외 (USER_NOT_FOUND)")
    void removePermission_userNotFound_throws() {
        when(userRepository.findById(88L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> userPermissionService.removePermission(new UserPermissionRequestDto(88L, 1L))
        );
        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());

        // 결과 출력
        System.out.println("⚠️ TC_UP_03_02 - 예외 코드: " + ex.getErrorCode());
    }

    // TC_UP_03_03: removePermission → 존재하지 않는 permissionId 예외 (PERMISSION_NOT_FOUND)
    @Test
    @DisplayName("TC_UP_03_03: removePermission → 존재하지 않는 permissionId 예외 (PERMISSION_NOT_FOUND)")
    void removePermission_permissionNotFound_throws() {
        User user = mock(User.class);
        when(userRepository.findById(20L)).thenReturn(Optional.of(user));
        when(permissionRepository.findById(77L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> userPermissionService.removePermission(new UserPermissionRequestDto(20L, 77L))
        );
        assertEquals(ErrorCode.PERMISSION_NOT_FOUND, ex.getErrorCode());

        // 결과 출력
        System.out.println("⚠️ TC_UP_03_03 - 예외 코드: " + ex.getErrorCode());
    }

    // TC_UP_03_04: removePermission → 사용자에게 없는 권한 예외 (USER_PERMISSION_NOT_FOUND)
    @Test
    @DisplayName("TC_UP_03_04: removePermission → 사용자에게 없는 권한 예외 (USER_PERMISSION_NOT_FOUND)")
    void removePermission_notAssigned_throws() {
        User user = mock(User.class);
        Permission permission = mock(Permission.class);
        when(userRepository.findById(33L)).thenReturn(Optional.of(user));
        when(permissionRepository.findById(44L)).thenReturn(Optional.of(permission));
        when(userPermissionRepository.findByUserAndPermission(user, permission))
                .thenReturn(Optional.empty());

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> userPermissionService.removePermission(new UserPermissionRequestDto(33L, 44L))
        );
        assertEquals(ErrorCode.USER_PERMISSION_NOT_FOUND, ex.getErrorCode());

        // 결과 출력
        System.out.println("⚠️ TC_UP_03_04 - 예외 코드: " + ex.getErrorCode());
    }
}
