package com.fisa.solra.domain.permission.service;

import com.fisa.solra.domain.permission.dto.PermissionRequestDto;
import com.fisa.solra.domain.permission.dto.PermissionResponseDto;
import com.fisa.solra.domain.permission.entity.Permission;
import com.fisa.solra.domain.permission.repository.PermissionRepository;
import com.fisa.solra.global.exception.BusinessException;
import com.fisa.solra.global.exception.ErrorCode;
import com.fisa.solra.global.security.UserPrincipal;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.DisplayName.class)
class PermissionServiceTest {

    @InjectMocks
    private PermissionService permissionService;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private Authentication authentication;

    private SecurityContextImpl securityContext;

    @BeforeEach
    void setUp() {
        // 매 테스트마다 새로운 SecurityContextImpl을 만들어 SecurityContextHolder에 주입
        securityContext = new SecurityContextImpl();
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        // 테스트 종료 후에는 Context를 비워줍니다
        SecurityContextHolder.clearContext();
    }

    // TC_PM_01_01: getAuthorities → grantedAuthority 리스트 반환
    @Test
    @DisplayName("TC_PM_01_01: getAuthorities → grantedAuthority 리스트 반환")
    void getAuthorities_success() {
        // 1) permissionRepository.findAllPermissionNamesByUserId(1L) 호출 시 ["PERM_A", "PERM_B"] 리턴
        when(permissionRepository.findAllPermissionNamesByUserId(1L))
                .thenReturn(List.of("PERM_A", "PERM_B"));

        // 2) 실제 호출
        List<GrantedAuthority> authorities = permissionService.getAuthorities(1L);

        // 3) 검증: size 2, 각각 PERM_A, PERM_B 권한 포함
        assertEquals(2, authorities.size());
        assertTrue(
                authorities.stream().anyMatch(a -> a.getAuthority().equals("PERM_A")),
                "PERM_A GrantedAuthority가 포함되어야 합니다"
        );
        assertTrue(
                authorities.stream().anyMatch(a -> a.getAuthority().equals("PERM_B")),
                "PERM_B GrantedAuthority가 포함되어야 합니다"
        );

        // 결과 출력
        System.out.println("✅ TC_PM_01_01 - grantedAuthority 리스트 수: " + authorities.size());
    }

    // TC_PM_02_01: checkPermission → 인증 정보 없을 때 예외
    @Test
    @DisplayName("TC_PM_02_01: checkPermission → 인증 정보 없을 때 예외")
    void checkPermission_noAuth_throws() {
        // SecurityContextHolder.getContext().getAuthentication() → null
        securityContext.setAuthentication(null);

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> permissionService.checkPermission("ANY_PERMISSION")
        );
        assertEquals(ErrorCode.SECURITY_CONTEXT_NOT_FOUND, ex.getErrorCode());

