package com.fisa.solra.domain.department.service;

import com.fisa.solra.domain.department.dto.DepartmentRequestDto;
import com.fisa.solra.domain.department.dto.DepartmentResponseDto;
import com.fisa.solra.domain.department.entity.Department;
import com.fisa.solra.domain.department.repository.DepartmentRepository;
import com.fisa.solra.domain.organization.entity.Organization;
import com.fisa.solra.domain.organization.repository.OrganizationRepository;
import com.fisa.solra.domain.permission.service.PermissionService;
import com.fisa.solra.global.exception.BusinessException;
import com.fisa.solra.global.exception.ErrorCode;
import com.fisa.solra.global.util.SecurityUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.DisplayName.class)
class DepartmentServiceTest {

    @InjectMocks
    private DepartmentService departmentService;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private PermissionService permissionService;

    private MockedStatic<SecurityUtil> securityUtilMock;

    @BeforeEach
    void setUp() {
        // SecurityUtil static 메서드 모킹 준비
        securityUtilMock = mockStatic(SecurityUtil.class);
    }

    @AfterEach
    void tearDown() {
        securityUtilMock.close();
    }

    // TC_DP_01_01: 전체 부서 조회 성공
    @Test
    @DisplayName("TC_DP_01_01: 전체 부서 조회 → 성공")
    void getAllDepartments_success() {
        // given
        // .hasRole("ROOT") → true
        securityUtilMock.when(() -> SecurityUtil.hasRole("ROOT")).thenReturn(true);
        // permissionService.checkPermission 은 void 메서드이므로 doNothing 사용
        doNothing().when(permissionService).checkPermission("DEPARTMENT_READ");

        List<Department> deptList = List.of(
                createDepartment(1L, "DeptA", 10L, "OrgA"),
                createDepartment(2L, "DeptB", 10L, "OrgA")
        );
        when(departmentRepository.findAll()).thenReturn(deptList);

        // when
        List<DepartmentResponseDto> result = departmentService.getAllDepartments(null);

        // then
        System.out.println("✅ TC_DP_01_01 - 조회된 부서 수: " + result.size());
        assertEquals(2, result.size());
        assertEquals(
                Set.of("DeptA", "DeptB"),
                result.stream().map(DepartmentResponseDto::getDeptName).collect(Collectors.toSet())
        );
    }

