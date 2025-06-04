package com.fisa.solra.domain.cluster.service;

import com.fisa.solra.domain.cluster.dto.ClusterRequestDto;
import com.fisa.solra.domain.cluster.entity.Cluster;
import com.fisa.solra.domain.cluster.repository.ClusterRepository;
import com.fisa.solra.domain.organization.entity.Organization;
import com.fisa.solra.domain.organization.repository.OrganizationRepository;
import com.fisa.solra.global.config.Fabric8K8sConfig;
import com.fisa.solra.global.exception.BusinessException;
import com.fisa.solra.global.util.SecurityUtil;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.DisplayName.class) // ✅ TC 순서대로 실행
class ClusterServiceTest {

    @InjectMocks
    private ClusterService clusterService;

    @Mock
    private ClusterRepository clusterRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private Fabric8K8sConfig k8sConfig;

    private MockedStatic<SecurityUtil> securityUtilMock;

    @BeforeEach
    void init() {
        securityUtilMock = mockStatic(SecurityUtil.class);
    }

    @AfterEach
    void tearDown() {
        securityUtilMock.close();
    }

    // TC_CL_01_01
    @Test
    @DisplayName("TC_CL_01_01: ROOT 사용자가 모든 클러스터 조회 시도 → 성공")
    void getClusters_asRootUser_success() {
        securityUtilMock.when(() -> SecurityUtil.hasRole("ROOT")).thenReturn(true);
        securityUtilMock.when(SecurityUtil::getOrgId).thenReturn(1L);

        List<Cluster> mockList = List.of(
                createCluster("cluster1", 1L),
                createCluster("cluster2", 2L)
        );
        when(clusterRepository.findAll()).thenReturn(mockList);

        var result = clusterService.getClusters(null);

        System.out.println("✅TC_CL_01_01 - 조회된 클러스터 수: " + result.size());
        result.forEach(c -> System.out.println(" - " + c.getName()));

        assertEquals(2, result.size());
    }

    // TC_CL_01_02
    @Test
    @DisplayName("TC_CL_01_02: 일반 사용자가 자신의 조직 클러스터 조회 → 성공")
    void getClusters_asOrgUser_ownOrg_success() {
        securityUtilMock.when(() -> SecurityUtil.hasRole("ROOT")).thenReturn(false);
        securityUtilMock.when(SecurityUtil::getOrgId).thenReturn(1L);

        List<Cluster> mockList = List.of(createCluster("cluster1", 1L));
        when(clusterRepository.findByOrganization_OrgId(1L)).thenReturn(mockList);

        var result = clusterService.getClusters(1L);

        System.out.println("✅TC_CL_01_02 -  조직 1의 클러스터 수: " + result.size());
        assertEquals(1, result.size());
    }

    // TC_CL_01_03
    @Test
    @DisplayName("TC_CL_01_03: 일반 사용자가 다른 조직 클러스터 조회 시도 → ACCESS_DENIED 예외")
    void getClusters_asOrgUser_otherOrg_throwsAccessDenied() {
        securityUtilMock.when(() -> SecurityUtil.hasRole("ROOT")).thenReturn(false);
        securityUtilMock.when(SecurityUtil::getOrgId).thenReturn(1L);

        Long 요청한_다른_조직_ID = 2L;
        BusinessException ex = assertThrows(BusinessException.class,
                () -> clusterService.getClusters(요청한_다른_조직_ID));

        System.out.println("⚠️TC_CL_01_03 -  발생한 예외 코드: " + ex.getErrorCode());
        assertEquals("ACCESS_DENIED", ex.getErrorCode().name());
    }

    // TC_CL_02_01
    @Test
    @DisplayName("TC_CL_02_01: 클러스터가 존재하고 권한이 맞으면 → 성공")
    void getCluster_existsAndHasPermission_success() {
        Long clusterId = 1L;
        Long orgId = 100L;

        Organization org = Organization.builder().orgId(orgId).orgName("Org1").build();
        Cluster cluster = Cluster.builder()
                .name("clusterA")
                .apiServerUrl("https://a")
                .env("dev")
                .caCert("ca")
                .saToken("token")
                .organization(org)
                .build();

        when(clusterRepository.findById(clusterId)).thenReturn(Optional.of(cluster));
        securityUtilMock.when(() -> SecurityUtil.hasRole("ROOT")).thenReturn(false);
        securityUtilMock.when(SecurityUtil::getOrgId).thenReturn(orgId);

        var result = clusterService.getCluster(clusterId);

        System.out.println("✅TC_CL_02_01 - 조회된 클러스터 이름: " + result.getName());
        assertEquals("clusterA", result.getName());
    }

