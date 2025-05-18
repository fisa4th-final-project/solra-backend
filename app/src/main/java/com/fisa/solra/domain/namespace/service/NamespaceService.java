// src/main/java/com/fisa/solra/domain/namespace/service/NamespaceService.java
package com.fisa.solra.domain.namespace.service;

import com.fisa.solra.domain.cluster.dto.ClusterRequestDto;
import com.fisa.solra.domain.cluster.entity.Cluster;
import com.fisa.solra.domain.cluster.repository.ClusterRepository;
import com.fisa.solra.domain.namespace.dto.NamespaceRequestDto;
import com.fisa.solra.domain.namespace.dto.NamespaceResponseDto;
import com.fisa.solra.global.config.Fabric8K8sConfig;
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

    private final ClusterRepository clusterRepository;
    private final Fabric8K8sConfig k8sConfig;

    /**
     * DB에서 등록된 첫 클러스터 메타를 조회하여
     * KubernetesClient를 생성합니다.
     */
    private KubernetesClient getClient() {
        Cluster cluster = clusterRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.CLUSTER_NOT_FOUND));
        ClusterRequestDto dto = ClusterRequestDto.fromEntity(cluster);
        return k8sConfig.buildClient(dto);
    }

    // ✅ 전체 네임스페이스 조회
    public List<NamespaceResponseDto> getNamespaces() {
        KubernetesClient client = getClient();
        return client.namespaces()
                .list()
                .getItems()
                .stream()
                .map(NamespaceResponseDto::from)
                .collect(Collectors.toList());
    }

    // ✅ 단일 네임스페이스 상세 조회
    public NamespaceResponseDto getNamespace(String name) {
        KubernetesClient client = getClient();
        Namespace ns = client.namespaces()
                .withName(name)
                .get();
        if (ns == null) {
            throw new BusinessException(ErrorCode.NAMESPACE_NOT_FOUND);
        }
        return NamespaceResponseDto.from(ns);
    }

    // ✅ 네임스페이스 생성
    public NamespaceResponseDto createNamespace(NamespaceRequestDto dto) {
        KubernetesClient client = getClient();
        boolean exists = client.namespaces()
                .withName(dto.getName())
                .get() != null;
        if (exists) {
            throw new BusinessException(ErrorCode.DUPLICATED_NAMESPACE_NAME);
        }
        Namespace built = new NamespaceBuilder()
                .withNewMetadata()
                .withName(dto.getName())
                .addToLabels(dto.getLabels())
                .addToAnnotations(dto.getAnnotations())
                .endMetadata()
                .build();
        Namespace created = client.namespaces()
                .resource(built)
                .create();
        if (created == null) {
            throw new BusinessException(ErrorCode.NAMESPACE_CREATION_FAILED);
        }
        return NamespaceResponseDto.from(created);
    }

    // ✅ 네임스페이스 수정 (라벨/어노테이션만)
    public NamespaceResponseDto updateNamespace(String name, NamespaceRequestDto dto) {
        KubernetesClient client = getClient();
        Namespace current = client.namespaces()
                .withName(name)
                .get();
        if (current == null) {
            throw new BusinessException(ErrorCode.NAMESPACE_NOT_FOUND);
        }
        var meta = current.getMetadata();
        var oldLabels = meta.getLabels();
        var oldAnnos  = meta.getAnnotations();
        var newLabels = dto.getLabels();
        var newAnnos  = dto.getAnnotations();
        boolean labelsUnchanged = newLabels.entrySet().stream()
                .allMatch(e -> e.getValue().equals(oldLabels.get(e.getKey())));
        boolean annosUnchanged  = newAnnos.entrySet().stream()
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
                        .build());
        if (updated == null) {
            throw new BusinessException(ErrorCode.NAMESPACE_UPDATE_FAILED);
        }
        return NamespaceResponseDto.from(updated);
    }

    // ✅ 네임스페이스 삭제
    public void deleteNamespace(String name) {
        KubernetesClient client = getClient();
        Namespace current = client.namespaces()
                .withName(name)
                .get();
        if (current == null) {
            throw new BusinessException(ErrorCode.NAMESPACE_NOT_FOUND);
        }
        List<StatusDetails> status = client.namespaces()
                .resource(current)
                .delete();
        if (status == null || status.isEmpty()) {
            throw new BusinessException(ErrorCode.NAMESPACE_DELETION_FAILED);
        }
    }
}
