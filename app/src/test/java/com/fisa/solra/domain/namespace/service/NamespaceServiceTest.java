package com.fisa.solra.domain.namespace.service;

import com.fisa.solra.domain.namespace.dto.NamespaceRequestDto;
import com.fisa.solra.domain.namespace.dto.NamespaceResponseDto;
import com.fisa.solra.global.config.KubernetesClientProvider;
import com.fisa.solra.global.exception.BusinessException;
import com.fisa.solra.global.exception.ErrorCode;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.DisplayName.class)
class NamespaceServiceTest {

    @InjectMocks
    private NamespaceService namespaceService;

    @Mock
    private KubernetesClientProvider clientProvider;

    @Mock
    private KubernetesClient kubernetesClient;

    @Mock
    private NonNamespaceOperation<Namespace, NamespaceList, Resource<Namespace>> namespaceOperation;

    @Mock
    private Resource<Namespace> namespaceResource;

    private final Long clusterId = 1L;

    @BeforeEach
    void setUp() {
        when(clientProvider.getClient(clusterId)).thenReturn(kubernetesClient);
        when(kubernetesClient.namespaces()).thenReturn(namespaceOperation);
    }

    @Test
    @DisplayName("TC_NS_01_01: 전체 네임스페이스 목록 조회 → 성공")
    void getNamespaces_success() {
        Namespace ns = new NamespaceBuilder()
                .withNewMetadata().withName("test-ns").endMetadata()
                .withNewStatus().withPhase("Active").endStatus()
                .build();

        when(namespaceOperation.list()).thenReturn(new NamespaceListBuilder().withItems(ns).build());

        List<NamespaceResponseDto> result = namespaceService.getNamespaces(clusterId);
        System.out.println("✅ TC_NS_01_01 - 조회된 네임스페이스 수: " + result.size());
        assertEquals(1, result.size());
        assertEquals("test-ns", result.get(0).getName());
    }

    @Test
    @DisplayName("TC_NS_02_01: 단일 네임스페이스 조회 → 성공")
    void getNamespace_success() {
        Namespace ns = new NamespaceBuilder()
                .withNewMetadata().withName("target-ns").endMetadata()
                .withNewStatus().withPhase("Active").endStatus()
                .build();

        when(namespaceOperation.withName("target-ns")).thenReturn(namespaceResource);
        when(namespaceResource.get()).thenReturn(ns);

        NamespaceResponseDto result = namespaceService.getNamespace(clusterId, "target-ns");
        System.out.println("✅ TC_NS_02_01 - 조회된 네임스페이스: " + result.getName());
        assertEquals("target-ns", result.getName());
    }

    @Test
    @DisplayName("TC_NS_02_02: 존재하지 않는 네임스페이스 조회 → 예외 (NAMESPACE_NOT_FOUND)")
    void getNamespace_notFound_throwsException() {
        when(namespaceOperation.withName("not-found")).thenReturn(namespaceResource);
        when(namespaceResource.get()).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                namespaceService.getNamespace(clusterId, "not-found"));