    // TC_CL_02_02
    @Test
    @DisplayName("TC_CL_02_02: 클러스터가 없으면 → 예외 발생 (CLUSTER_NOT_FOUND)")
    void getCluster_notExist_throwsClusterNotFound() {
        Long clusterId = 999L;
        when(clusterRepository.findById(clusterId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> clusterService.getCluster(clusterId));

        System.out.println("⚠️TC_CL_02_02 - 발생한 예외 코드: " + ex.getErrorCode());
        assertEquals("CLUSTER_NOT_FOUND", ex.getErrorCode().name());
    }

    // TC_CL_02_03
    @Test
    @DisplayName("TC_CL_02_03: 권한이 없는 조직의 클러스터 → 예외 발생 (ACCESS_DENIED)")
    void getCluster_differentOrgUser_throwsAccessDenied() {
        Long clusterId = 2L;

        Organization org = Organization.builder().orgId(200L).orgName("OtherOrg").build();
        Cluster cluster = Cluster.builder()
                .name("otherCluster")
                .apiServerUrl("https://b")
                .env("prod")
                .caCert("ca")
                .saToken("token")
                .organization(org)
                .build();

        when(clusterRepository.findById(clusterId)).thenReturn(Optional.of(cluster));
        securityUtilMock.when(() -> SecurityUtil.hasRole("ROOT")).thenReturn(false);
        securityUtilMock.when(SecurityUtil::getOrgId).thenReturn(100L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> clusterService.getCluster(clusterId));

        System.out.println("⚠️TC_CL_02_03 - 발생한 예외 코드: " + ex.getErrorCode());
        assertEquals("ACCESS_DENIED", ex.getErrorCode().name());
    }

    // 🔧 클러스터 생성 도우미
    private Cluster createCluster(String name, Long orgId) {
        Organization org = Organization.builder()
                .orgId(orgId)
                .orgName("Org" + orgId)
                .build();

        return Cluster.builder()
                .name(name)
                .apiServerUrl("https://api" + orgId)
                .env("dev")
                .caCert("ca")
                .saToken("token")
                .organization(org)
                .build();
    }

    // ✅ TC_CL_03_01: 정상 등록 시 → 성공
    @Test
    @DisplayName("TC_CL_03_01: 정상 등록 시 → 성공")
    void createCluster_success() {
        // given
        ClusterRequestDto dto = ClusterRequestDto.builder()
                .name("new-cluster")
                .apiServerUrl("https://api.new")
                .orgId(1L)
                .env("dev")
                .caCert("ca")
                .saToken("token")
                .build();

        Organization org = Organization.builder()
                .orgId(1L)
                .orgName("Org1")
                .build();

        Cluster savedCluster = dto.toEntity(org);

        when(clusterRepository.existsByName(dto.getName())).thenReturn(false);
        when(clusterRepository.existsByApiServerUrl(dto.getApiServerUrl())).thenReturn(false);
        when(organizationRepository.findById(dto.getOrgId())).thenReturn(Optional.of(org));
        when(clusterRepository.save(any())).thenReturn(savedCluster);

        // when
        var result = clusterService.createCluster(dto);

        // then
        System.out.println("✅ TC_CL_03_01 - 생성된 클러스터 이름: " + result.getName());
        assertEquals("new-cluster", result.getName());
    }

    // ⚠️ TC_CL_03_02: 클러스터 이름 중복 → 예외 발생 (DUPLICATED_CLUSTER_NAME)
    @Test
    @DisplayName("TC_CL_03_02: 클러스터 이름 중복 → 예외 발생 (DUPLICATED_CLUSTER_NAME)")
    void createCluster_nameDuplicate_throwsException() {
        // given
        ClusterRequestDto dto = ClusterRequestDto.builder()
                .name("duplicate")
                .apiServerUrl("https://api.dup")
                .orgId(1L)
                .env("dev")
                .caCert("ca")
                .saToken("token")
                .build();

        when(clusterRepository.existsByName("duplicate")).thenReturn(true);

        // when & then
        BusinessException ex = assertThrows(BusinessException.class, () -> clusterService.createCluster(dto));
        System.out.println("⚠️ TC_CL_03_02 - 발생한 예외 코드: " + ex.getErrorCode());
        assertEquals("DUPLICATED_CLUSTER_NAME", ex.getErrorCode().name());
    }

    // ⚠️ TC_CL_03_03: API 서버 URL 중복 → 예외 발생 (CLUSTER_APISERVER_DUPLICATE)
    @Test
    @DisplayName("TC_CL_03_03: API 서버 URL 중복 → 예외 발생 (CLUSTER_APISERVER_DUPLICATE)")
    void createCluster_apiUrlDuplicate_throwsException() {
        // given
        ClusterRequestDto dto = ClusterRequestDto.builder()
                .name("unique-name")
                .apiServerUrl("https://api.dup")
                .orgId(1L)
                .env("dev")
                .caCert("ca")
                .saToken("token")
                .build();

        when(clusterRepository.existsByName("unique-name")).thenReturn(false);
        when(clusterRepository.existsByApiServerUrl("https://api.dup")).thenReturn(true);

        // when & then
        BusinessException ex = assertThrows(BusinessException.class, () -> clusterService.createCluster(dto));
        System.out.println("⚠️ TC_CL_03_03 - 발생한 예외 코드: " + ex.getErrorCode());
        assertEquals("CLUSTER_APISERVER_DUPLICATE", ex.getErrorCode().name());
    }

    // ⚠️ TC_CL_03_04: 존재하지 않는 조직 ID → 예외 발생 (ORGANIZATION_NOT_FOUND)
    @Test
    @DisplayName("TC_CL_03_04: 존재하지 않는 조직 ID → 예외 발생 (ORGANIZATION_NOT_FOUND)")
    void createCluster_orgNotFound_throwsException() {
        // given
        ClusterRequestDto dto = ClusterRequestDto.builder()
                .name("cluster")
                .apiServerUrl("https://api.cluster")
                .orgId(404L)
                .env("dev")
                .caCert("ca")
                .saToken("token")
                .build();

        when(clusterRepository.existsByName(dto.getName())).thenReturn(false);
        when(clusterRepository.existsByApiServerUrl(dto.getApiServerUrl())).thenReturn(false);
        when(organizationRepository.findById(404L)).thenReturn(Optional.empty());

        // when & then
        BusinessException ex = assertThrows(BusinessException.class, () -> clusterService.createCluster(dto));
        System.out.println("⚠️ TC_CL_03_04 - 발생한 예외 코드: " + ex.getErrorCode());
        assertEquals("ORGANIZATION_NOT_FOUND", ex.getErrorCode().name());
    }

    // ✅ TC_CL_04_01: 변경이 있고 권한이 맞을 때 → 성공
    @Test
    @DisplayName("TC_CL_04_01: 변경이 있고 권한이 맞을 때 → 성공")
    void updateCluster_success_withChange() {
        // given
        Long clusterId = 1L;
        Long orgId = 1L;

        Organization org = Organization.builder().orgId(orgId).orgName("Org1").build();

        Cluster existing = Cluster.builder()
                .name("old-name")
                .apiServerUrl("https://old-api")
                .env("dev")
                .caCert("ca")
                .saToken("token")
                .organization(org)
                .build();

        ClusterRequestDto dto = ClusterRequestDto.builder()
                .name("new-name") // 변경
                .apiServerUrl("https://old-api") // 변경 없음
                .orgId(orgId)
                .env("dev")
                .caCert("ca")
                .saToken("token")
                .build();

        when(clusterRepository.findById(clusterId)).thenReturn(Optional.of(existing));
        securityUtilMock.when(() -> SecurityUtil.hasRole("ROOT")).thenReturn(false);
        securityUtilMock.when(SecurityUtil::getOrgId).thenReturn(orgId);
        when(clusterRepository.existsByName("new-name")).thenReturn(false);
        when(clusterRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // when
        var result = clusterService.updateCluster(clusterId, dto);

        // then
        System.out.println("✅ TC_CL_04_01 - 업데이트된 클러스터 이름: " + result.getName());
        assertEquals("new-name", result.getName());
    }

    // ⚠️ TC_CL_04_02: 클러스터 존재하지 않음 → 예외 (CLUSTER_NOT_FOUND)
    @Test
    @DisplayName("TC_CL_04_02: 클러스터 존재하지 않음 → 예외 (CLUSTER_NOT_FOUND)")
    void updateCluster_notFound_throwsException() {
        Long clusterId = 999L;
        when(clusterRepository.findById(clusterId)).thenReturn(Optional.empty());

        ClusterRequestDto dto = ClusterRequestDto.builder().name("new").build();

        BusinessException ex = assertThrows(BusinessException.class, () ->
                clusterService.updateCluster(clusterId, dto));

        System.out.println("⚠️ TC_CL_04_02 - 발생한 예외 코드: " + ex.getErrorCode());
        assertEquals("CLUSTER_NOT_FOUND", ex.getErrorCode().name());
    }

    // ⚠️ TC_CL_04_03: 권한이 없는 사용자 → 예외 (ACCESS_DENIED)
    @Test
    @DisplayName("TC_CL_04_03: 권한이 없는 사용자 → 예외 (ACCESS_DENIED)")
    void updateCluster_wrongOrgUser_throwsAccessDenied() {
        Long clusterId = 1L;

        Organization org = Organization.builder().orgId(200L).orgName("OtherOrg").build();
        Cluster existing = Cluster.builder().name("old").organization(org).build();

        when(clusterRepository.findById(clusterId)).thenReturn(Optional.of(existing));
        securityUtilMock.when(() -> SecurityUtil.hasRole("ROOT")).thenReturn(false);
        securityUtilMock.when(SecurityUtil::getOrgId).thenReturn(100L); // 다른 조직

        ClusterRequestDto dto = ClusterRequestDto.builder().name("new").build();

        BusinessException ex = assertThrows(BusinessException.class, () ->
                clusterService.updateCluster(clusterId, dto));

        System.out.println("⚠️ TC_CL_04_03 - 발생한 예외 코드: " + ex.getErrorCode());
        assertEquals("ACCESS_DENIED", ex.getErrorCode().name());
    }

    // ⚠️ TC_CL_04_04: name 중복 → 예외 (DUPLICATED_CLUSTER_NAME)
    @Test
    @DisplayName("TC_CL_04_04: name 중복 → 예외 (DUPLICATED_CLUSTER_NAME)")
    void updateCluster_duplicateName_throwsException() {
        Long clusterId = 1L;
        Long orgId = 1L;

        Organization org = Organization.builder().orgId(orgId).orgName("Org").build();
        Cluster existing = Cluster.builder().name("original").organization(org).build();

        ClusterRequestDto dto = ClusterRequestDto.builder()
                .name("duplicated-name") // 바뀐 이름 → 중복
                .orgId(orgId)
                .build();

        when(clusterRepository.findById(clusterId)).thenReturn(Optional.of(existing));
        securityUtilMock.when(() -> SecurityUtil.hasRole("ROOT")).thenReturn(false);
        securityUtilMock.when(SecurityUtil::getOrgId).thenReturn(orgId);
        when(clusterRepository.existsByName("duplicated-name")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                clusterService.updateCluster(clusterId, dto));

        System.out.println("⚠️ TC_CL_04_04 - 발생한 예외 코드: " + ex.getErrorCode());
        assertEquals("DUPLICATED_CLUSTER_NAME", ex.getErrorCode().name());
    }

    // ✅ TC_CL_04_05: API 서버 URL 중복 → 예외 (CLUSTER_APISERVER_DUPLICATE)
    @Test
    @DisplayName("TC_CL_04_05: API 서버 URL 중복 → 예외 (CLUSTER_APISERVER_DUPLICATE)")
    void updateCluster_duplicateApiServer_throwsException() {
        Long clusterId = 1L;
        Long orgId = 1L;

        Organization org = Organization.builder().orgId(orgId).orgName("Org").build();
        Cluster existing = Cluster.builder()
                .name("cluster")
                .apiServerUrl("https://old-api")
                .env("dev")
                .caCert("ca")
                .saToken("token")
                .organization(org)
                .build();

        ClusterRequestDto dto = ClusterRequestDto.builder()
                .name("cluster")
                .apiServerUrl("https://dup-api")
                .env("dev")
                .caCert("ca")
                .saToken("token")
                .orgId(orgId)
                .build();

        when(clusterRepository.findById(clusterId)).thenReturn(Optional.of(existing));
        securityUtilMock.when(() -> SecurityUtil.hasRole("ROOT")).thenReturn(false);
        securityUtilMock.when(SecurityUtil::getOrgId).thenReturn(orgId);
        when(clusterRepository.existsByApiServerUrl("https://dup-api")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                clusterService.updateCluster(clusterId, dto));

        System.out.println("⚠️ TC_CL_04_05 - 발생한 예외 코드: " + ex.getErrorCode());
        assertEquals("CLUSTER_APISERVER_DUPLICATE", ex.getErrorCode().name());
    }

    // ✅ TC_CL_04_06: 변경된 필드 없음 → 예외 (CLUSTER_UPDATE_NO_CHANGE)
    @Test
    @DisplayName("TC_CL_04_06: 변경된 필드 없음 → 예외 (CLUSTER_UPDATE_NO_CHANGE)")
    void updateCluster_noChange_throwsException() {
        Long clusterId = 1L;
        Long orgId = 1L;

        Organization org = Organization.builder().orgId(orgId).orgName("Org").build();
        Cluster existing = Cluster.builder()
                .name("same")
                .apiServerUrl("https://api.same")
                .env("dev")
                .caCert("cert")
                .saToken("token")
                .organization(org)
                .build();

        ClusterRequestDto dto = ClusterRequestDto.builder()
                .name("same")
                .apiServerUrl("https://api.same")
                .env("dev")
                .caCert("cert")
                .saToken("token")
                .orgId(orgId)
                .build();

        when(clusterRepository.findById(clusterId)).thenReturn(Optional.of(existing));
        securityUtilMock.when(() -> SecurityUtil.hasRole("ROOT")).thenReturn(false);
        securityUtilMock.when(SecurityUtil::getOrgId).thenReturn(orgId);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                clusterService.updateCluster(clusterId, dto));

        System.out.println("⚠️ TC_CL_04_06 - 발생한 예외 코드: " + ex.getErrorCode());
        assertEquals("CLUSTER_UPDATE_NO_CHANGE", ex.getErrorCode().name());
    }


    // ✅ TC_CL_05_01: 존재하고 권한 맞을 경우 → 성공
    @Test
    @DisplayName("TC_CL_05_01: 존재하고 권한 맞을 경우 → 성공")
    void deleteCluster_success() {
        Long clusterId = 1L;
        Long orgId = 1L;

        Organization org = Organization.builder().orgId(orgId).orgName("Org").build();
        Cluster cluster = Cluster.builder().name("to-be-deleted").organization(org).build();

        when(clusterRepository.findById(clusterId)).thenReturn(Optional.of(cluster));
        securityUtilMock.when(() -> SecurityUtil.hasRole("ROOT")).thenReturn(false);
        securityUtilMock.when(SecurityUtil::getOrgId).thenReturn(orgId);

        // when
        clusterService.delete(clusterId);

        // then
        System.out.println("✅ TC_CL_05_01 - 클러스터 삭제 성공: " + cluster.getName());
        verify(clusterRepository, times(1)).deleteById(clusterId);
    }

    // ⚠️ TC_CL_05_02: 클러스터 없음 → 예외 (CLUSTER_NOT_FOUND)
    @Test
    @DisplayName("TC_CL_05_02: 클러스터 없음 → 예외 (CLUSTER_NOT_FOUND)")
    void deleteCluster_notFound_throwsException() {
        Long clusterId = 999L;
        when(clusterRepository.findById(clusterId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                clusterService.delete(clusterId));

        System.out.println("⚠️ TC_CL_05_02 - 발생한 예외 코드: " + ex.getErrorCode());
        assertEquals("CLUSTER_NOT_FOUND", ex.getErrorCode().name());
    }

    // ⚠️ TC_CL_05_03: 권한 없음 → 예외 (ACCESS_DENIED)
    @Test
    @DisplayName("TC_CL_05_03: 권한 없음 → 예외 (ACCESS_DENIED)")
    void deleteCluster_accessDenied_throwsException() {
        Long clusterId = 1L;

        Organization org = Organization.builder().orgId(200L).orgName("OtherOrg").build();
        Cluster cluster = Cluster.builder().name("no-permission").organization(org).build();

        when(clusterRepository.findById(clusterId)).thenReturn(Optional.of(cluster));
        securityUtilMock.when(() -> SecurityUtil.hasRole("ROOT")).thenReturn(false);
        securityUtilMock.when(SecurityUtil::getOrgId).thenReturn(100L);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                clusterService.delete(clusterId));

        System.out.println("⚠️ TC_CL_05_03 - 발생한 예외 코드: " + ex.getErrorCode());
        assertEquals("ACCESS_DENIED", ex.getErrorCode().name());
    }

}