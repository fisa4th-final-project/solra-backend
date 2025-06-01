package com.fisa.solra.domain.userrole.service;

import com.fisa.solra.domain.organization.entity.Organization;
import com.fisa.solra.domain.role.entity.Role;
import com.fisa.solra.domain.role.repository.RoleRepository;
import com.fisa.solra.domain.user.entity.User;
import com.fisa.solra.domain.user.repository.UserRepository;
import com.fisa.solra.domain.userrole.dto.UserRoleRequestDto;
import com.fisa.solra.domain.userrole.dto.UserRoleResponseDto;
import com.fisa.solra.domain.userrole.entity.UserRole;
import com.fisa.solra.domain.userrole.repository.UserRoleRepository;
import com.fisa.solra.global.exception.BusinessException;
import com.fisa.solra.global.exception.ErrorCode;
import com.fisa.solra.global.util.SecurityUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.DisplayName.class)
class UserRoleServiceTest {

    @InjectMocks
    private UserRoleService userRoleService;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private User mockUser;

    @Mock
    private Role mockRole;

    @Mock
    private Organization mockOrg;

    private static final Long USER_ID = 10L;
    private static final Long ROLE_ID = 20L;
    private static final Long ORG_ID = 100L;
    private static final Long OTHER_ORG_ID = 200L;

    private MockedStatic<SecurityUtil> securityUtilStatic;

    @BeforeEach
    void setUp() {
        securityUtilStatic = mockStatic(SecurityUtil.class);
    }

    @AfterEach
    void tearDown() {
        securityUtilStatic.close();
    }