    // TC_DP_01_02: 전체 부서 조회 결과 없음 → 예외
    @Test
    @DisplayName("TC_DP_01_02: 전체 부서 조회 → 예외 (DEPARTMENT_NOT_FOUND)")
    void getAllDepartments_notFound() {
        // given
        securityUtilMock.when(() -> SecurityUtil.hasRole("ROOT")).thenReturn(true);
        doNothing().when(permissionService).checkPermission("DEPARTMENT_READ");
        when(departmentRepository.findAll()).thenReturn(Collections.emptyList());

        // when & then
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> departmentService.getAllDepartments(null)
        );
        System.out.println("⚠️ TC_DP_01_02 - 예외 코드: " + ex.getErrorCode());
        assertEquals(ErrorCode.DEPARTMENT_NOT_FOUND, ex.getErrorCode());
    }

    // TC_DP_02_01: 단일 부서 조회 성공 (ROOT 권한)
    @Test
    @DisplayName("TC_DP_02_01: 단일 부서 조회 → 성공 (ROOT 권한)")
    void getDepartmentById_success_root() {
        // given
        Long deptId = 100L;
        Organization org = createOrganization(20L, "OrgX");
        Department dept = createDepartment(deptId, "DeptX", org.getOrgId(), org.getOrgName());

        doNothing().when(permissionService).checkPermission("DEPARTMENT_READ");
        when(departmentRepository.findById(deptId)).thenReturn(Optional.of(dept));
        securityUtilMock.when(() -> SecurityUtil.hasRole("ROOT")).thenReturn(true);

        // when
        DepartmentResponseDto result = departmentService.getDepartmentById(deptId);

        // then
        System.out.println("✅ TC_DP_02_01 - 조회된 부서: " + result.getDeptName());
        assertEquals("DeptX", result.getDeptName());
        assertEquals(org.getOrgId(), result.getOrgId());
    }

    // TC_DP_02_02: 단일 부서 조회 성공 (조직 일치)
    @Test
    @DisplayName("TC_DP_02_02: 단일 부서 조회 → 성공 (조직 일치)")
    void getDepartmentById_success_sameOrg() {
        // given
        Long deptId = 101L;
        Organization org = createOrganization(30L, "OrgY");
        Department dept = createDepartment(deptId, "DeptY", org.getOrgId(), org.getOrgName());

        doNothing().when(permissionService).checkPermission("DEPARTMENT_READ");
        when(departmentRepository.findById(deptId)).thenReturn(Optional.of(dept));
        securityUtilMock.when(() -> SecurityUtil.hasRole("ROOT")).thenReturn(false);
        securityUtilMock.when(SecurityUtil::getOrgId).thenReturn(org.getOrgId());

        // when
        DepartmentResponseDto result = departmentService.getDepartmentById(deptId);

        // then
        System.out.println("✅ TC_DP_02_02 - 조회된 부서: " + result.getDeptName());
        assertEquals("DeptY", result.getDeptName());
        assertEquals(org.getOrgId(), result.getOrgId());
    }

    // TC_DP_02_03: 단일 부서 조회 실패 - 부서 없음
    @Test
    @DisplayName("TC_DP_02_03: 단일 부서 조회 → 예외 (DEPARTMENT_NOT_FOUND)")
    void getDepartmentById_notFound() {
        // given
        Long deptId = 999L;
        doNothing().when(permissionService).checkPermission("DEPARTMENT_READ");
        when(departmentRepository.findById(deptId)).thenReturn(Optional.empty());

        // when & then
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> departmentService.getDepartmentById(deptId)
        );
        System.out.println("⚠️ TC_DP_02_03 - 예외 코드: " + ex.getErrorCode());
        assertEquals(ErrorCode.DEPARTMENT_NOT_FOUND, ex.getErrorCode());
    }

    // TC_DP_02_04: 단일 부서 조회 실패 - 조직 불일치
    @Test
    @DisplayName("TC_DP_02_04: 단일 부서 조회 → 예외 (ACCESS_DENIED)")
    void getDepartmentById_accessDenied() {
        // given
        Long deptId = 102L;
        Organization org = createOrganization(40L, "OrgZ");
        Department dept = createDepartment(deptId, "DeptZ", org.getOrgId(), org.getOrgName());

        doNothing().when(permissionService).checkPermission("DEPARTMENT_READ");
        when(departmentRepository.findById(deptId)).thenReturn(Optional.of(dept));
        securityUtilMock.when(() -> SecurityUtil.hasRole("ROOT")).thenReturn(false);
        securityUtilMock.when(SecurityUtil::getOrgId).thenReturn(999L);

        // when & then
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> departmentService.getDepartmentById(deptId)
        );
        System.out.println("⚠️ TC_DP_02_04 - 예외 코드: " + ex.getErrorCode());
        assertEquals(ErrorCode.ACCESS_DENIED, ex.getErrorCode());
    }

    // TC_DP_03_01: 부서 생성 성공 (ROOT 권한)
    @Test
    @DisplayName("TC_DP_03_01: 부서 생성 → 성공 (ROOT 권한)")
    void createDepartment_success_root() {
        // given
        Long orgId = 50L;
        DepartmentRequestDto dto = DepartmentRequestDto.builder()
                .organizationId(orgId)
                .deptName("NewDept")
                .build();

        doNothing().when(permissionService).checkPermission("DEPARTMENT_CREATE");
        when(organizationRepository.existsById(orgId)).thenReturn(true);
        securityUtilMock.when(() -> SecurityUtil.hasRole("ROOT")).thenReturn(true);

        Organization org = createOrganization(orgId, "OrgNew");
        when(organizationRepository.getReferenceById(orgId)).thenReturn(org);

        Department saved = Department.builder()
                .deptId(200L)
                .organization(org)
                .deptName("NewDept")
                .build();

        when(departmentRepository.existsByOrganizationOrgIdAndDeptName(orgId, "NewDept"))
                .thenReturn(false);
        when(departmentRepository.save(any(Department.class))).thenReturn(saved);

        // when
        DepartmentResponseDto result = departmentService.createDepartment(dto);

        // then
        System.out.println("✅ TC_DP_03_01 - 생성된 부서 ID: " + result.getDeptId());
        assertEquals("NewDept", result.getDeptName());
        assertEquals(orgId, result.getOrgId());
    }

    // TC_DP_03_02: 부서 생성 실패 - 조직 ID 없음
    @Test
    @DisplayName("TC_DP_03_02: 부서 생성 → 예외 (ORGANIZATION_NOT_FOUND)")
    void createDepartment_orgNotFound() {
        // given
        DepartmentRequestDto dto = DepartmentRequestDto.builder()
                .organizationId(999L)
                .deptName("DeptX")
                .build();

        doNothing().when(permissionService).checkPermission("DEPARTMENT_CREATE");
        when(organizationRepository.existsById(999L)).thenReturn(false);

        // when & then
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> departmentService.createDepartment(dto)
        );
        System.out.println("⚠️ TC_DP_03_02 - 예외 코드: " + ex.getErrorCode());
        assertEquals(ErrorCode.ORGANIZATION_NOT_FOUND, ex.getErrorCode());
    }

    // TC_DP_03_03: 부서 생성 실패 - 조직 불일치
    @Test
    @DisplayName("TC_DP_03_03: 부서 생성 → 예외 (ACCESS_DENIED)")
    void createDepartment_accessDenied() {
        // given
        Long orgId = 60L;
        DepartmentRequestDto dto = DepartmentRequestDto.builder()
                .organizationId(orgId)
                .deptName("DeptY")
                .build();

        doNothing().when(permissionService).checkPermission("DEPARTMENT_CREATE");
        when(organizationRepository.existsById(orgId)).thenReturn(true);
        securityUtilMock.when(() -> SecurityUtil.hasRole("ROOT")).thenReturn(false);
        securityUtilMock.when(SecurityUtil::getOrgId).thenReturn(999L);

        // when & then
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> departmentService.createDepartment(dto)
        );
        System.out.println("⚠️ TC_DP_03_03 - 예외 코드: " + ex.getErrorCode());
        assertEquals(ErrorCode.ACCESS_DENIED, ex.getErrorCode());
    }

    // TC_DP_03_04: 부서 생성 실패 - 이름 유효성 실패
    @Test
    @DisplayName("TC_DP_03_04: 부서 생성 → 예외 (INVALID_INPUT)")
    void createDepartment_invalidInput() {
        // given
        Long orgId = 70L;
        DepartmentRequestDto dto = DepartmentRequestDto.builder()
                .organizationId(orgId)
                .deptName("   ")
                .build();

        doNothing().when(permissionService).checkPermission("DEPARTMENT_CREATE");
        when(organizationRepository.existsById(orgId)).thenReturn(true);
        securityUtilMock.when(() -> SecurityUtil.hasRole("ROOT")).thenReturn(true);

        // when & then
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> departmentService.createDepartment(dto)
        );
        System.out.println("⚠️ TC_DP_03_04 - 예외 코드: " + ex.getErrorCode());
        assertEquals(ErrorCode.INVALID_INPUT, ex.getErrorCode());
    }

    // TC_DP_03_05: 부서 생성 실패 - 중복 이름
    @Test
    @DisplayName("TC_DP_03_05: 부서 생성 → 예외 (DUPLICATED_DEPARTMENT_NAME)")
    void createDepartment_duplicateName() {
        // given
        Long orgId = 80L;
        DepartmentRequestDto dto = DepartmentRequestDto.builder()
                .organizationId(orgId)
                .deptName("DupDept")
                .build();

        doNothing().when(permissionService).checkPermission("DEPARTMENT_CREATE");
        when(organizationRepository.existsById(orgId)).thenReturn(true);
        securityUtilMock.when(() -> SecurityUtil.hasRole("ROOT")).thenReturn(true);
        when(departmentRepository.existsByOrganizationOrgIdAndDeptName(orgId, "DupDept"))
                .thenReturn(true);

        // when & then
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> departmentService.createDepartment(dto)
        );
        System.out.println("⚠️ TC_DP_03_05 - 예외 코드: " + ex.getErrorCode());
        assertEquals(ErrorCode.DUPLICATED_DEPARTMENT_NAME, ex.getErrorCode());
    }

    // TC_DP_03_06: 부서 생성 실패 - DataIntegrityViolationException
    @Test
    @DisplayName("TC_DP_03_06: 부서 생성 → 예외 (DEPARTMENT_CREATE_FAILED)")
    void createDepartment_dataIntegrityViolation() {
        // given
        Long orgId = 90L;
        DepartmentRequestDto dto = DepartmentRequestDto.builder()
                .organizationId(orgId)
                .deptName("DeptFail")
                .build();

        doNothing().when(permissionService).checkPermission("DEPARTMENT_CREATE");
        when(organizationRepository.existsById(orgId)).thenReturn(true);
        securityUtilMock.when(() -> SecurityUtil.hasRole("ROOT")).thenReturn(true);
        when(departmentRepository.existsByOrganizationOrgIdAndDeptName(orgId, "DeptFail"))
                .thenReturn(false);
        when(organizationRepository.getReferenceById(orgId))
                .thenReturn(createOrganization(orgId, "OrgFail"));
        when(departmentRepository.save(any(Department.class)))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("constraint"));

        // when & then
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> departmentService.createDepartment(dto)
        );
        System.out.println("⚠️ TC_DP_03_06 - 예외 코드: " + ex.getErrorCode());
        assertEquals(ErrorCode.DEPARTMENT_CREATE_FAILED, ex.getErrorCode());
    }

    // TC_DP_04_01: 부서명 수정 성공 (ROOT 권한)
    @Test
    @DisplayName("TC_DP_04_01: 부서명 수정 → 성공 (ROOT 권한)")
    void updateDepartmentName_success_root() {
        // given
        Long deptId = 300L;
        Organization org = createOrganization(100L, "OrgUp");
        Department existing = createDepartment(deptId, "OldName", org.getOrgId(), org.getOrgName());

        DepartmentRequestDto dto = DepartmentRequestDto.builder()
                .deptName("NewName")
                .build();

        doNothing().when(permissionService).checkPermission("DEPARTMENT_UPDATE");
        when(departmentRepository.findById(deptId)).thenReturn(Optional.of(existing));
        securityUtilMock.when(() -> SecurityUtil.hasRole("ROOT")).thenReturn(true);
        when(departmentRepository.existsByOrganizationOrgIdAndDeptName(org.getOrgId(), "NewName"))
                .thenReturn(false);

        // when
        DepartmentResponseDto result = departmentService.updateDepartmentName(deptId, dto);

        // then
        System.out.println("✅ TC_DP_04_01 - 수정된 부서명: " + result.getDeptName());
        assertEquals("NewName", result.getDeptName());
        assertEquals(org.getOrgId(), result.getOrgId());
    }

    // TC_DP_04_02: 부서명 수정 실패 - 부서 없음
    @Test
    @DisplayName("TC_DP_04_02: 부서명 수정 → 예외 (DEPARTMENT_NOT_FOUND)")
    void updateDepartmentName_notFound() {
        // given
        Long deptId = 301L;
        DepartmentRequestDto dto = DepartmentRequestDto.builder().deptName("X").build();

        doNothing().when(permissionService).checkPermission("DEPARTMENT_UPDATE");
        when(departmentRepository.findById(deptId)).thenReturn(Optional.empty());

        // when & then
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> departmentService.updateDepartmentName(deptId, dto)
        );
        System.out.println("⚠️ TC_DP_04_02 - 예외 코드: " + ex.getErrorCode());
        assertEquals(ErrorCode.DEPARTMENT_NOT_FOUND, ex.getErrorCode());
    }

    // TC_DP_04_03: 부서명 수정 실패 - 권한 없음
    @Test
    @DisplayName("TC_DP_04_03: 부서명 수정 → 예외 (ACCESS_DENIED)")
    void updateDepartmentName_accessDenied() {
        // given
        Long deptId = 302L;
        Organization org = createOrganization(110L, "OrgA");
        Department existing = createDepartment(deptId, "DeptA", org.getOrgId(), org.getOrgName());

        DepartmentRequestDto dto = DepartmentRequestDto.builder().deptName("DeptB").build();

        doNothing().when(permissionService).checkPermission("DEPARTMENT_UPDATE");
        when(departmentRepository.findById(deptId)).thenReturn(Optional.of(existing));
        securityUtilMock.when(() -> SecurityUtil.hasRole("ROOT")).thenReturn(false);
        securityUtilMock.when(SecurityUtil::getOrgId).thenReturn(999L);

        // when & then
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> departmentService.updateDepartmentName(deptId, dto)
        );
        System.out.println("⚠️ TC_DP_04_03 - 예외 코드: " + ex.getErrorCode());
        assertEquals(ErrorCode.ACCESS_DENIED, ex.getErrorCode());
    }

    // TC_DP_04_04: 부서명 수정 실패 - 입력값 유효성 실패
    @Test
    @DisplayName("TC_DP_04_04: 부서명 수정 → 예외 (INVALID_INPUT)")
    void updateDepartmentName_invalidInput() {
        // given
        Long deptId = 303L;
        Organization org = createOrganization(120L, "OrgB");
        Department existing = createDepartment(deptId, "DeptB", org.getOrgId(), org.getOrgName());

        DepartmentRequestDto dto = DepartmentRequestDto.builder().deptName("   ").build();

        doNothing().when(permissionService).checkPermission("DEPARTMENT_UPDATE");
        when(departmentRepository.findById(deptId)).thenReturn(Optional.of(existing));
        securityUtilMock.when(() -> SecurityUtil.hasRole("ROOT")).thenReturn(true);

        // when & then
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> departmentService.updateDepartmentName(deptId, dto)
        );
        System.out.println("⚠️ TC_DP_04_04 - 예외 코드: " + ex.getErrorCode());
        assertEquals(ErrorCode.INVALID_INPUT, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC_DP_04_05: 부서명 수정 → 예외 (DUPLICATED_DEPARTMENT_NAME)")
    void updateDepartmentName_duplicateName() {
        // given
        Long deptId = 304L;
        Organization org = createOrganization(130L, "OrgC");
        Department existing = createDepartment(deptId, "DeptC", org.getOrgId(), org.getOrgName());

        // ① newName을 기존과 다른 "DeptX"로 설정
        DepartmentRequestDto dto = DepartmentRequestDto.builder()
                .deptName("DeptX")
                .build();

        doNothing().when(permissionService).checkPermission("DEPARTMENT_UPDATE");
        when(departmentRepository.findById(deptId)).thenReturn(Optional.of(existing));
        // ROOT 권한 모킹
        securityUtilMock.when(() -> SecurityUtil.hasRole("ROOT")).thenReturn(true);

        // ② “같은 조직(OrgC)에 이미 'DeptX'라는 부서가 있다”라고 stub
        when(departmentRepository.existsByOrganizationOrgIdAndDeptName(org.getOrgId(), "DeptX"))
                .thenReturn(true);

        // when & then (중복 예외 발생)
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> departmentService.updateDepartmentName(deptId, dto)
        );
        System.out.println("⚠️ TC_DP_04_05 - 예외 코드: " + ex.getErrorCode());
        assertEquals(ErrorCode.DUPLICATED_DEPARTMENT_NAME, ex.getErrorCode());
    }

    // TC_DP_05_01: 부서 삭제 성공 (ROOT 권한)
    @Test
    @DisplayName("TC_DP_05_01: 부서 삭제 → 성공 (ROOT 권한)")
    void deleteDepartment_success_root() {
        // given
        Long deptId = 400L;
        Organization org = createOrganization(140L, "OrgDel");
        Department existing = createDepartment(deptId, "DeptDel", org.getOrgId(), org.getOrgName());

        doNothing().when(permissionService).checkPermission("DEPARTMENT_DELETE");
        when(departmentRepository.findById(deptId)).thenReturn(Optional.of(existing));
        securityUtilMock.when(() -> SecurityUtil.hasRole("ROOT")).thenReturn(true);

        // when
        departmentService.deleteDepartment(deptId);

        // then
        System.out.println("✅ TC_DP_05_01 - 부서 삭제 호출됨");
        verify(departmentRepository, times(1)).deleteById(deptId);
    }

    // TC_DP_05_02: 부서 삭제 실패 - 부서 없음
    @Test
    @DisplayName("TC_DP_05_02: 부서 삭제 → 예외 (DEPARTMENT_NOT_FOUND)")
    void deleteDepartment_notFound() {
        // given
        Long deptId = 401L;
        doNothing().when(permissionService).checkPermission("DEPARTMENT_DELETE");
        when(departmentRepository.findById(deptId)).thenReturn(Optional.empty());

        // when & then
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> departmentService.deleteDepartment(deptId)
        );
        System.out.println("⚠️ TC_DP_05_02 - 예외 코드: " + ex.getErrorCode());
        assertEquals(ErrorCode.DEPARTMENT_NOT_FOUND, ex.getErrorCode());
    }

    // TC_DP_05_03: 부서 삭제 실패 - 권한 없음
    @Test
    @DisplayName("TC_DP_05_03: 부서 삭제 → 예외 (ACCESS_DENIED)")
    void deleteDepartment_accessDenied() {
        // given
        Long deptId = 402L;
        Organization org = createOrganization(150L, "OrgX");
        Department existing = createDepartment(deptId, "DeptX", org.getOrgId(), org.getOrgName());

        doNothing().when(permissionService).checkPermission("DEPARTMENT_DELETE");
        when(departmentRepository.findById(deptId)).thenReturn(Optional.of(existing));
        securityUtilMock.when(() -> SecurityUtil.hasRole("ROOT")).thenReturn(false);
        securityUtilMock.when(SecurityUtil::getOrgId).thenReturn(999L);

        // when & then
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> departmentService.deleteDepartment(deptId)
        );
        System.out.println("⚠️ TC_DP_05_03 - 예외 코드: " + ex.getErrorCode());
        assertEquals(ErrorCode.ACCESS_DENIED, ex.getErrorCode());
    }

    // --- Helper methods to create sample entities ---
    private Organization createOrganization(Long orgId, String orgName) {
        Organization org = Organization.builder()
                .orgId(orgId)
                .orgName(orgName)
                .build();
        return org;
    }

    private Department createDepartment(Long deptId, String deptName, Long orgId, String orgName) {
        Organization org = createOrganization(orgId, orgName);
        Department dept = Department.builder()
                .deptId(deptId)
                .organization(org)
                .deptName(deptName)
                .build();
        return dept;
    }
}
