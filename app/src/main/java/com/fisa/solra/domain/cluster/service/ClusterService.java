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

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClusterService {

    private final Fabric8K8sConfig k8sConfig;
    private final ClusterRepository clusterRepository;

    // ✅ 클러스터 전체 조회
    public List<ClusterResponseDto> getClusters() {
        return clusterRepository.findAll().stream()
                .map(ClusterResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    // ✅ 클러스터 단일 조회
    public ClusterResponseDto getCluster(Long clusterId) {
        Cluster c = clusterRepository.findById(clusterId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CLUSTER_NOT_FOUND));
        return ClusterResponseDto.fromEntity(c);
    }

    // ✅ 클러스터 등록
    public ClusterResponseDto createCluster(ClusterRequestDto dto) {
        // 1) name 중복 검사
        if (clusterRepository.existsByName(dto.getName())) {
            throw new BusinessException(ErrorCode.DUPLICATED_CLUSTER_NAME);
        }
        // 2) apiServerUrl 중복 검사
        if (clusterRepository.existsByApiServerUrl(dto.getApiServerUrl())) {
            throw new BusinessException(ErrorCode.CLUSTER_APISERVER_DUPLICATE);
        }

        // 3) DB에 저장
        Cluster saved = clusterRepository.save(dto.toEntity());

        // 4) 저장된 엔티티로 Client 생성 & 연결 테스트
        KubernetesClient client = k8sConfig.buildClient(ClusterRequestDto.fromEntity(saved));
        try {
            client.getVersion();  // 실패 시 예외 발생
        } catch (Exception e) {
            // 연결 실패하면 저장 롤백
            throw new BusinessException(ErrorCode.CLUSTER_CONNECTION_FAILED);
        }

        // 5) 정상 등록 응답
        return ClusterResponseDto.fromEntity(saved);
    }

    // ✅
    public void delete(Long clusterId) {
        if (!clusterRepository.existsById(clusterId)) {
            throw new BusinessException(ErrorCode.CLUSTER_NOT_FOUND);
        }
        clusterRepository.deleteById(clusterId);
    }
}