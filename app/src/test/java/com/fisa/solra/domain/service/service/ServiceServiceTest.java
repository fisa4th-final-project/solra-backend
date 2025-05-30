package com.fisa.solra.domain.service.service;

import com.fisa.solra.domain.service.dto.ServiceRequestDto;
import com.fisa.solra.domain.service.dto.ServiceResponseDto;
import com.fisa.solra.global.config.KubernetesClientProvider;
import com.fisa.solra.global.exception.BusinessException;
import com.fisa.solra.global.exception.ErrorCode;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.ServiceListBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NamespaceableResource;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.DisplayName.class)
class ServiceServiceTest {

    @InjectMocks
    private ServiceService serviceService;

    @Mock
    private KubernetesClientProvider clientProvider;

    @Mock
    private KubernetesClient kubernetesClient;

    // namespace 조회
    @Mock
    private NonNamespaceOperation<Namespace, NamespaceList, Resource<Namespace>> namespaceOperation;
    @Mock
    private Resource<Namespace> namespaceResource;

    // service 조회/생성/수정
    @Mock
    private MixedOperation<
            io.fabric8.kubernetes.api.model.Service,
            KubernetesResourceList<io.fabric8.kubernetes.api.model.Service>,
            Resource<io.fabric8.kubernetes.api.model.Service>
            > serviceOperation;
    @Mock
    private Resource<io.fabric8.kubernetes.api.model.Service> serviceResource;

    // 삭제용
    @Mock
    private NamespaceableResource<io.fabric8.kubernetes.api.model.Service> deletionResource;

    private final Long clusterId = 1L;
    private final String namespace = "test-ns";

    @BeforeEach
    void setUp() {
        when(clientProvider.getClient(clusterId)).thenReturn(kubernetesClient);

        // 네임스페이스
        when(kubernetesClient.namespaces()).thenReturn(namespaceOperation);
        when(namespaceOperation.withName(namespace)).thenReturn(namespaceResource);

        // Service 리소스
        when(kubernetesClient.resources(io.fabric8.kubernetes.api.model.Service.class))
                .thenReturn(serviceOperation);
        when(serviceOperation.inNamespace(namespace)).thenReturn(serviceOperation);
    }

    // TC_SV_01_01
    @Test
    @DisplayName("TC_SV_01_01: 전체 서비스 목록 조회 → 성공")
    void getServices_success() {
        // given: namespace exists
        when(namespaceResource.get()).thenReturn(new NamespaceBuilder()
                .withNewMetadata().withName(namespace).endMetadata()
                .build());

        // and: one Service with non-null spec
        io.fabric8.kubernetes.api.model.Service svc = new io.fabric8.kubernetes.api.model.ServiceBuilder()
                .withNewMetadata().withName("svc1").endMetadata()
                .withNewSpec()
                .withType("ClusterIP")
                .addNewPort()
                .withName("http")
                .withPort(80)
                .withTargetPort(new IntOrString(8080))
                .withProtocol("TCP")
                .endPort()
                .endSpec()
                .build();

        when(serviceOperation.list())
                .thenReturn(new ServiceListBuilder().withItems(svc).build());

        // when
        List<ServiceResponseDto> result = serviceService.getServices(clusterId, namespace);

        // then
        System.out.println("✅ TC_SV_01_01 - 조회된 서비스 수: " + result.size());
        assertEquals(1, result.size());
        assertEquals("svc1", result.get(0).getName());
    }

    // TC_SV_01_02
    @Test
    @DisplayName("TC_SV_01_02: 네임스페이스 없음 → 예외 (NAMESPACE_NOT_FOUND)")
    void getServices_namespaceNotFound() {
        when(namespaceResource.get()).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> serviceService.getServices(clusterId, namespace));

