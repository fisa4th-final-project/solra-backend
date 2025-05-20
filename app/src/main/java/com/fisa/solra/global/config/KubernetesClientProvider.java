package com.fisa.solra.global.config;

import com.fisa.solra.domain.cluster.dto.ClusterRequestDto;
import com.fisa.solra.domain.cluster.entity.Cluster;
import com.fisa.solra.domain.cluster.repository.ClusterRepository;
import com.fisa.solra.global.exception.BusinessException;
import com.fisa.solra.global.exception.ErrorCode;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KubernetesClientProvider {

    private final Fabric8K8sConfig k8sConfig;
    private final ClusterRepository clusterRepository;

    /**
     * 클러스터 ID로 캐시 조회. 없으면 DB → 인스턴스 생성 후 캐시/반환
     */
    @Cacheable(cacheNames = "k8sClient", key = "#clusterId")
    public KubernetesClient getClient(Long clusterId) {
        try {
            Cluster cluster = clusterRepository.findById(clusterId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.CLUSTER_CONNECTION_FAILED)); // 503으로 통일
            ClusterRequestDto dto = ClusterRequestDto.fromEntity(cluster);
            return k8sConfig.buildClient(dto);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.CLUSTER_CONNECTION_FAILED);
        }
    }


    /** 클러스터 메타 변경 시 캐시 무효화 필요 */
    @CacheEvict(cacheNames = "k8sClient", key = "#clusterId")
    public void evictClient(Long clusterId) { }
}