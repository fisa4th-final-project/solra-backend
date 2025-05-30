package com.fisa.solra.domain.deployment.service;

import com.fisa.solra.domain.deployment.dto.DeploymentCreateRequestDto;
import com.fisa.solra.domain.deployment.dto.DeploymentCreateResponseDto;
import com.fisa.solra.domain.deployment.dto.DeploymentRequestDto;
import com.fisa.solra.domain.deployment.dto.DeploymentResponseDto;
import com.fisa.solra.global.config.KubernetesClientProvider;
import com.fisa.solra.global.exception.BusinessException;
import com.fisa.solra.global.exception.ErrorCode;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.NamespaceList;
import io.fabric8.kubernetes.api.model.StatusDetails;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentList;
import io.fabric8.kubernetes.api.model.apps.DeploymentListBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.DisplayName.class)
class DeploymentServiceTest {

    @InjectMocks
    private DeploymentService deploymentService;

    @Mock
    private KubernetesClientProvider clientProvider;

    @Mock
    private KubernetesClient kubernetesClient;

    @Mock
    private AppsAPIGroupDSL appsGroup;

    @Mock
    private MixedOperation<Deployment, DeploymentList, RollableScalableResource<Deployment>> deploymentOperation;

    @Mock
    private RollableScalableResource<Deployment> deploymentResource;

    @Mock
    private MixedOperation<Namespace, NamespaceList, Resource<Namespace>> namespaceOperation;

    @Mock
    private Resource<Namespace> namespaceResource;

    private final Long clusterId = 1L;
    private final String namespace = "dev";

    private Deployment existingDeployment;
    private Deployment updatedDeployment;
    private DeploymentRequestDto updateDto;

    @Mock
    private NamespaceableResource<Deployment> deletionResource;

    @BeforeEach
    void setUp() {

        // clientProvider
        when(clientProvider.getClient(clusterId)).thenReturn(kubernetesClient);

        // apps().deployments()
        lenient().when(kubernetesClient.apps()).thenReturn(appsGroup);
        lenient().when(appsGroup.deployments()).thenReturn(deploymentOperation);
        // namespaces()
        lenient().when(kubernetesClient.namespaces()).thenReturn(namespaceOperation);
        lenient().when(namespaceOperation.withName(namespace)).thenReturn(namespaceResource);
        lenient().when(namespaceResource.get()).thenReturn(
                new NamespaceBuilder()
                        .withNewMetadata().withName(namespace).endMetadata()
                        .withNewStatus().withPhase("Active").endStatus()
                        .build()
        );
        lenient().when(kubernetesClient.resource(existingDeployment))
                .thenReturn(deletionResource);

        // prepare existing & updated deployments
        existingDeployment = new DeploymentBuilder()
                .withNewMetadata().withName("target-deploy").endMetadata()
                .withNewSpec().withReplicas(1).endSpec()
                .build();
        updatedDeployment = new DeploymentBuilder()
                .withNewMetadata().withName("target-deploy").endMetadata()
                .withNewSpec().withReplicas(2).endSpec()
                .build();

        // prepare update DTO
        updateDto = DeploymentRequestDto.builder()
                .replicas(2)
                .container(new DeploymentRequestDto.ContainerDto(null, null, null))
                .build();
    }

    @Test
    @DisplayName("TC_DP_01_01: 전체 디플로이먼트 목록 조회 성공")
    void TC_DP_01_01() {
        Deployment dp = new DeploymentBuilder()
                .withNewMetadata().withName("sample-deploy").endMetadata()
                .build();

        when(deploymentOperation.inNamespace(namespace)).thenReturn(deploymentOperation);
        when(deploymentOperation.list())
                .thenReturn(new DeploymentListBuilder()
                        .withItems(Collections.singletonList(dp))
                        .build()
                );

        List<DeploymentResponseDto> result = deploymentService.getDeployments(clusterId, namespace);

        System.out.println("✅ TC_DP_01_01 - 디플로이먼트 수: " + result.size());
        assertEquals(1, result.size());
        assertEquals("sample-deploy", result.get(0).getName());
    }