        System.out.println("⚠️ TC_SV_01_02 - 예외 코드: " + ex.getErrorCode());
        assertEquals(ErrorCode.NAMESPACE_NOT_FOUND, ex.getErrorCode());
    }

    // TC_SV_02_01
    @Test
    @DisplayName("TC_SV_02_01: 단일 서비스 조회 → 성공")
    void getService_success() {
        io.fabric8.kubernetes.api.model.Service svc = new io.fabric8.kubernetes.api.model.ServiceBuilder()
                .withNewMetadata().withName("svcA").endMetadata()
                .withNewSpec().endSpec()
                .build();

        when(serviceOperation.withName("svcA")).thenReturn(serviceResource);
        when(serviceResource.get()).thenReturn(svc);

        ServiceResponseDto result = serviceService.getService(clusterId, namespace, "svcA");

        System.out.println("✅ TC_SV_02_01 - 조회된 서비스: " + result.getName());
        assertEquals("svcA", result.getName());
    }

    // TC_SV_02_02
    @Test
    @DisplayName("TC_SV_02_02: 서비스 없음 → 예외 (SERVICE_NOT_FOUND)")
    void getService_notFound() {
        when(serviceOperation.withName("no-svc")).thenReturn(serviceResource);
        when(serviceResource.get()).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> serviceService.getService(clusterId, namespace, "no-svc"));

        System.out.println("⚠️ TC_SV_02_02 - 예외 코드: " + ex.getErrorCode());
        assertEquals(ErrorCode.SERVICE_NOT_FOUND, ex.getErrorCode());
    }

    // TC_SV_03_01
    @Test
    @DisplayName("TC_SV_03_01: 서비스 생성 → 성공")
    void createService_success() {
        ServiceRequestDto.Port p = ServiceRequestDto.Port.builder()
                .name("p1").port(80).targetPort(8080).protocol("TCP").nodePort(30080).build();
        ServiceRequestDto dto = ServiceRequestDto.builder()
                .name("new-svc").type("NodePort")
                .selector(Map.of("app","x")).ports(List.of(p)).build();

        when(namespaceResource.get()).thenReturn(new NamespaceBuilder()
                .withNewMetadata().withName(namespace).endMetadata()
                .build());
        when(serviceOperation.withName("new-svc")).thenReturn(serviceResource);
        when(serviceResource.get()).thenReturn(null);
        when(serviceOperation.resource(any())).thenReturn(serviceResource);
        when(serviceResource.create()).thenReturn(new io.fabric8.kubernetes.api.model.ServiceBuilder()
                .withNewMetadata().withName("new-svc").endMetadata()
                .withNewSpec().endSpec()
                .build()
        );

        ServiceResponseDto result =
                serviceService.createService(clusterId, namespace, dto);

        System.out.println("✅ TC_SV_03_01 - 생성된 서비스: " + result.getName());
        assertEquals("new-svc", result.getName());
    }

    // TC_SV_03_02
    @Test
    @DisplayName("TC_SV_03_02: 네임스페이스 없음 → 예외 (NAMESPACE_NOT_FOUND)")
    void createService_nsNotFound() {
        ServiceRequestDto dto = ServiceRequestDto.builder().name("s").ports(List.of()).build();
        when(namespaceResource.get()).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> serviceService.createService(clusterId, namespace, dto));

        System.out.println("⚠️ TC_SV_03_02 - 예외 코드: " + ex.getErrorCode());
        assertEquals(ErrorCode.NAMESPACE_NOT_FOUND, ex.getErrorCode());
    }

    // TC_SV_03_03
    @Test
    @DisplayName("TC_SV_03_03: 이름 중복 → 예외 (DUPLICATED_SERVICE_NAME)")
    void createService_duplicateName() {
        ServiceRequestDto dto = ServiceRequestDto.builder().name("dup").ports(List.of()).build();
        when(namespaceResource.get()).thenReturn(new Namespace());
        when(serviceOperation.withName("dup")).thenReturn(serviceResource);
        when(serviceResource.get()).thenReturn(new io.fabric8.kubernetes.api.model.Service());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> serviceService.createService(clusterId, namespace, dto));

        System.out.println("⚠️ TC_SV_03_03 - 예외 코드: " + ex.getErrorCode());
        assertEquals(ErrorCode.DUPLICATED_SERVICE_NAME, ex.getErrorCode());
    }

    // TC_SV_03_04
    @Test
    @DisplayName("TC_SV_03_04: 생성 실패 → 예외 (SERVICE_CREATION_FAILED)")
    void createService_creationFails() {
        ServiceRequestDto dto = ServiceRequestDto.builder().name("fail").ports(List.of()).build();
        when(namespaceResource.get()).thenReturn(new Namespace());
        when(serviceOperation.withName("fail")).thenReturn(serviceResource);
        when(serviceResource.get()).thenReturn(null);
        when(serviceOperation.resource(any())).thenReturn(serviceResource);
        when(serviceResource.create()).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> serviceService.createService(clusterId, namespace, dto));

        System.out.println("⚠️ TC_SV_03_04 - 예외 코드: " + ex.getErrorCode());
        assertEquals(ErrorCode.SERVICE_CREATION_FAILED, ex.getErrorCode());
    }

    // TC_SV_04_01
    @Test
    @DisplayName("TC_SV_04_01: 서비스 수정 → 성공")
    void updateService_success() {
        ServiceRequestDto.Port p = ServiceRequestDto.Port.builder()
                .name("p").port(80).targetPort(8080).protocol("TCP").nodePort(30080).build();
        ServiceRequestDto dto = ServiceRequestDto.builder()
                .selector(Map.of("app","y")).type(null).ports(List.of(p)).build();

        io.fabric8.kubernetes.api.model.Service existing =
                new io.fabric8.kubernetes.api.model.ServiceBuilder()
                        .withNewMetadata().withName("svc").endMetadata()
                        .withNewSpec().addNewPort().withNodePort(30000).endPort().endSpec()
                        .build();
        io.fabric8.kubernetes.api.model.Service updated =
                new io.fabric8.kubernetes.api.model.ServiceBuilder(existing)
                        .editSpec().withType("ClusterIP").endSpec()
                        .build();

        when(serviceOperation.withName("svc")).thenReturn(serviceResource);
        when(serviceResource.get()).thenReturn(existing);
        when(serviceOperation.list())
                .thenReturn(new ServiceListBuilder().withItems(existing).build());
        when(serviceResource.edit(any(UnaryOperator.class))).thenReturn(updated);

        ServiceResponseDto result =
                serviceService.updateService(clusterId, namespace, "svc", dto);

        System.out.println("✅ TC_SV_04_01 - 수정된 서비스 타입: " + result.getType());
        assertEquals("ClusterIP", result.getType());
    }

    // TC_SV_04_02
    @Test
    @DisplayName("TC_SV_04_02: 수정할 서비스 없음 → 예외 (SERVICE_NOT_FOUND)")
    void updateService_notFound() {
        ServiceRequestDto dto = ServiceRequestDto.builder().ports(List.of()).build();
        when(serviceOperation.withName("svc")).thenReturn(serviceResource);
        when(serviceResource.get()).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> serviceService.updateService(clusterId, namespace, "svc", dto));

        System.out.println("⚠️ TC_SV_04_02 - 예외 코드: " + ex.getErrorCode());
        assertEquals(ErrorCode.SERVICE_NOT_FOUND, ex.getErrorCode());
    }

    // TC_SV_04_03
    @Test
    @DisplayName("TC_SV_04_03: NodePort 충돌 → 예외 (POD_NODEPORT_CONFLICT)")
    void updateService_portConflict() {
        ServiceRequestDto.Port p = ServiceRequestDto.Port.builder()
                .name("p").port(80).targetPort(8080).protocol("TCP").nodePort(30080).build();
        ServiceRequestDto dto = ServiceRequestDto.builder().ports(List.of(p)).build();

        io.fabric8.kubernetes.api.model.Service existing =
                new io.fabric8.kubernetes.api.model.ServiceBuilder()
                        .withNewMetadata().withName("svc").endMetadata()
                        .withNewSpec().addNewPort().withNodePort(30080).endPort().endSpec()
                        .build();

        when(serviceOperation.withName("svc")).thenReturn(serviceResource);
        when(serviceResource.get()).thenReturn(existing);
        when(serviceOperation.list())
                .thenReturn(new ServiceListBuilder().withItems(existing).build());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> serviceService.updateService(clusterId, namespace, "svc", dto));

        System.out.println("⚠️ TC_SV_04_03 - 예외 코드: " + ex.getErrorCode());
        assertEquals(ErrorCode.POD_NODEPORT_CONFLICT, ex.getErrorCode());
    }

    // TC_SV_04_04
    @Test
    @DisplayName("TC_SV_04_04: 수정 실패 → 예외 (SERVICE_UPDATE_FAILED)")
    void updateService_editFails() {
        ServiceRequestDto dto = ServiceRequestDto.builder().ports(List.of()).build();
        io.fabric8.kubernetes.api.model.Service existing =
                new io.fabric8.kubernetes.api.model.ServiceBuilder()
                        .withNewMetadata().withName("svc").endMetadata()
                        .withNewSpec().endSpec()
                        .build();

        when(serviceOperation.withName("svc")).thenReturn(serviceResource);
        when(serviceResource.get()).thenReturn(existing);
        when(serviceOperation.list())
                .thenReturn(new ServiceListBuilder().withItems(existing).build());
        when(serviceResource.edit(any(UnaryOperator.class))).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> serviceService.updateService(clusterId, namespace, "svc", dto));

        System.out.println("⚠️ TC_SV_04_04 - 예외 코드: " + ex.getErrorCode());
        assertEquals(ErrorCode.SERVICE_UPDATE_FAILED, ex.getErrorCode());
    }

    // TC_SV_05_01: 서비스 삭제 → 성공
    @Test
    @DisplayName("TC_SV_05_01: 서비스 삭제 → 성공")
    void deleteService_success() {
        var svc = new io.fabric8.kubernetes.api.model.ServiceBuilder()
                .withNewMetadata().withName("svc").endMetadata()
                .withNewSpec().endSpec()
                .build();

        when(serviceOperation.withName("svc")).thenReturn(serviceResource);
        when(serviceResource.get()).thenReturn(svc);
        // ↓ 여기를 kubernetesClient.resource 가 아니라 serviceOperation.resource 로 바꿉니다.
        when(serviceOperation.resource(svc)).thenReturn(deletionResource);
        when(deletionResource.delete()).thenReturn(List.of(new StatusDetails()));

        serviceService.deleteService(clusterId, namespace, "svc");

        System.out.println("✅ TC_SV_05_01 - 서비스 삭제 성공");
        verify(deletionResource).delete();
    }

    // TC_SV_05_02
    @Test
    @DisplayName("TC_SV_05_02: 삭제할 서비스 없음 → 예외 (SERVICE_NOT_FOUND)")
    void deleteService_notFound() {
        when(serviceOperation.withName("no")).thenReturn(serviceResource);
        when(serviceResource.get()).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> serviceService.deleteService(clusterId, namespace, "no"));

        System.out.println("⚠️ TC_SV_05_02 - 예외 코드: " + ex.getErrorCode());
        assertEquals(ErrorCode.SERVICE_NOT_FOUND, ex.getErrorCode());
    }

    // TC_SV_05_03: 삭제 실패 → 예외 (SERVICE_DELETION_FAILED)
    @Test
    @DisplayName("TC_SV_05_03: 삭제 실패 → 예외 (SERVICE_DELETION_FAILED)")
    void deleteService_deletionFails() {
        var svc = new io.fabric8.kubernetes.api.model.ServiceBuilder()
                .withNewMetadata().withName("svc").endMetadata()
                .withNewSpec().endSpec()
                .build();

        when(serviceOperation.withName("svc")).thenReturn(serviceResource);
        when(serviceResource.get()).thenReturn(svc);
        // ↓ 여기 역시 serviceOperation.resource 로 stub
        when(serviceOperation.resource(svc)).thenReturn(deletionResource);
        when(deletionResource.delete()).thenReturn(Collections.emptyList());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> serviceService.deleteService(clusterId, namespace, "svc"));

        System.out.println("⚠️ TC_SV_05_03 - 예외 코드: " + ex.getErrorCode());
        assertEquals(ErrorCode.SERVICE_DELETION_FAILED, ex.getErrorCode());
    }

}
