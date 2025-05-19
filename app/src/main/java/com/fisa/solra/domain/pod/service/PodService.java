// src/main/java/com/fisa/solra/domain/pod/service/PodService.java
package com.fisa.solra.domain.pod.service;

import com.fisa.solra.domain.cluster.dto.ClusterRequestDto;
import com.fisa.solra.domain.cluster.entity.Cluster;
import com.fisa.solra.domain.cluster.repository.ClusterRepository;
import com.fisa.solra.domain.pod.dto.PodResponseDto;
import com.fisa.solra.global.config.Fabric8K8sConfig;
import com.fisa.solra.global.config.KubernetesClientProvider;
import com.fisa.solra.global.exception.BusinessException;
import com.fisa.solra.global.exception.ErrorCode;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PodService {

    private final KubernetesClientProvider clientProvider;

    // ✅ 파드 전체 조회
    public List<PodResponseDto> getPods(Long clusterId, String namespace) {
        KubernetesClient client = clientProvider.getClient(clusterId);
        return client.pods()
                .inNamespace(namespace)
                .list()
                .getItems()
                .stream()
                .map(PodResponseDto::from)
                .collect(Collectors.toList());
    }

    // ✅ 단일 파드 조회
    public PodResponseDto getPod(Long clusterId, String namespace, String name) {
        KubernetesClient client = clientProvider.getClient(clusterId);
        Pod pod = client.pods()
                .inNamespace(namespace)
                .withName(name)
                .get();
        if (pod == null) {
            throw new BusinessException(ErrorCode.POD_NOT_FOUND);
        }
        return PodResponseDto.from(pod);
    }
}