        System.out.println("⚠️ TC_NS_02_02 - 발생한 예외 코드: " + ex.getErrorCode());
        assertEquals(ErrorCode.NAMESPACE_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC_NS_03_01: 네임스페이스 생성 성공")
    void createNamespace_success() {
        NamespaceRequestDto dto = NamespaceRequestDto.builder()
                .name("new-ns")
                .labels(Map.of("team", "dev"))
                .annotations(Map.of("env", "test"))
                .build();

        when(namespaceOperation.withName("new-ns")).thenReturn(namespaceResource);
        when(namespaceResource.get()).thenReturn(null);

        Namespace created = new NamespaceBuilder()
                .withNewMetadata().withName("new-ns").endMetadata()
                .withNewStatus().withPhase("Active").endStatus()
                .build();

        when(namespaceOperation.resource(any(Namespace.class))).thenReturn(namespaceResource);
        when(namespaceResource.create()).thenReturn(created);

        NamespaceResponseDto result = namespaceService.createNamespace(clusterId, dto);
        System.out.println("✅ TC_NS_03_01 - 생성된 네임스페이스: " + result.getName());
        assertNotNull(result);
    }

    @Test
    @DisplayName("TC_NS_03_02: 중복 이름 → 예외 (DUPLICATED_NAMESPACE_NAME)")
    void createNamespace_duplicateName_throwsException() {
        NamespaceRequestDto dto = NamespaceRequestDto.builder()
                .name("duplicate")
                .labels(Map.of())
                .annotations(Map.of())
                .build();

        when(namespaceOperation.withName("duplicate")).thenReturn(namespaceResource);
        when(namespaceResource.get()).thenReturn(new Namespace());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                namespaceService.createNamespace(clusterId, dto));

        System.out.println("⚠️ TC_NS_03_02 - 예외 코드: " + ex.getErrorCode());
        assertEquals(ErrorCode.DUPLICATED_NAMESPACE_NAME, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC_NS_03_03: 네임스페이스 생성 실패 → 예외 (NAMESPACE_CREATION_FAILED)")
    void createNamespace_createFails_throwsException() {
        NamespaceRequestDto dto = NamespaceRequestDto.builder()
                .name("fail-ns")
                .labels(Map.of())
                .annotations(Map.of())
                .build();

        when(namespaceOperation.withName("fail-ns")).thenReturn(namespaceResource);
        when(namespaceResource.get()).thenReturn(null);
        when(namespaceOperation.resource(any(Namespace.class))).thenReturn(namespaceResource);
        when(namespaceResource.create()).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                namespaceService.createNamespace(clusterId, dto));

        System.out.println("⚠️ TC_NS_03_03 - 예외 코드: " + ex.getErrorCode());
        assertEquals(ErrorCode.NAMESPACE_CREATION_FAILED, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC_NS_04_01: 네임스페이스 수정 성공")
    void updateNamespace_success() {
        String name = "editable";

        Namespace existing = new NamespaceBuilder()
                .withNewMetadata().withName(name)
                .withLabels(Map.of("old", "value"))
                .withAnnotations(Map.of("oldAnno", "v"))
                .endMetadata()
                .withNewStatus().withPhase("Active").endStatus()
                .build();

        Namespace updated = new NamespaceBuilder()
                .withNewMetadata().withName(name)
                .withLabels(Map.of("new", "label"))
                .withAnnotations(Map.of("env", "prod"))
                .endMetadata()
                .withNewStatus().withPhase("Active").endStatus()
                .build();

        NamespaceRequestDto dto = NamespaceRequestDto.builder()
                .labels(Map.of("new", "label"))
                .annotations(Map.of("env", "prod"))
                .build();

        when(namespaceOperation.withName(name)).thenReturn(namespaceResource);
        when(namespaceResource.get()).thenReturn(existing);
        when(namespaceResource.edit(any(UnaryOperator.class))).thenReturn(updated);

        NamespaceResponseDto result = namespaceService.updateNamespace(clusterId, name, dto);
        System.out.println("✅ TC_NS_04_01 - 수정된 네임스페이스: " + result.getName());
        assertEquals(name, result.getName());
    }

    @Test
    @DisplayName("TC_NS_04_02: 존재하지 않는 네임스페이스 수정 → 예외 (NAMESPACE_NOT_FOUND)")
    void updateNamespace_notFound_throwsException() {
        String name = "not-found";
        NamespaceRequestDto dto = NamespaceRequestDto.builder()
                .labels(Map.of("new", "label"))
                .annotations(Map.of("env", "prod"))
                .build();

        when(namespaceOperation.withName(name)).thenReturn(namespaceResource);
        when(namespaceResource.get()).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                namespaceService.updateNamespace(clusterId, name, dto));

        System.out.println("⚠️ TC_NS_04_02 - 예외 코드: " + ex.getErrorCode());
        assertEquals(ErrorCode.NAMESPACE_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC_NS_04_03: 수정 대상 없음 → 예외 (NAMESPACE_UPDATE_NO_CHANGE)")
    void updateNamespace_noChange_throwsException() {
        String name = "no-change";

        Namespace existing = new NamespaceBuilder()
                .withNewMetadata().withName(name)
                .withLabels(Map.of("team", "dev"))
                .withAnnotations(Map.of("env", "test"))
                .endMetadata()
                .withNewStatus().withPhase("Active").endStatus()
                .build();

        NamespaceRequestDto dto = NamespaceRequestDto.builder()
                .labels(Map.of("team", "dev"))
                .annotations(Map.of("env", "test"))
                .build();

        when(namespaceOperation.withName(name)).thenReturn(namespaceResource);
        when(namespaceResource.get()).thenReturn(existing);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                namespaceService.updateNamespace(clusterId, name, dto));

        System.out.println("⚠️ TC_NS_04_03 - 예외 코드: " + ex.getErrorCode());
        assertEquals(ErrorCode.NAMESPACE_UPDATE_NO_CHANGE, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC_NS_04_04: 네임스페이스 수정 실패 → 예외 (NAMESPACE_UPDATE_FAILED)")
    void updateNamespace_editFails_throwsException() {
        String name = "fail-edit";

        Namespace existing = new NamespaceBuilder()
                .withNewMetadata().withName(name).endMetadata()
                .withNewStatus().withPhase("Active").endStatus()
                .build();

        NamespaceRequestDto dto = NamespaceRequestDto.builder()
                .labels(Map.of("new", "label"))
                .annotations(Map.of("env", "prod"))
                .build();

        when(namespaceOperation.withName(name)).thenReturn(namespaceResource);
        when(namespaceResource.get()).thenReturn(existing);
        when(namespaceResource.edit(any(UnaryOperator.class))).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                namespaceService.updateNamespace(clusterId, name, dto));

        System.out.println("⚠️ TC_NS_04_04 - 예외 코드: " + ex.getErrorCode());
        assertEquals(ErrorCode.NAMESPACE_UPDATE_FAILED, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC_NS_05_01: 네임스페이스 삭제 성공")
    void deleteNamespace_success() {
        Namespace ns = new NamespaceBuilder()
                .withNewMetadata().withName("to-delete").endMetadata()
                .withNewStatus().withPhase("Active").endStatus()
                .build();

        when(namespaceOperation.withName("to-delete")).thenReturn(namespaceResource);
        when(namespaceResource.get()).thenReturn(ns);
        when(namespaceOperation.resource(any(Namespace.class))).thenReturn(namespaceResource);
        when(namespaceResource.delete()).thenReturn(List.of(new StatusDetails()));

        namespaceService.deleteNamespace(clusterId, "to-delete");

        System.out.println("✅ TC_NS_05_01 - 네임스페이스 삭제 완료");
        verify(namespaceResource, times(1)).delete();
    }

    @Test
    @DisplayName("TC_NS_05_02: 네임스페이스 없음 → 예외 (NAMESPACE_NOT_FOUND)")
    void deleteNamespace_notFound_throwsException() {
        when(namespaceOperation.withName("not-found")).thenReturn(namespaceResource);
        when(namespaceResource.get()).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                namespaceService.deleteNamespace(clusterId, "not-found"));

        System.out.println("⚠️ TC_NS_05_02 - 예외 코드: " + ex.getErrorCode());
        assertEquals(ErrorCode.NAMESPACE_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC_NS_05_03: 삭제 실패 → 예외 (NAMESPACE_DELETION_FAILED)")
    void deleteNamespace_failed_throwsException() {
        Namespace ns = new NamespaceBuilder()
                .withNewMetadata().withName("to-delete").endMetadata()
                .withNewStatus().withPhase("Active").endStatus()
                .build();

        when(namespaceOperation.withName("to-delete")).thenReturn(namespaceResource);
        when(namespaceResource.get()).thenReturn(ns);
        when(namespaceOperation.resource(any(Namespace.class))).thenReturn(namespaceResource);
        when(namespaceResource.delete()).thenReturn(Collections.emptyList());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                namespaceService.deleteNamespace(clusterId, "to-delete"));

        System.out.println("⚠️ TC_NS_05_03 - 예외 코드: " + ex.getErrorCode());
        assertEquals(ErrorCode.NAMESPACE_DELETION_FAILED, ex.getErrorCode());
    }
}
