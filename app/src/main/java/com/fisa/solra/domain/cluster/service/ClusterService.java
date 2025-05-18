package com.fisa.solra.domain.cluster.service;

import com.fisa.solra.domain.cluster.dto.ClusterRequestDto;
import com.fisa.solra.domain.cluster.dto.ClusterResponseDto;
import com.fisa.solra.domain.cluster.entity.Cluster;
import com.fisa.solra.domain.cluster.repository.ClusterRepository;
import com.fisa.solra.global.config.Fabric8K8sConfig;
import com.fisa.solra.global.exception.BusinessException;
import com.fisa.solra.global.exception.ErrorCode;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClusterService {

    private final Fabric8K8sConfig k8sConfig;
    private final ClusterRepository clusterRepository;

    // ✅ 클러스터 등록 (연결 검증 후 저장)
    public ClusterResponseDto createCluster(ClusterRequestDto dto) {
        // 1) name 중복 체크
        if (clusterRepository.existsByName(dto.getName())) {
            throw new BusinessException(ErrorCode.DUPLICATED_CLUSTER_NAME);
        }
        // 2) (선택) env까지 조합해서 체크
        if (clusterRepository.existsByNameAndEnv(dto.getName(), dto.getEnv())) {
            throw new BusinessException(ErrorCode.DUPLICATED_CLUSTER_NAME);
        }
        // 3) (선택) apiServerUrl 체크
        if (clusterRepository.existsByApiServerUrl(dto.getApiServerUrl())) {
            throw new BusinessException(ErrorCode.CLUSTER_APISERVER_DUPLICATE);
        }

        // 정상적으로 중복이 아니면 Fabric8 연결 검증 후 저장
        KubernetesClient client = k8sConfig.buildClient(dto);
        client.getVersion(); // 연결 테스트

        Cluster saved = clusterRepository.save(dto.toEntity());
        return ClusterResponseDto.fromEntity(saved);
    }
}