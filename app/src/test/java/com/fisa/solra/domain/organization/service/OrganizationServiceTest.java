// src/test/java/com/fisa/solra/domain/organization/service/OrganizationServiceTest.java
package com.fisa.solra.domain.organization.service;

import com.fisa.solra.domain.organization.dto.OrganizationResponseDto;
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
import org.springframework.dao.DataIntegrityViolationException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.DisplayName.class)
class OrganizationServiceTest {

    @InjectMocks
    private OrganizationService organizationService;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private PermissionService permissionService;

    private MockedStatic<SecurityUtil> securityUtilMock;

    @BeforeEach
    void init() {
        securityUtilMock = mockStatic(SecurityUtil.class);
    }

    @AfterEach
    void tearDown() {
        securityUtilMock.close();
    }

    // TC_ORG_01_01
    @Test
    @DisplayName("TC_ORG_01_01: 조직 생성 성공")
    void createOrganization_success() {
        String orgName = "New Org";
        Organization saved = Organization.builder().orgId(1L).orgName(orgName).build();

        when(organizationRepository.existsByOrgName(orgName)).thenReturn(false);
        when(organizationRepository.save(any(Organization.class))).thenReturn(saved);

        OrganizationResponseDto result = organizationService.createOrganization(orgName);
        System.out.println("✅ TC_ORG_01_01 - 생성된 조직 ID: " + result.getOrgId());
        assertEquals(1L, result.getOrgId());
        assertEquals(orgName, result.getOrgName());
    }

    // TC_ORG_01_02
    @Test
    @DisplayName("TC_ORG_01_02: orgName이 null 또는 빈 문자열 → 예외 (INVALID_INPUT)")
    void createOrganization_invalidInput() {
        BusinessException ex1 = assertThrows(BusinessException.class, () ->
                organizationService.createOrganization(null));
        System.out.println("⚠️ TC_ORG_01_02a - 예외 코드: " + ex1.getErrorCode());
        assertEquals(ErrorCode.INVALID_INPUT, ex1.getErrorCode());

        BusinessException ex2 = assertThrows(BusinessException.class, () ->
                organizationService.createOrganization("   "));
        System.out.println("⚠️ TC_ORG_01_02b - 예외 코드: " + ex2.getErrorCode());
        assertEquals(ErrorCode.INVALID_INPUT, ex2.getErrorCode());
    }

    // TC_ORG_01_03
    @Test
    @DisplayName("TC_ORG_01_03: 중복 orgName → 예외 (DUPLICATED_ORGANIZATION_NAME)")
    void createOrganization_duplicateName() {
        String orgName = "Existing Org";
        when(organizationRepository.existsByOrgName(orgName)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                organizationService.createOrganization(orgName));
        System.out.println("⚠️ TC_ORG_01_03 - 예외 코드: " + ex.getErrorCode());
        assertEquals(ErrorCode.DUPLICATED_ORGANIZATION_NAME, ex.getErrorCode());
    }