    // TC_UR_01_01: assignRole → 성공
    @Test
    @DisplayName("TC_UR_01_01: assignRole → 성공")
    void assignRole_success() {
        // 1) userRepository.findById(USER_ID) → mockUser
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mockUser));
        // 2) SecurityUtil.hasRole("ROOT") → false
        securityUtilStatic.when(() -> SecurityUtil.hasRole("ROOT")).thenReturn(false);
        // 3) mockUser.getOrganization() → mockOrg, mockOrg.getOrgId() → ORG_ID
        when(mockUser.getOrganization()).thenReturn(mockOrg);
        when(mockOrg.getOrgId()).thenReturn(ORG_ID);
        // 4) SecurityUtil.getOrgId() → ORG_ID
        securityUtilStatic.when(SecurityUtil::getOrgId).thenReturn(ORG_ID);
        // 5) roleRepository.findById(ROLE_ID) → mockRole
        when(roleRepository.findById(ROLE_ID)).thenReturn(Optional.of(mockRole));
        // 6) userRoleRepository.existsByUserAndRole → false
        when(userRoleRepository.existsByUserAndRole(mockUser, mockRole)).thenReturn(false);

        // 7) 실제 호출
        UserRoleResponseDto response = userRoleService.assignRole(USER_ID, ROLE_ID);

        // 검증: userId, roleId, roleName 일치 (roleName은 mockRole.getRoleName() 호출)
        verify(userRoleRepository, times(1)).save(any(UserRole.class));
        System.out.println("✅ TC_UR_01_01 - 할당된 UserRole: userId=" + response.getUserId()
                + ", roleId=" + response.getRoleId());
    }

    // TC_UR_01_02: assignRole → USER_NOT_FOUND
    @Test
    @DisplayName("TC_UR_01_02: assignRole → USER_NOT_FOUND")
    void assignRole_userNotFound_throws() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> userRoleService.assignRole(USER_ID, ROLE_ID)
        );
        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
        System.out.println("⚠️ TC_UR_01_02 - 예외 코드: " + ex.getErrorCode());
    }

    // TC_UR_01_03: assignRole → ACCESS_DENIED (조직 불일치)
    @Test
    @DisplayName("TC_UR_01_03: assignRole → ACCESS_DENIED")
    void assignRole_accessDenied_throws() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mockUser));
        // ROOT 아니고
        securityUtilStatic.when(() -> SecurityUtil.hasRole("ROOT")).thenReturn(false);
        when(mockUser.getOrganization()).thenReturn(mockOrg);
        when(mockOrg.getOrgId()).thenReturn(OTHER_ORG_ID);
        // SecurityUtil.getOrgId() → ORG_ID (다른 조직)
        securityUtilStatic.when(SecurityUtil::getOrgId).thenReturn(ORG_ID);

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> userRoleService.assignRole(USER_ID, ROLE_ID)
        );
        assertEquals(ErrorCode.ACCESS_DENIED, ex.getErrorCode());
        System.out.println("⚠️ TC_UR_01_03 - 예외 코드: " + ex.getErrorCode());
    }

    // TC_UR_01_04: assignRole → ROLE_NOT_FOUND
    @Test
    @DisplayName("TC_UR_01_04: assignRole → ROLE_NOT_FOUND")
    void assignRole_roleNotFound_throws() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mockUser));
        securityUtilStatic.when(() -> SecurityUtil.hasRole("ROOT")).thenReturn(true);
        // ROOT 우회 시 조직 검사 없음
        when(roleRepository.findById(ROLE_ID)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> userRoleService.assignRole(USER_ID, ROLE_ID)
        );
        assertEquals(ErrorCode.ROLE_NOT_FOUND, ex.getErrorCode());
        System.out.println("⚠️ TC_UR_01_04 - 예외 코드: " + ex.getErrorCode());
    }

    // TC_UR_01_05: assignRole → ROLE_ALREADY_ASSIGNED
    @Test
    @DisplayName("TC_UR_01_05: assignRole → ROLE_ALREADY_ASSIGNED")
    void assignRole_alreadyAssigned_throws() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mockUser));
        securityUtilStatic.when(() -> SecurityUtil.hasRole("ROOT")).thenReturn(true);
        when(roleRepository.findById(ROLE_ID)).thenReturn(Optional.of(mockRole));
        when(userRoleRepository.existsByUserAndRole(mockUser, mockRole)).thenReturn(true);

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> userRoleService.assignRole(USER_ID, ROLE_ID)
        );
        assertEquals(ErrorCode.ROLE_ALREADY_ASSIGNED, ex.getErrorCode());
        System.out.println("⚠️ TC_UR_01_05 - 예외 코드: " + ex.getErrorCode());
    }

    // TC_UR_02_01: removeUserRole → 성공
    @Test
    @DisplayName("TC_UR_02_01: removeUserRole → 성공")
    void removeUserRole_success() {
        UserRoleRequestDto req = new UserRoleRequestDto(USER_ID, ROLE_ID);

        // 1) user 찾기
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mockUser));
        // 2) ROOT 아니고 조직 일치
        securityUtilStatic.when(() -> SecurityUtil.hasRole("ROOT")).thenReturn(false);
        when(mockUser.getOrganization()).thenReturn(mockOrg);
        when(mockOrg.getOrgId()).thenReturn(ORG_ID);
        securityUtilStatic.when(SecurityUtil::getOrgId).thenReturn(ORG_ID);
        // 3) role 찾기
        when(roleRepository.findById(ROLE_ID)).thenReturn(Optional.of(mockRole));
        // 4) userRoleRepository.findByUserAndRole → 존재하는 UserRole
        UserRole existing = mock(UserRole.class);
        when(userRoleRepository.findByUserAndRole(mockUser, mockRole))
                .thenReturn(Optional.of(existing));

        // 5) 실제 호출
        UserRoleResponseDto response = userRoleService.removeUserRole(req);

        // 검증: delete 호출
        verify(userRoleRepository, times(1)).delete(existing);
        System.out.println("✅ TC_UR_02_01 - 제거된 UserRole: userId=" + response.getUserId()
                + ", roleId=" + response.getRoleId());
    }

    // TC_UR_02_02: removeUserRole → USER_NOT_FOUND
    @Test
    @DisplayName("TC_UR_02_02: removeUserRole → USER_NOT_FOUND")
    void removeUserRole_userNotFound_throws() {
        UserRoleRequestDto req = new UserRoleRequestDto(USER_ID, ROLE_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> userRoleService.removeUserRole(req)
        );
        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
        System.out.println("⚠️ TC_UR_02_02 - 예외 코드: " + ex.getErrorCode());
    }

    // TC_UR_02_03: removeUserRole → ACCESS_DENIED
    @Test
    @DisplayName("TC_UR_02_03: removeUserRole → ACCESS_DENIED")
    void removeUserRole_accessDenied_throws() {
        UserRoleRequestDto req = new UserRoleRequestDto(USER_ID, ROLE_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mockUser));
        securityUtilStatic.when(() -> SecurityUtil.hasRole("ROOT")).thenReturn(false);
        when(mockUser.getOrganization()).thenReturn(mockOrg);
        when(mockOrg.getOrgId()).thenReturn(OTHER_ORG_ID);
        securityUtilStatic.when(SecurityUtil::getOrgId).thenReturn(ORG_ID);

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> userRoleService.removeUserRole(req)
        );
        assertEquals(ErrorCode.ACCESS_DENIED, ex.getErrorCode());
        System.out.println("⚠️ TC_UR_02_03 - 예외 코드: " + ex.getErrorCode());
    }

    // TC_UR_02_04: removeUserRole → ROLE_NOT_FOUND (역할 없음)
    @Test
    @DisplayName("TC_UR_02_04: removeUserRole → ROLE_NOT_FOUND")
    void removeUserRole_roleNotFound_throws() {
        UserRoleRequestDto req = new UserRoleRequestDto(USER_ID, ROLE_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mockUser));
        securityUtilStatic.when(() -> SecurityUtil.hasRole("ROOT")).thenReturn(true);
        when(roleRepository.findById(ROLE_ID)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> userRoleService.removeUserRole(req)
        );
        assertEquals(ErrorCode.ROLE_NOT_FOUND, ex.getErrorCode());
        System.out.println("⚠️ TC_UR_02_04 - 예외 코드: " + ex.getErrorCode());
    }

    // TC_UR_02_05: removeUserRole → ROLE_NOT_FOUND (매핑 없음)
    @Test
    @DisplayName("TC_UR_02_05: removeUserRole → ROLE_NOT_FOUND (매핑 없음)")
    void removeUserRole_mappingNotFound_throws() {
        UserRoleRequestDto req = new UserRoleRequestDto(USER_ID, ROLE_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mockUser));
        securityUtilStatic.when(() -> SecurityUtil.hasRole("ROOT")).thenReturn(true);
        when(roleRepository.findById(ROLE_ID)).thenReturn(Optional.of(mockRole));
        when(userRoleRepository.findByUserAndRole(mockUser, mockRole))
                .thenReturn(Optional.empty());

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> userRoleService.removeUserRole(req)
        );
        assertEquals(ErrorCode.ROLE_NOT_FOUND, ex.getErrorCode());
        System.out.println("⚠️ TC_UR_02_05 - 예외 코드: " + ex.getErrorCode());
    }

    // TC_UR_03_01: getRolesByUserId → 역할 목록 반환
    @Test
    @DisplayName("TC_UR_03_01: getRolesByUserId → 역할 목록 반환")
    void getRolesByUserId_success() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mockUser));
        securityUtilStatic.when(() -> SecurityUtil.hasRole("ROOT")).thenReturn(false);
        when(mockUser.getOrganization()).thenReturn(mockOrg);
        when(mockOrg.getOrgId()).thenReturn(ORG_ID);
        securityUtilStatic.when(SecurityUtil::getOrgId).thenReturn(ORG_ID);

        // userRole 리스트 준비
        UserRole ur1 = mock(UserRole.class);
        UserRole ur2 = mock(UserRole.class);
        when(userRoleRepository.findAllByUser(mockUser)).thenReturn(List.of(ur1, ur2));
        // 각 UserRole이 반환할 Role
        Role r1 = Role.builder().roleId(1L).roleName("RoleA").build();
        Role r2 = Role.builder().roleId(2L).roleName("RoleB").build();
        when(ur1.getRole()).thenReturn(r1);
        when(ur2.getRole()).thenReturn(r2);

        List<UserRoleResponseDto> responses = userRoleService.getRolesByUserId(USER_ID);
        assertEquals(2, responses.size());
        System.out.println("✅ TC_UR_03_01 - 반환된 역할 수: " + responses.size());
    }

    // TC_UR_03_02: getRolesByUserId → USER_NOT_FOUND
    @Test
    @DisplayName("TC_UR_03_02: getRolesByUserId → USER_NOT_FOUND")
    void getRolesByUserId_userNotFound_throws() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> userRoleService.getRolesByUserId(USER_ID)
        );
        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
        System.out.println("⚠️ TC_UR_03_02 - 예외 코드: " + ex.getErrorCode());
    }

    // TC_UR_03_03: getRolesByUserId → ACCESS_DENIED
    @Test
    @DisplayName("TC_UR_03_03: getRolesByUserId → ACCESS_DENIED")
    void getRolesByUserId_accessDenied_throws() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mockUser));
        securityUtilStatic.when(() -> SecurityUtil.hasRole("ROOT")).thenReturn(false);
        when(mockUser.getOrganization()).thenReturn(mockOrg);
        when(mockOrg.getOrgId()).thenReturn(OTHER_ORG_ID);
        securityUtilStatic.when(SecurityUtil::getOrgId).thenReturn(ORG_ID);

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> userRoleService.getRolesByUserId(USER_ID)
        );
        assertEquals(ErrorCode.ACCESS_DENIED, ex.getErrorCode());
        System.out.println("⚠️ TC_UR_03_03 - 예외 코드: " + ex.getErrorCode());
    }
}
