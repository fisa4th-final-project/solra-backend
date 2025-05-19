// src/main/java/com/fisa/solra/domain/namespace/service/NamespaceService.java
package com.fisa.solra.domain.namespace.service;

import com.fisa.solra.domain.cluster.dto.ClusterRequestDto;
import com.fisa.solra.domain.cluster.entity.Cluster;
import com.fisa.solra.domain.cluster.repository.ClusterRepository;
import com.fisa.solra.domain.namespace.dto.NamespaceRequestDto;
import com.fisa.solra.domain.namespace.dto.NamespaceResponseDto;
import com.fisa.solra.global.config.Fabric8K8sConfig;
import com.fisa.solra.global.config.KubernetesClientProvider;
import com.fisa.solra.global.exception.BusinessException;
import com.fisa.solra.global.exception.ErrorCode;
import io.fabric8.kubernetes.api.model.StatusDetails;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NamespaceService {

    private final KubernetesClientProvider clientProvider;

    // ✅ 전체 네임스페이스 조회
    public List<NamespaceResponseDto> getNamespaces(Long clusterId) {
        KubernetesClient client = clientProvider.getClient(clusterId);
        return client.namespaces()
                .list().getItems().stream()
                .map(ns -> NamespaceResponseDto.from(clusterId, ns))
                .collect(Collectors.toList());
    }

    // ✅ 단일 네임스페이스 상세 조회
    public NamespaceResponseDto getNamespace(Long clusterId, String name) {
        KubernetesClient client = clientProvider.getClient(clusterId);
        Namespace ns = client.namespaces().withName(name).get();
        if (ns == null) throw new BusinessException(ErrorCode.NAMESPACE_NOT_FOUND);
        return NamespaceResponseDto.from(clusterId, ns);
    }

    // ✅ 네임스페이스 생성
    public NamespaceResponseDto createNamespace(Long clusterId, NamespaceRequestDto dto) {
        KubernetesClient client = clientProvider.getClient(clusterId);

        boolean exists = client.namespaces()
                .withName(dto.getName()).get() != null;
        if (exists) throw new BusinessException(ErrorCode.DUPLICATED_NAMESPACE_NAME);

        Namespace built = new NamespaceBuilder()
                .withNewMetadata()
                .withName(dto.getName())
                .addToLabels(dto.getLabels())
                .addToAnnotations(dto.getAnnotations())
                .endMetadata()
                .build();

        Namespace created = client.namespaces()
                .resource(built).create();
        if (created == null) throw new BusinessException(ErrorCode.NAMESPACE_CREATION_FAILED);

        return NamespaceResponseDto.from(clusterId, created);
    }

    // ✅ 네임스페이스 수정 (라벨/어노테이션만)
    public NamespaceResponseDto updateNamespace(
            Long clusterId,
            String name,
            NamespaceRequestDto dto) {

        KubernetesClient client = clientProvider.getClient(clusterId);
        Namespace current = client.namespaces().withName(name).get();
        if (current == null) throw new BusinessException(ErrorCode.NAMESPACE_NOT_FOUND);

        var meta = current.getMetadata();
        var oldLabels = meta.getLabels();
        var oldAnnos = meta.getAnnotations();
        var newLabels = dto.getLabels();
        var newAnnos = dto.getAnnotations();

        boolean labelsUnchanged = newLabels.entrySet().stream()
                .allMatch(e -> e.getValue().equals(oldLabels.get(e.getKey())));
        boolean annosUnchanged = newAnnos.entrySet().stream()
                .allMatch(e -> e.getValue().equals(oldAnnos.get(e.getKey())));

        if (labelsUnchanged && annosUnchanged) {
            throw new BusinessException(ErrorCode.NAMESPACE_UPDATE_NO_CHANGE);
        }

        Namespace updated = client.namespaces()
                .withName(name)
                .edit(n -> new NamespaceBuilder(n)
                        .editMetadata()
                        .withLabels(newLabels)
                        .withAnnotations(newAnnos)
                        .endMetadata()
                        .build()
                );
        if (updated == null) throw new BusinessException(ErrorCode.NAMESPACE_UPDATE_FAILED);

        return NamespaceResponseDto.from(clusterId, updated);
    }

    // ✅ 네임스페이스 삭제
    public void deleteNamespace(Long clusterId, String name) {
        KubernetesClient client = clientProvider.getClient(clusterId);
        Namespace current = client.namespaces().withName(name).get();
        if (current == null) throw new BusinessException(ErrorCode.NAMESPACE_NOT_FOUND);

        List<StatusDetails> status = client.namespaces()
                .resource(current)
                .delete();
        if (status == null || status.isEmpty()) {
            throw new BusinessException(ErrorCode.NAMESPACE_DELETION_FAILED);
        }
    }
}