    // TC_ORG_01_04
    @Test
    @DisplayName("TC_ORG_01_04: DataIntegrityViolationException 발생 → 예외 (ORGANIZATION_CREATE_FAILED)")
    void createOrganization_dataIntegrityViolation() {
        String orgName = "Valid Org";
        when(organizationRepository.existsByOrgName(orgName)).thenReturn(false);
        when(organizationRepository.save(any(Organization.class)))
                .thenThrow(new DataIntegrityViolationException("DB error"));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                organizationService.createOrganization(orgName));
        System.out.println("⚠️ TC_ORG_01_04 - 예외 코드: " + ex.getErrorCode());
        assertEquals(ErrorCode.ORGANIZATION_CREATE_FAILED, ex.getErrorCode());
    }

    // TC_ORG_02_01
    @Test
    @DisplayName("TC_ORG_02_01: 조직 전체 조회 성공")
    void getAllOrganizations_success() {
        Organization o1 = Organization.builder().orgId(1L).orgName("OrgA").build();
        Organization o2 = Organization.builder().orgId(2L).orgName("OrgB").build();
        when(organizationRepository.findAll()).thenReturn(List.of(o1, o2));

        List<OrganizationResponseDto> result = organizationService.getAllOrganizations();
        System.out.println("✅ TC_ORG_02_01 - 조회된 조직 수: " + result.size());
        assertEquals(2, result.size());
        assertEquals("OrgA", result.get(0).getOrgName());
        assertEquals("OrgB", result.get(1).getOrgName());
    }

    // TC_ORG_02_02
    @Test
    @DisplayName("TC_ORG_02_02: 조직이 하나도 없어서 빈 리스트 반환 시 → 예외 (ORGANIZATION_NOT_FOUND)")
    void getAllOrganizations_notFound() {
        when(organizationRepository.findAll()).thenReturn(Collections.emptyList());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                organizationService.getAllOrganizations());
        System.out.println("⚠️ TC_ORG_02_02 - 예외 코드: " + ex.getErrorCode());
        assertEquals(ErrorCode.ORGANIZATION_NOT_FOUND, ex.getErrorCode());
    }

    // TC_ORG_03_01
    @Test
    @DisplayName("TC_ORG_03_01: 조직 단일 조회 성공 (ROOT 또는 동일 orgId)")
    void getOrganizationById_success() {
        Long orgId = 5L;
        Organization org = Organization.builder().orgId(orgId).orgName("OrgX").build();
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));
        // ROOT 권한
        securityUtilMock.when(() -> SecurityUtil.hasRole("ROOT")).thenReturn(true);
        // permissionService 호출 무조건 통과
        doNothing().when(permissionService).checkPermission("ORGANIZATION_READ");

        OrganizationResponseDto result = organizationService.getOrganizationById(orgId);
        System.out.println("✅ TC_ORG_03_01 - 조회된 조직 이름: " + result.getOrgName());
        assertEquals("OrgX", result.getOrgName());
    }

    // TC_ORG_03_02
    @Test
    @DisplayName("TC_ORG_03_02: 존재하지 않는 orgId 조회 → 예외 (ORGANIZATION_NOT_FOUND)")
    void getOrganizationById_notFound() {
        Long orgId = 10L;
        when(organizationRepository.findById(orgId)).thenReturn(Optional.empty());
        doNothing().when(permissionService).checkPermission("ORGANIZATION_READ");
        securityUtilMock.when(() -> SecurityUtil.hasRole("ROOT")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                organizationService.getOrganizationById(orgId));
        System.out.println("⚠️ TC_ORG_03_02 - 예외 코드: " + ex.getErrorCode());
        assertEquals(ErrorCode.ORGANIZATION_NOT_FOUND, ex.getErrorCode());
    }

    // TC_ORG_03_03
    @Test
    @DisplayName("TC_ORG_03_03: 권한 없는 조직 조회 → 예외 (ACCESS_DENIED)")
    void getOrganizationById_accessDenied() {
        Long orgId = 20L;
        Organization org = Organization.builder().orgId(orgId).orgName("OrgY").build();
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));
        doNothing().when(permissionService).checkPermission("ORGANIZATION_READ");
        // ROOT 아닌 상태에서 SecurityUtil.getOrgId와 조회 id 불일치
        securityUtilMock.when(() -> SecurityUtil.hasRole("ROOT")).thenReturn(false);
        securityUtilMock.when(SecurityUtil::getOrgId).thenReturn(999L);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                organizationService.getOrganizationById(orgId));
        System.out.println("⚠️ TC_ORG_03_03 - 예외 코드: " + ex.getErrorCode());
        assertEquals(ErrorCode.ACCESS_DENIED, ex.getErrorCode());
    }

    // TC_ORG_04_01
    @Test
    @DisplayName("TC_ORG_04_01: 조직명 수정 성공 (ROOT 또는 동일 orgId)")
    void updateOrganizationName_success() {
        Long orgId = 7L;
        Organization org = Organization.builder().orgId(orgId).orgName("OldName").build();
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));
        doNothing().when(permissionService).checkPermission("ORGANIZATION_UPDATE");

        // ROOT 권한
        securityUtilMock.when(() -> SecurityUtil.hasRole("ROOT")).thenReturn(true);

        OrganizationResponseDto result = organizationService.updateOrganizationName(orgId, "NewName");
        System.out.println("✅ TC_ORG_04_01 - 수정된 조직 이름: " + result.getOrgName());
        assertEquals("NewName", result.getOrgName());
    }

    // TC_ORG_04_02
    @Test
    @DisplayName("TC_ORG_04_02: 수정할 조직 없음 → 예외 (ORGANIZATION_NOT_FOUND)")
    void updateOrganizationName_notFound() {
        Long orgId = 30L;
        when(organizationRepository.findById(orgId)).thenReturn(Optional.empty());
        doNothing().when(permissionService).checkPermission("ORGANIZATION_UPDATE");
        securityUtilMock.when(() -> SecurityUtil.hasRole("ROOT")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                organizationService.updateOrganizationName(orgId, "Name"));
        System.out.println("⚠️ TC_ORG_04_02 - 예외 코드: " + ex.getErrorCode());
        assertEquals(ErrorCode.ORGANIZATION_NOT_FOUND, ex.getErrorCode());
    }

    // TC_ORG_04_03
    @Test
    @DisplayName("TC_ORG_04_03: 권한 없는 조직 수정 → 예외 (ACCESS_DENIED)")
    void updateOrganizationName_accessDenied() {
        Long orgId = 40L;
        Organization org = Organization.builder().orgId(orgId).orgName("OrgZ").build();
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));
        doNothing().when(permissionService).checkPermission("ORGANIZATION_UPDATE");
        securityUtilMock.when(() -> SecurityUtil.hasRole("ROOT")).thenReturn(false);
        securityUtilMock.when(SecurityUtil::getOrgId).thenReturn(777L);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                organizationService.updateOrganizationName(orgId, "NewZ"));
        System.out.println("⚠️ TC_ORG_04_03 - 예외 코드: " + ex.getErrorCode());
        assertEquals(ErrorCode.ACCESS_DENIED, ex.getErrorCode());
    }

    // TC_ORG_05_01
    @Test
    @DisplayName("TC_ORG_05_01: 조직 삭제 성공 (ROOT 또는 동일 orgId)")
    void deleteOrganization_success() {
        Long orgId = 50L;
        Organization org = Organization.builder().orgId(orgId).orgName("DelOrg").build();
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));
        doNothing().when(permissionService).checkPermission("ORGANIZATION_DELETE");
        securityUtilMock.when(() -> SecurityUtil.hasRole("ROOT")).thenReturn(true);

        // 호출 시 예외 발생 없이 deleteById가 호출되어야 함
        organizationService.deleteOrganization(orgId);
        System.out.println("✅ TC_ORG_05_01 - 조직 삭제 호출됨");
        verify(organizationRepository, times(1)).deleteById(orgId);
    }

    // TC_ORG_05_02
    @Test
    @DisplayName("TC_ORG_05_02: 삭제할 조직 없음 → 예외 (ORGANIZATION_NOT_FOUND)")
    void deleteOrganization_notFound() {
        Long orgId = 60L;
        when(organizationRepository.findById(orgId)).thenReturn(Optional.empty());
        doNothing().when(permissionService).checkPermission("ORGANIZATION_DELETE");
        securityUtilMock.when(() -> SecurityUtil.hasRole("ROOT")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                organizationService.deleteOrganization(orgId));
        System.out.println("⚠️ TC_ORG_05_02 - 예외 코드: " + ex.getErrorCode());
        assertEquals(ErrorCode.ORGANIZATION_NOT_FOUND, ex.getErrorCode());
    }

    // TC_ORG_05_03
    @Test
    @DisplayName("TC_ORG_05_03: 권한 없는 조직 삭제 → 예외 (ACCESS_DENIED)")
    void deleteOrganization_accessDenied() {
        Long orgId = 70L;
        Organization org = Organization.builder().orgId(orgId).orgName("CannotDel").build();
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));
        doNothing().when(permissionService).checkPermission("ORGANIZATION_DELETE");
        securityUtilMock.when(() -> SecurityUtil.hasRole("ROOT")).thenReturn(false);
        securityUtilMock.when(SecurityUtil::getOrgId).thenReturn(123L);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                organizationService.deleteOrganization(orgId));
        System.out.println("⚠️ TC_ORG_05_03 - 예외 코드: " + ex.getErrorCode());
        assertEquals(ErrorCode.ACCESS_DENIED, ex.getErrorCode());
    }
}
