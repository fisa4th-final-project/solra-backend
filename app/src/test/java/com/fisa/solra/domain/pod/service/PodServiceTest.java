package com.fisa.solra.domain.pod.service;

import com.fisa.solra.domain.pod.dto.PodResponseDto;
import com.fisa.solra.global.config.KubernetesClientProvider;
import com.fisa.solra.global.exception.BusinessException;
import com.fisa.solra.global.exception.ErrorCode;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.PodListBuilder;
import io.fabric8.kubernetes.api.model.PodSpecBuilder;
import io.fabric8.kubernetes.api.model.PodStatusBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.PodResource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.DisplayName.class)
class PodServiceTest {

    @InjectMocks
    private PodService podService;

    @Mock
    private KubernetesClientProvider clientProvider;

    @Mock
    private KubernetesClient kubernetesClient;

    @Mock
    private MixedOperation<Pod, PodList, PodResource> podOperation;

    @Mock
    private PodResource podResource;

    private final Long clusterId = 1L;
    private final String namespace = "test-ns";

    @BeforeEach
    void setUp() {
        when(clientProvider.getClient(clusterId)).thenReturn(kubernetesClient);
        when(kubernetesClient.pods()).thenReturn(podOperation);
        when(podOperation.inNamespace(namespace)).thenReturn(podOperation);
    }

    @Test
    @DisplayName("TC_POD_01_01: 전체 파드 목록 조회 → 성공")
    void getPods_success() {
        Pod pod = new PodBuilder()
                .withNewMetadata().withName("pod1").endMetadata()
                // spec을 반드시 포함해야 getSpec()이 null이 아님
                .withSpec(new PodSpecBuilder().withNodeName("nodeA").build())
                .withStatus(new PodStatusBuilder().withPhase("Running").build())
                .build();

        when(podOperation.list())
                .thenReturn(new PodListBuilder().withItems(pod).build());

        List<PodResponseDto> result = podService.getPods(clusterId, namespace);
        System.out.println("✅ TC_POD_01_01 - 조회된 파드 수: " + result.size());
        assertEquals(1, result.size());
        assertEquals("pod1", result.get(0).getName());
        assertEquals("Running", result.get(0).getPhase());
        assertEquals("nodeA", result.get(0).getNodeName());
    }

    @Test
    @DisplayName("TC_POD_02_01: 단일 파드 조회 → 성공")
    void getPod_success() {
        Pod pod = new PodBuilder()
                .withNewMetadata().withName("podA").endMetadata()
                .withSpec(new PodSpecBuilder().withNodeName("nodeB").build())
                .withStatus(new PodStatusBuilder().withPhase("Pending").build())
                .build();

        when(podOperation.withName("podA")).thenReturn(podResource);
        when(podResource.get()).thenReturn(pod);

        PodResponseDto result = podService.getPod(clusterId, namespace, "podA");
        System.out.println("✅ TC_POD_02_01 - 조회된 파드: " +
                result.getName() + ", phase=" + result.getPhase() + ", node=" + result.getNodeName());
        assertEquals("podA", result.getName());
        assertEquals("Pending", result.getPhase());
        assertEquals("nodeB", result.getNodeName());
    }

    @Test
    @DisplayName("TC_POD_02_02: 존재하지 않는 파드 조회 → 예외 (POD_NOT_FOUND)")
    void getPod_notFound() {
        when(podOperation.withName("no-pod")).thenReturn(podResource);
        when(podResource.get()).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> podService.getPod(clusterId, namespace, "no-pod"));
        System.out.println("⚠️ TC_POD_02_02 - 예외 코드: " + ex.getErrorCode());
        assertEquals(ErrorCode.POD_NOT_FOUND, ex.getErrorCode());
    }
}
