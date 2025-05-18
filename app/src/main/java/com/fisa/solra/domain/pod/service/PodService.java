// src/main/java/com/fisa/solra/domain/pod/service/PodService.java
package com.fisa.solra.domain.pod.service;

import com.fisa.solra.domain.cluster.dto.ClusterRequestDto;
import com.fisa.solra.domain.cluster.entity.Cluster;
import com.fisa.solra.domain.cluster.repository.ClusterRepository;
import com.fisa.solra.domain.pod.dto.PodResponseDto;
import com.fisa.solra.global.config.Fabric8K8sConfig;
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

    // ✅ 파드 전체 조회
    public List<PodResponseDto> getPods(String namespace) {
        KubernetesClient client = getClient();
        return client.pods()
                .inNamespace(namespace)
                .list()
                .getItems()
                .stream()
                .map(PodResponseDto::from)
                .collect(Collectors.toList());
    }

    // ✅ 단일 파드 조회
    public PodResponseDto getPod(String namespace, String name) {
        KubernetesClient client = getClient();
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