    @Test
    @DisplayName("TC_DP_01_02: 네임스페이스 없음 → 예외 (NAMESPACE_NOT_FOUND)")
    void TC_DP_01_02() {
        when(namespaceResource.get()).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> deploymentService.getDeployments(clusterId, namespace)
        );
        System.out.println("⚠️ TC_DP_01_02 - 예외 코드: " + ex.getErrorCode());
        assertEquals(ErrorCode.NAMESPACE_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC_DP_02_01: 단일 디플로이먼트 조회 성공")
    void TC_DP_02_01() {
        when(deploymentOperation.inNamespace(namespace)).thenReturn(deploymentOperation);
        when(deploymentOperation.withName("target-deploy")).thenReturn(deploymentResource);
        when(deploymentResource.get()).thenReturn(existingDeployment);

        DeploymentResponseDto result = deploymentService.getDeployment(clusterId, namespace, "target-deploy");
        System.out.println("✅ TC_DP_02_01 - 조회된 디플로이먼트: " + result.getName());
        assertEquals("target-deploy", result.getName());
    }

    @Test
    @DisplayName("TC_DP_02_02: 디플로이먼트 없음 → 예외 (DEPLOYMENT_NOT_FOUND)")
    void TC_DP_02_02() {
        when(deploymentOperation.inNamespace(namespace)).thenReturn(deploymentOperation);
        when(deploymentOperation.withName("missing")).thenReturn(deploymentResource);
        when(deploymentResource.get()).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> deploymentService.getDeployment(clusterId, namespace, "missing")
        );
        System.out.println("⚠️ TC_DP_02_02 - 예외 코드: " + ex.getErrorCode());
        assertEquals(ErrorCode.DEPLOYMENT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC_DP_03_01: 디플로이먼트 생성 성공")
    void TC_DP_03_01() {
        var dto = DeploymentCreateRequestDto.builder()
                .name("new-deploy")
                .replicas(1)
                .labels(Map.of("app","web"))
                .container(new DeploymentCreateRequestDto.ContainerDto("nginx","nginx:latest",80))
                .build();

        when(deploymentOperation.inNamespace(namespace)).thenReturn(deploymentOperation);
        when(deploymentOperation.withName("new-deploy")).thenReturn(deploymentResource);
        when(deploymentResource.get()).thenReturn(null);
        when(deploymentOperation.resource(any(Deployment.class))).thenReturn(deploymentResource);
        when(deploymentResource.create()).thenReturn(new Deployment());

        DeploymentCreateResponseDto result = deploymentService.createDeployment(clusterId, namespace, dto);
        System.out.println("✅ TC_DP_03_01 - 생성된 디플로이먼트: " + result.getName());
        assertEquals("new-deploy", result.getName());
    }

    @Test
    @DisplayName("TC_DP_03_02: 디플로이먼트 이름 중복 → 예외 (DUPLICATED_DEPLOYMENT_NAME)")
    void TC_DP_03_02() {
        var dto = DeploymentCreateRequestDto.builder()
                .name("dup")
                .replicas(1)
                .labels(Map.of())
                .container(new DeploymentCreateRequestDto.ContainerDto("c","i",1))
                .build();

        when(deploymentOperation.inNamespace(namespace)).thenReturn(deploymentOperation);
        when(deploymentOperation.withName("dup")).thenReturn(deploymentResource);
        when(deploymentResource.get()).thenReturn(new Deployment());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> deploymentService.createDeployment(clusterId, namespace, dto)
        );
        System.out.println("⚠️ TC_DP_03_02 - 예외 코드: " + ex.getErrorCode());
        assertEquals(ErrorCode.DUPLICATED_DEPLOYMENT_NAME, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC_DP_03_03: 디플로이먼트 생성 실패 → 예외 (DEPLOYMENT_CREATION_FAILED)")
    void TC_DP_03_03() {
        var dto = DeploymentCreateRequestDto.builder()
                .name("fail")
                .replicas(1)
                .labels(Map.of())
                .container(new DeploymentCreateRequestDto.ContainerDto("c","i",1))
                .build();

        when(deploymentOperation.inNamespace(namespace)).thenReturn(deploymentOperation);
        when(deploymentOperation.withName("fail")).thenReturn(deploymentResource);
        when(deploymentResource.get()).thenReturn(null);
        when(deploymentOperation.resource(any(Deployment.class))).thenReturn(deploymentResource);
        when(deploymentResource.create()).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> deploymentService.createDeployment(clusterId, namespace, dto)
        );
        System.out.println("⚠️ TC_DP_03_03 - 예외 코드: " + ex.getErrorCode());
        assertEquals(ErrorCode.DEPLOYMENT_CREATION_FAILED, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC_DP_04_01: 디플로이먼트 업데이트 성공")
    void TC_DP_04_01() {
        when(deploymentOperation.inNamespace(namespace)).thenReturn(deploymentOperation);
        when(deploymentOperation.withName("target-deploy")).thenReturn(deploymentResource);
        when(deploymentResource.get()).thenReturn(existingDeployment);
        when(deploymentResource.edit(any(UnaryOperator.class))).thenReturn(updatedDeployment);

        DeploymentResponseDto result = deploymentService.updateDeployment(
                clusterId, namespace, "target-deploy", updateDto);
        System.out.println("✅ TC_DP_04_01 - 업데이트된 디플로이먼트 레플리카: " + result.getReplicas());
        assertEquals(2, result.getReplicas());
    }

    @Test
    @DisplayName("TC_DP_04_02: 디플로이먼트 없음 → 예외 (DEPLOYMENT_NOT_FOUND)")
    void TC_DP_04_02() {
        when(deploymentOperation.inNamespace(namespace)).thenReturn(deploymentOperation);
        when(deploymentOperation.withName("target-deploy")).thenReturn(deploymentResource);
        when(deploymentResource.get()).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> deploymentService.updateDeployment(
                        clusterId, namespace, "target-deploy", updateDto));
        System.out.println("⚠️ TC_DP_04_02 - 예외 코드: " + ex.getErrorCode());
        assertEquals(ErrorCode.DEPLOYMENT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC_DP_04_03: 디플로이먼트 업데이트 중 에러 → 예외 (DEPLOYMENT_UPDATE_FAILED)")
    void TC_DP_04_03() {
        String name = "target-deploy";

        // 1) 존재 여부 확인을 위해 get()은 non-null 반환
        when(deploymentOperation.inNamespace(namespace)).thenReturn(deploymentOperation);
        when(deploymentOperation.withName(name)).thenReturn(deploymentResource);
        when(deploymentResource.get()).thenReturn(existingDeployment);

        // 2) edit() 결과를 null로 만들어서 서비스 로직이 DEPLOYMENT_UPDATE_FAILED를 던지게 함
        when(deploymentResource.edit(any(UnaryOperator.class)))
                .thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> deploymentService.updateDeployment(clusterId, namespace, name, updateDto)
        );

        System.out.println("⚠️ TC_DP_04_03 - 발생한 예외 코드: " + ex.getErrorCode());
        assertEquals(ErrorCode.DEPLOYMENT_UPDATE_FAILED, ex.getErrorCode());
    }


    // 3) TC_DP_05_01 에서 deletionResource.delete() 를 stub
    @Test
    @DisplayName("TC_DP_05_01: 디플로이먼트 삭제 성공")
    void TC_DP_05_01() {
        String name = "target-deploy";

        // 1) 기존 조회 stub
        when(appsGroup.deployments()).thenReturn(deploymentOperation);
        when(deploymentOperation.inNamespace(namespace)).thenReturn(deploymentOperation);
        when(deploymentOperation.withName(name)).thenReturn(deploymentResource);
        when(deploymentResource.get()).thenReturn(existingDeployment);

        // 2) kubernetesClient.resource(existingDeployment) → deletionResource 리턴
        when(kubernetesClient.resource(existingDeployment))
                .thenReturn(deletionResource);

        // 3) delete() → 성공 리스트 반환
        when(deletionResource.delete())
                .thenReturn(List.of(new StatusDetails()));

        // 4) 호출
        deploymentService.deleteDeployment(clusterId, namespace, name);

        System.out.println("✅ TC_DP_05_01 - 디플로이먼트 삭제 성공");
        verify(deletionResource).delete();
    }

    @Test
    @DisplayName("TC_DP_05_02: 디플로이먼트 없음 → 예외 (DEPLOYMENT_NOT_FOUND)")
    void TC_DP_05_02() {
        when(deploymentOperation.inNamespace(namespace)).thenReturn(deploymentOperation);
        when(deploymentOperation.withName("target-deploy")).thenReturn(deploymentResource);
        when(deploymentResource.get()).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> deploymentService.deleteDeployment(clusterId, namespace, "target-deploy"));
        System.out.println("⚠️ TC_DP_05_02 - 예외 코드: " + ex.getErrorCode());
        assertEquals(ErrorCode.DEPLOYMENT_NOT_FOUND, ex.getErrorCode());
    }

    // 4) TC_DP_05_03 에서는 빈 리스트를 반환하도록 stub
    @Test
    @DisplayName("TC_DP_05_03: 디플로이먼트 삭제 실패 → 예외 (DEPLOYMENT_DELETION_FAILED)")
    void TC_DP_05_03() {
        String name = "target-deploy";

        // 1) 기존 조회 stub
        when(appsGroup.deployments()).thenReturn(deploymentOperation);
        when(deploymentOperation.inNamespace(namespace)).thenReturn(deploymentOperation);
        when(deploymentOperation.withName(name)).thenReturn(deploymentResource);
        when(deploymentResource.get()).thenReturn(existingDeployment);

        // 2) kubernetesClient.resource(existingDeployment) → deletionResource 리턴
        when(kubernetesClient.resource(existingDeployment))
                .thenReturn(deletionResource);

        // 3) delete() → 빈 리스트 반환해서 실패 시나리오
        when(deletionResource.delete())
                .thenReturn(Collections.emptyList());

        // 4) BusinessException 발생 검증
        BusinessException ex = assertThrows(BusinessException.class,
                () -> deploymentService.deleteDeployment(clusterId, namespace, name)
        );

        System.out.println("⚠️ TC_DP_05_03 - 예외 코드: " + ex.getErrorCode());
        assertEquals(ErrorCode.DEPLOYMENT_DELETION_FAILED, ex.getErrorCode());
    }
}
