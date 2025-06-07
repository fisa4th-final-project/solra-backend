package com.fisa.solra.global.config;

import com.fisa.solra.domain.cluster.dto.ClusterRequestDto;
import com.fisa.solra.domain.cluster.entity.Cluster;
import com.fisa.solra.domain.cluster.repository.ClusterRepository;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.util.Base64;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class Fabric8K8sConfig {

    private final ClusterRepository clusterRepository;

    /**
     * 기본 클러스터 전역 클라이언트 (싱글클러스터에서만 사용)
     */
    public KubernetesClient kubernetesClient() {
        Cluster cluster = clusterRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("클러스터 메타가 없습니다."));
        ClusterRequestDto dto = ClusterRequestDto.fromEntity(cluster);
        return buildClient(dto); // 이 줄로 리팩터링
    }

    /**
     * DTO 기반 동적 클라이언트 생성
     */
    public KubernetesClient buildClient(ClusterRequestDto dto) {
        String caCertData = new String(Base64.getDecoder().decode(dto.getCaCert()));
        String token      = new String(Base64.getDecoder().decode(dto.getSaToken()));
        String masterUrl  = dto.getApiServerUrl();

        log.info("[Fabric8K8sConfig] buildClient → {}", masterUrl);

        Config config = new ConfigBuilder()
                .withMasterUrl(masterUrl)
                .withOauthToken(token)
                .withCaCertData(caCertData)
                .withTrustCerts(true)
                .withDisableHostnameVerification(true)
                .withConnectionTimeout(10_000)
                .withRequestTimeout(60_000)
                .build();

        return new DefaultKubernetesClient(config);
    }
}
