// src/test/java/com/fisa/solra/domain/user/service/UserServiceTest.java
package com.fisa.solra.domain.user.service;

import com.fisa.solra.domain.department.entity.Department;
import com.fisa.solra.domain.department.repository.DepartmentRepository;
import com.fisa.solra.domain.organization.entity.Organization;
import com.fisa.solra.domain.organization.repository.OrganizationRepository;
import com.fisa.solra.domain.permission.service.PermissionService;
import com.fisa.solra.domain.role.entity.Role;
import com.fisa.solra.domain.user.dto.*;
import com.fisa.solra.domain.user.entity.User;
import com.fisa.solra.domain.user.repository.UserRepository;
import com.fisa.solra.global.exception.BusinessException;
import com.fisa.solra.global.exception.ErrorCode;
import com.fisa.solra.global.util.SecurityUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.DisplayName.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock private UserRepository userRepository;
    @Mock private OrganizationRepository organizationRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private PermissionService permissionService;

    private MockedStatic<SecurityUtil> securityUtil;

    @BeforeEach
    void init() {
        securityUtil = mockStatic(SecurityUtil.class);
    }

    @AfterEach
    void tearDown() {
        securityUtil.close();
    }

    @Test
    @DisplayName("TC_US_01_01: 로그인 성공")
    void login_success() {
        Role userRole = Role.builder(). roleName("USER"). build();

        String rawPassword    = "password";
        String encodedPassword = "encodedPass";
        when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(true);

        Set<Role> roles = new HashSet<>();
        roles.add(userRole);

        User user = User.builder()
                .userLoginId("alice")
                .password(encodedPassword)
                .roles(roles)     // Role 엔티티 집합을 builder로 바로 넘겨준다
                .build();

        when(userRepository.findByUserLoginId("alice"))
                .thenReturn(Optional.of(user));

        UserLoginInfo info = userService.login("alice", rawPassword);

        System.out.println("✅ TC_US_01_01 - 로그인 성공, userId: " + info.getUserId());
        assertEquals(user.getUserId(), info.getUserId());
        assertNull(info.getOrgId(), "조직이 없으면 null이어야 합니다");
        assertNull(info.getDeptId(), "부서가 없으면 null이어야 합니다");

        // 만약 UserLoginInfo.roles 타입이 Set<Role>라면, 다음처럼 검증
        assertTrue(
                info.getRoles().stream().anyMatch(r -> r.getRoleName().equals("USER")),
                "Role 객체 안에 roleName=USER가 포함되어야 합니다"
        );
    }

    @Test @DisplayName("TC_US_01_02: 로그인 시 사용자 없음 → USER_NOT_FOUND")
    void login_userNotFound() {
        when(userRepository.findByUserLoginId("bob"))
                .thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.login("bob", "pw"));

        System.out.println("⚠️ TC_US_01_02 - 예외 코드: " + ex.getErrorCode());
        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
    }

    @Test @DisplayName("TC_US_01_03: 로그인 시 비밀번호 불일치 → INVALID_CREDENTIALS")
    void login_badPassword() {
        User user = User.builder().userLoginId("alice").password("encoded").build();
        when(userRepository.findByUserLoginId("alice"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.login("alice", "wrong"));

        System.out.println("⚠️ TC_US_01_03 - 예외 코드: " + ex.getErrorCode());
        assertEquals(ErrorCode.INVALID_CREDENTIALS, ex.getErrorCode());
    }

    // TC_US_02: 사용자 생성(createUser)
    @Test @DisplayName("TC_US_02_01: 사용자 생성 성공")
    void createUser_success() {
        var dto = UserCreateRequestDto.builder()
                .userLoginId("newUser").password("pw")
                .userName("New").email("e@x.com")
                .orgId(1L).deptId(2L)
                .build();
        Organization org = Organization.builder().orgId(1L).build();
        Department dept = Department.builder().deptId(2L).build();

        when(userRepository.existsByUserLoginId("newUser")).thenReturn(false);
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(org));
        when(departmentRepository.findById(2L)).thenReturn(Optional.of(dept));
        when(passwordEncoder.encode("pw")).thenReturn("ENC");

        UserResponseDto res = userService.createUser(dto);

        System.out.println("✅ TC_US_02_01 - 새 사용자 생성: " + res.getUserLoginId());
        assertEquals("newUser", res.getUserLoginId());
        assertEquals(1L, res.getOrganizationId());
        assertEquals(2L, res.getDepartmentId());
        verify(userRepository).save(any(User.class));
    }

    @Test @DisplayName("TC_US_02_02: 생성 시 로그인ID 중복 → DUPLICATED_LOGIN_ID")
    void createUser_dupLogin() {
        var dto = UserCreateRequestDto.builder().userLoginId("dup").build();
        when(userRepository.existsByUserLoginId("dup")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.createUser(dto));

        System.out.println("⚠️ TC_US_02_02 - 예외 코드: " + ex.getErrorCode());
        assertEquals(ErrorCode.DUPLICATED_LOGIN_ID, ex.getErrorCode());
    }

    @Test @DisplayName("TC_US_02_03: 생성 시 조직 없음 → ORGANIZATION_NOT_FOUND")
    void createUser_orgNotFound() {
        var dto = UserCreateRequestDto.builder()
                .userLoginId("u").orgId(9L).build();
        when(userRepository.existsByUserLoginId("u")).thenReturn(false);
        when(organizationRepository.findById(9L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.createUser(dto));

        System.out.println("⚠️ TC_US_02_03 - 예외 코드: " + ex.getErrorCode());
        assertEquals(ErrorCode.ORGANIZATION_NOT_FOUND, ex.getErrorCode());
    }

    @Test @DisplayName("TC_US_02_04: 생성 시 부서 없음 → DEPARTMENT_NOT_FOUND")
    void createUser_deptNotFound() {
        var dto = UserCreateRequestDto.builder()
                .userLoginId("u").deptId(8L).build();
        when(userRepository.existsByUserLoginId("u")).thenReturn(false);
        when(departmentRepository.findById(8L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.createUser(dto));

        System.out.println("⚠️ TC_US_02_04 - 예외 코드: " + ex.getErrorCode());
        assertEquals(ErrorCode.DEPARTMENT_NOT_FOUND, ex.getErrorCode());
    }

    // TC_US_03: 사용자 조회(getUserById)
    @Test @DisplayName("TC_US_03_01: 사용자 조회 성공")
    void getUserById_success() {
        User u = User.builder()
                .userId(5L).userLoginId("x").userName("X").email("x@x")
                .organization(Organization.builder().orgId(3L).orgName("O").build())
                .department(Department.builder().deptId(4L).deptName("D").build())
                .build();
        when(userRepository.findById(5L)).thenReturn(Optional.of(u));

        UserResponseDto res = userService.getUserById(5L);

        System.out.println("✅ TC_US_03_01 - 조회된 사용자: " + res.getUserName());
        assertEquals("X", res.getUserName());
        assertEquals("O", res.getOrganizationName());
        assertEquals("D", res.getDepartmentName());
    }

    @Test @DisplayName("TC_US_03_02: 조회 시 사용자 없음 → USER_NOT_FOUND")
    void getUserById_notFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.getUserById(99L));

        System.out.println("⚠️ TC_US_03_02 - 예외 코드: " + ex.getErrorCode());
        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
    }

    // TC_US_04: 사용자 수정(updateUser)
    @Test @DisplayName("TC_US_04_01: 사용자 수정 성공")
    void updateUser_success() {
        User existing = User.builder()
                .userId(7L)
                .userLoginId("old")
                .email("e@e")
                .organization(Organization.builder().orgId(2L).build())
                .build();
        UserUpdateRequestDto dto = UserUpdateRequestDto.builder()
                .userLoginId("new").userName("N").email("n@n").password("pw").build();

        securityUtil.when(SecurityUtil::getOrgId).thenReturn(2L);
        securityUtil.when(() -> SecurityUtil.hasRole("ROOT")).thenReturn(false);
        doNothing().when(permissionService).checkPermission("USER_UPDATE");
        when(userRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(userRepository.existsByUserLoginId("new")).thenReturn(false);
        when(userRepository.existsByEmailAndUserIdNot("n@n", 7L)).thenReturn(false);
        when(passwordEncoder.encode("pw")).thenReturn("ENC_PW");

        UserResponseDto res = userService.updateUser(7L, dto);

        System.out.println("✅ TC_US_04_01 - 수정된 로그인ID: " + res.getUserLoginId());
        assertEquals("new", res.getUserLoginId());
    }

    @Test @DisplayName("TC_US_04_02: 수정 시 사용자 없음 → USER_NOT_FOUND")
    void updateUser_notFound() {
        securityUtil.when(SecurityUtil::getOrgId).thenReturn(1L);
        doNothing().when(permissionService).checkPermission("USER_UPDATE");
        when(userRepository.findById(8L)).thenReturn(Optional.empty());
        UserUpdateRequestDto updateDto = UserUpdateRequestDto.builder()
                .userLoginId("newLoginId")
                .userName("newUserName")
                .email("newEmail@example.com")
                .password("newPassword")
                .build();
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.updateUser(8L, updateDto));

        System.out.println("⚠️ TC_US_04_02 - 예외 코드: " + ex.getErrorCode());
        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
    }

    @Test @DisplayName("TC_US_04_03: 수정 시 조직 불일치 → ACCESS_DENIED")
    void updateUser_accessDenied() {
        User u = User.builder()
                .userId(9L)
                .organization(Organization.builder().orgId(5L).build())
                .build();
        securityUtil.when(SecurityUtil::getOrgId).thenReturn(6L);
        UserUpdateRequestDto updateDto = UserUpdateRequestDto.builder()
                .userLoginId("newLoginId")
                .userName("newUserName")
                .email("newEmail@example.com")
                .password("newPassword")
                .build();
        securityUtil.when(() -> SecurityUtil.hasRole("ROOT")).thenReturn(false);
        doNothing().when(permissionService).checkPermission("USER_UPDATE");
        when(userRepository.findById(9L)).thenReturn(Optional.of(u));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.updateUser(9L, updateDto));

        System.out.println("⚠️ TC_US_04_03 - 예외 코드: " + ex.getErrorCode());
        assertEquals(ErrorCode.ACCESS_DENIED, ex.getErrorCode());
    }

    @Test @DisplayName("TC_US_04_04: 수정 시 로그인ID 중복 → USER_LOGIN_ID_DUPLICATED")
    void updateUser_dupLogin() {
        User u = User.builder().userId(10L).userLoginId("a").organization(Organization.builder().orgId(1L).build()).build();
        UserUpdateRequestDto dto = UserUpdateRequestDto.builder().userLoginId("b").build();
        securityUtil.when(SecurityUtil::getOrgId).thenReturn(1L);
        securityUtil.when(() -> SecurityUtil.hasRole("ROOT")).thenReturn(false);
        doNothing().when(permissionService).checkPermission("USER_UPDATE");
        when(userRepository.findById(10L)).thenReturn(Optional.of(u));
        when(userRepository.existsByUserLoginId("b")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.updateUser(10L, dto));

        System.out.println("⚠️ TC_US_04_04 - 예외 코드: " + ex.getErrorCode());
        assertEquals(ErrorCode.USER_LOGIN_ID_DUPLICATED, ex.getErrorCode());
    }

    @Test @DisplayName("TC_US_04_05: 수정 시 이메일 중복 → DUPLICATED_EMAIL")
    void updateUser_dupEmail() {
        User u = User.builder().userId(11L).organization(Organization.builder().orgId(1L).build()).build();
        UserUpdateRequestDto dto = UserUpdateRequestDto.builder().email("dup@e").build();
        securityUtil.when(SecurityUtil::getOrgId).thenReturn(1L);
        securityUtil.when(() -> SecurityUtil.hasRole("ROOT")).thenReturn(false);
        doNothing().when(permissionService).checkPermission("USER_UPDATE");
        when(userRepository.findById(11L)).thenReturn(Optional.of(u));
        when(userRepository.existsByEmailAndUserIdNot("dup@e", 11L)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.updateUser(11L, dto));

        System.out.println("⚠️ TC_US_04_05 - 예외 코드: " + ex.getErrorCode());
        assertEquals(ErrorCode.DUPLICATED_EMAIL, ex.getErrorCode());
    }

    // TC_US_05: 사용자 삭제(deleteUser)
    @Test @DisplayName("TC_US_05_01: 사용자 삭제 성공")
    void deleteUser_success() {
        User u = User.builder().userId(12L)
                .organization(Organization.builder().orgId(2L).build())
                .build();
        securityUtil.when(SecurityUtil::getOrgId).thenReturn(2L);
        securityUtil.when(() -> SecurityUtil.hasRole("ROOT")).thenReturn(false);
        doNothing().when(permissionService).checkPermission("USER_DELETE");
        when(userRepository.findById(12L)).thenReturn(Optional.of(u));

        userService.deleteUser(12L);
        System.out.println("✅ TC_US_05_01 - 사용자 삭제 처리");
        verify(userRepository).delete(u);
    }

    @Test @DisplayName("TC_US_05_02: 삭제 시 사용자 없음 → USER_NOT_FOUND")
    void deleteUser_notFound() {
        securityUtil.when(SecurityUtil::getOrgId).thenReturn(1L);
        doNothing().when(permissionService).checkPermission("USER_DELETE");
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.deleteUser(99L));

        System.out.println("⚠️ TC_US_05_02 - 예외 코드: " + ex.getErrorCode());
        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
    }

    @Test @DisplayName("TC_US_05_03: 삭제 시 조직 불일치 → ACCESS_DENIED")
    void deleteUser_accessDenied() {
        User u = User.builder().userId(13L)
                .organization(Organization.builder().orgId(3L).build())
                .build();
        securityUtil.when(SecurityUtil::getOrgId).thenReturn(4L);
        securityUtil.when(() -> SecurityUtil.hasRole("ROOT")).thenReturn(false);
        doNothing().when(permissionService).checkPermission("USER_DELETE");
        when(userRepository.findById(13L)).thenReturn(Optional.of(u));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.deleteUser(13L));

        System.out.println("⚠️ TC_US_05_03 - 예외 코드: " + ex.getErrorCode());
        assertEquals(ErrorCode.ACCESS_DENIED, ex.getErrorCode());
    }
}