        // 결과 출력
        System.out.println("⚠️ TC_PM_02_01 - 예외 코드: " + ex.getErrorCode());
    }

    // TC_PM_02_02: checkPermission → principal이 UserPrincipal이 아니면 예외
    @Test
    @DisplayName("TC_PM_02_02: checkPermission → principal이 UserPrincipal 아니면 예외")
    void checkPermission_invalidPrincipal_throws() {
        // 1) SecurityContextHolder에 mock Authentication 주입
        securityContext.setAuthentication(authentication);
        // 2) authentication.getPrincipal() → 임의의 Object 반환
        when(authentication.getPrincipal()).thenReturn(new Object());

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> permissionService.checkPermission("ANY_PERMISSION")
        );
        assertEquals(ErrorCode.SECURITY_CONTEXT_NOT_FOUND, ex.getErrorCode());

        // 결과 출력
        System.out.println("⚠️ TC_PM_02_02 - 예외 코드: " + ex.getErrorCode());
    }

    // TC_PM_02_03: checkPermission → ROOT role 보유 시 통과
    @Test
    @DisplayName("TC_PM_02_03: checkPermission → ROOT role 보유 시 통과")
    void checkPermission_rootRole_passes() {
        // 1) SecurityContextHolder에 mock Authentication 주입
        securityContext.setAuthentication(authentication);

        // 2) 실제 애플리케이션 UserPrincipal을 만들어 PRINCIPAL로 설정
        //    – 생성자 시그니처: UserPrincipal(Long userId, Long orgId, Long deptId,
        //                               List<String> roles, Collection<? extends GrantedAuthority> authorities)
        UserPrincipal principal = new UserPrincipal(
                1L,               // userId
                1L,               // orgId (테스트에서 원하는 값 아무거나)
                1L,               // deptId (테스트에서 원하는 값 아무거나)
                List.of("ROOT"),  // roles 중 ROOT 포함 → ROOT 우회 허용
                List.of(new SimpleGrantedAuthority("ANY_AUTH"))  // authorities 값 무의미
        );
        when(authentication.getPrincipal()).thenReturn(principal);

        // 3) 실제 호출 → 예외 없이 통과 (ROOT 우회)
        assertDoesNotThrow(
                () -> permissionService.checkPermission("SOME_PERMISSION"),
                "ROOT 역할이 있으면 무조건 통과해야 합니다"
        );

        // 결과 출력
        System.out.println("✅ TC_PM_02_03 - ROOT 역할 우회 통과");
    }

    // TC_PM_02_04: checkPermission → 권한이 없으면 예외 (ACCESS_DENIED)
    @Test
    @DisplayName("TC_PM_02_04: checkPermission → 권한이 없으면 예외 (ACCESS_DENIED)")
    void checkPermission_noMatchingAuthority_throws() {
        // 1) SecurityContextHolder에 mock Authentication 주입
        securityContext.setAuthentication(authentication);

        // 2) UserPrincipal 설정 (roles=”USER”이고, authorities에 “OTHER_GRANTED”만 있음)
        UserPrincipal principal = new UserPrincipal(
                3L,                           // userId
                1L,                           // orgId
                1L,                           // deptId
                List.of("USER"),              // roles
                List.of(new SimpleGrantedAuthority("OTHER_GRANTED"))  // authorities
        );
        when(authentication.getPrincipal()).thenReturn(principal);

        // 3) 실제 호출 → “REQUIRED” 권한이 없으므로 ACCESS_DENIED 예외
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> permissionService.checkPermission("REQUIRED")
        );
        assertEquals(ErrorCode.ACCESS_DENIED, ex.getErrorCode());

        // 결과 출력
        System.out.println("⚠️ TC_PM_02_04 - 예외 코드: " + ex.getErrorCode());
    }

    // TC_PM_03_01: createPermission → 성공
    @Test
    @DisplayName("TC_PM_03_01: createPermission → 성공")
    void createPermission_success() {
        // 1) 입력 DTO 준비 (permissionName="NEW_PERM")
        PermissionRequestDto req = PermissionRequestDto.builder()
                .permissionName("NEW_PERM")
                .description("desc")
                .build();

        // 2) existsByPermissionName("NEW_PERM") → false (중복 아님)
        when(permissionRepository.existsByPermissionName("NEW_PERM"))
                .thenReturn(false);

        // 3) 저장될 엔티티 stub 설정: save(any(Permission.class)) → ID=100L, name="NEW_PERM", desc="desc"
        Permission savedEntity = Permission.builder()
                .permissionId(100L)
                .permissionName("NEW_PERM")
                .description("desc")
                .build();
        when(permissionRepository.save(any(Permission.class)))
                .thenReturn(savedEntity);

        // 4) 실제 호출
        PermissionResponseDto dto = permissionService.createPermission(req);

        // 5) 검증: ID 100, 이름 "NEW_PERM", desc "desc"
        assertEquals(100L, dto.getPermissionId());
        assertEquals("NEW_PERM", dto.getPermissionName());
        assertEquals("desc", dto.getDescription());

        // 결과 출력
        System.out.println("✅ TC_PM_03_01 - 생성된 Permission ID: " + dto.getPermissionId());
    }

    // TC_PM_03_02: createPermission → 권한 이름 null 혹은 공백 입력 시 예외
    @Test
    @DisplayName("TC_PM_03_02: createPermission → 권한 이름 미입력 시 예외 (INVALID_INPUT)")
    void createPermission_nullOrBlankName_throws() {
        // name이 공백인 경우
        PermissionRequestDto reqBlank = PermissionRequestDto.builder()
                .permissionName("   ")
                .description("desc")
                .build();
        BusinessException ex1 = assertThrows(
                BusinessException.class,
                () -> permissionService.createPermission(reqBlank)
        );
        assertEquals(ErrorCode.INVALID_INPUT, ex1.getErrorCode());
        System.out.println("⚠️ TC_PM_03_02 - 예외 코드: " + ex1.getErrorCode());

        // name이 null인 경우
        PermissionRequestDto reqNull = PermissionRequestDto.builder()
                .permissionName(null)
                .description("desc")
                .build();
        BusinessException ex2 = assertThrows(
                BusinessException.class,
                () -> permissionService.createPermission(reqNull)
        );
        assertEquals(ErrorCode.INVALID_INPUT, ex2.getErrorCode());
        // (추가적인 출력은 생략해도 무방)
    }

    // TC_PM_03_03: createPermission → 중복 이름 입력 시 예외 (DUPLICATED_PERMISSION_NAME)
    @Test
    @DisplayName("TC_PM_03_03: createPermission → 중복 이름 시 예외 (DUPLICATED_PERMISSION_NAME)")
    void createPermission_duplicateName_throws() {
        // existsByPermissionName("DUPLICATE") → true
        PermissionRequestDto req = PermissionRequestDto.builder()
                .permissionName("DUPLICATE")
                .description("desc")
                .build();
        when(permissionRepository.existsByPermissionName("DUPLICATE"))
                .thenReturn(true);

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> permissionService.createPermission(req)
        );
        assertEquals(ErrorCode.DUPLICATED_PERMISSION_NAME, ex.getErrorCode());

        // 결과 출력
        System.out.println("⚠️ TC_PM_03_03 - 예외 코드: " + ex.getErrorCode());
    }

    // TC_PM_03_04: createPermission → DB 제약 위반 시 예외 (INTERNAL_SERVER_ERROR)
    @Test
    @DisplayName("TC_PM_03_04: createPermission → DB 제약 오류 시 예외 (INTERNAL_SERVER_ERROR)")
    void createPermission_saveThrowsDBError_throws() {
        // existsByPermissionName("ANY") → false
        PermissionRequestDto req = PermissionRequestDto.builder()
                .permissionName("ANY")
                .description("desc")
                .build();
        when(permissionRepository.existsByPermissionName("ANY"))
                .thenReturn(false);
        // save(...) 호출 시 DataIntegrityViolationException 던지기
        when(permissionRepository.save(any(Permission.class)))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("DB error"));

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> permissionService.createPermission(req)
        );
        assertEquals(ErrorCode.INTERNAL_SERVER_ERROR, ex.getErrorCode());

        // 결과 출력
        System.out.println("⚠️ TC_PM_03_04 - 예외 코드: " + ex.getErrorCode());
    }

    // TC_PM_04_01: getAllPermissions → 권한 목록 반환
    @Test
    @DisplayName("TC_PM_04_01: getAllPermissions → 권한 목록 반환")
    void getAllPermissions_success() {
        // findAll() → 두 개의 Permission 엔티티 리턴
        Permission p1 = Permission.builder()
                .permissionId(1L)
                .permissionName("P1")
                .description("d1")
                .build();
        Permission p2 = Permission.builder()
                .permissionId(2L)
                .permissionName("P2")
                .description("d2")
                .build();
        when(permissionRepository.findAll())
                .thenReturn(List.of(p1, p2));

        // 실제 호출
        List<PermissionResponseDto> dtoList = permissionService.getAllPermissions();

        // 검증: size=2, 권한 이름 "P1","P2"
        assertEquals(2, dtoList.size());
        assertEquals("P1", dtoList.get(0).getPermissionName());
        assertEquals("P2", dtoList.get(1).getPermissionName());

        // 결과 출력
        System.out.println("✅ TC_PM_04_01 - 권한 목록 수: " + dtoList.size());
    }

    // TC_PM_04_02: getAllPermissions → 빈 리스트 예외 (PERMISSION_NOT_FOUND)
    @Test
    @DisplayName("TC_PM_04_02: getAllPermissions → 빈 리스트 예외 (PERMISSION_NOT_FOUND)")
    void getAllPermissions_empty_throws() {
        // findAll() → 빈 리스트
        when(permissionRepository.findAll())
                .thenReturn(Collections.emptyList());

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> permissionService.getAllPermissions()
        );
        assertEquals(ErrorCode.PERMISSION_NOT_FOUND, ex.getErrorCode());

        // 결과 출력
        System.out.println("⚠️ TC_PM_04_02 - 예외 코드: " + ex.getErrorCode());
    }

    // TC_PM_05_01: updatePermission → 없는 ID 예외 (PERMISSION_NOT_FOUND)
    @Test
    @DisplayName("TC_PM_05_01: updatePermission → 없는 ID 예외 (PERMISSION_NOT_FOUND)")
    void updatePermission_notFound_throws() {
        // findById(999L) → Optional.empty()
        when(permissionRepository.findById(999L))
                .thenReturn(Optional.empty());

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> permissionService.updatePermission(
                        999L,
                        PermissionRequestDto.builder()
                                .permissionName(null)
                                .description("x")
                                .build()
                )
        );
        assertEquals(ErrorCode.PERMISSION_NOT_FOUND, ex.getErrorCode());

        // 결과 출력
        System.out.println("⚠️ TC_PM_05_01 - 예외 코드: " + ex.getErrorCode());
    }

    // TC_PM_05_02: updatePermission → 설명 미입력 예외 (INVALID_INPUT)
    @Test
    @DisplayName("TC_PM_05_02: updatePermission → 설명 미입력 예외 (INVALID_INPUT)")
    void updatePermission_invalidInput_throws() {
        // findById(10L) → Permission(description="old")
        Permission existing = Permission.builder()
                .permissionId(10L)
                .permissionName("XYZ")
                .description("old")
                .build();
        when(permissionRepository.findById(10L))
                .thenReturn(Optional.of(existing));

        // description null
        PermissionRequestDto bad1 = PermissionRequestDto.builder()
                .permissionName(null)
                .description(null)
                .build();
        BusinessException ex1 = assertThrows(
                BusinessException.class,
                () -> permissionService.updatePermission(10L, bad1)
        );
        assertEquals(ErrorCode.INVALID_INPUT, ex1.getErrorCode());
        System.out.println("⚠️ TC_PM_05_02 - 예외 코드: " + ex1.getErrorCode());

        // description 공백
        PermissionRequestDto bad2 = PermissionRequestDto.builder()
                .permissionName(null)
                .description("   ")
                .build();
        BusinessException ex2 = assertThrows(
                BusinessException.class,
                () -> permissionService.updatePermission(10L, bad2)
        );
        assertEquals(ErrorCode.INVALID_INPUT, ex2.getErrorCode());
        // (추가 출력을 생략해도 무방)
    }

    // TC_PM_05_03: updatePermission → 동일 설명 예외 (PERMISSION_ALREADY_ASSIGNED)
    @Test
    @DisplayName("TC_PM_05_03: updatePermission → 동일 설명 예외 (PERMISSION_ALREADY_ASSIGNED)")
    void updatePermission_sameDescription_throws() {
        // findById(20L) → Permission(description="same")
        Permission existing = Permission.builder()
                .permissionId(20L)
                .permissionName("ABC")
                .description("same")
                .build();
        when(permissionRepository.findById(20L))
                .thenReturn(Optional.of(existing));

        // 요청 DTO의 description도 "same"
        PermissionRequestDto dto = PermissionRequestDto.builder()
                .permissionName(null)
                .description("same")
                .build();

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> permissionService.updatePermission(20L, dto)
        );
        assertEquals(ErrorCode.PERMISSION_ALREADY_ASSIGNED, ex.getErrorCode());

        // 결과 출력
        System.out.println("⚠️ TC_PM_05_03 - 예외 코드: " + ex.getErrorCode());
    }

    // TC_PM_05_04: updatePermission → 성공
    @Test
    @DisplayName("TC_PM_05_04: updatePermission → 성공")
    void updatePermission_success() {
        // findById(30L) → Permission(description="old")
        Permission existing = Permission.builder()
                .permissionId(30L)
                .permissionName("XYZ")
                .description("old")
                .build();
        when(permissionRepository.findById(30L))
                .thenReturn(Optional.of(existing));

        // 요청 DTO(description="newDesc")
        PermissionRequestDto dto = PermissionRequestDto.builder()
                .permissionName(null)
                .description("newDesc")
                .build();

        // 실제 호출
        PermissionResponseDto result = permissionService.updatePermission(30L, dto);

        // 검증: ID=30, name="XYZ", desc="newDesc"
        assertEquals(30L, result.getPermissionId());
        assertEquals("XYZ", result.getPermissionName());
        assertEquals("newDesc", result.getDescription());

        // 결과 출력
        System.out.println("✅ TC_PM_05_04 - 업데이트된 설명: " + result.getDescription());
    }

    // TC_PM_06_01: deletePermission → 없는 ID 예외 (PERMISSION_NOT_FOUND)
    @Test
    @DisplayName("TC_PM_06_01: deletePermission → 없는 ID 예외 (PERMISSION_NOT_FOUND)")
    void deletePermission_notFound_throws() {
        // existsById(50L) → false
        when(permissionRepository.existsById(50L))
                .thenReturn(false);

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> permissionService.deletePermission(50L)
        );
        assertEquals(ErrorCode.PERMISSION_NOT_FOUND, ex.getErrorCode());

        // 결과 출력
        System.out.println("⚠️ TC_PM_06_01 - 예외 코드: " + ex.getErrorCode());
    }

    // TC_PM_06_02: deletePermission → 성공
    @Test
    @DisplayName("TC_PM_06_02: deletePermission → 성공")
    void deletePermission_success() {
        // existsById(70L) → true
        when(permissionRepository.existsById(70L))
                .thenReturn(true);

        // 실제 호출: 예외 없이 통과해야 함
        assertDoesNotThrow(
                () -> permissionService.deletePermission(70L)
        );

        // deleteById(70L)와 flush()가 각각 1번씩 호출되었는지 검증
        verify(permissionRepository, times(1)).deleteById(70L);
        verify(permissionRepository, times(1)).flush();

        // 결과 출력
        System.out.println("✅ TC_PM_06_02 - deletePermission 호출됨");
    }
}
