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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Base64;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class Fabric8K8sConfig {

    private final ClusterRepository clusterRepository;

    /**
     * DB에서 첫 번째 클러스터 메타를 읽어와
     * 외부 VPN API 서버로만 연결하는 전역 KubernetesClient 빈을 생성합니다.
     */
    @Bean
    public KubernetesClient kubernetesClient() {
        // 1) DB에서 기본 클러스터 메타 조회
        Cluster cluster = clusterRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("클러스터 메타가 없습니다."));
        ClusterRequestDto dto = ClusterRequestDto.fromEntity(cluster);

        // 2) Base64 디코딩
        String caCertData = new String(Base64.getDecoder().decode(dto.getCaCert()));
        String token      = new String(Base64.getDecoder().decode(dto.getSaToken()));
        String masterUrl  = dto.getApiServerUrl();

        log.info("[Fabric8K8sConfig] initializing external client → {}", masterUrl);

        // 3) 외부 API 서버 전용 Config 빌드
        Config config = new ConfigBuilder()
                .withMasterUrl(masterUrl)
                .withOauthToken(token)
                .withCaCertData(caCertData)
                .withTrustCerts(true)
                .withDisableHostnameVerification(true)
                .withConnectionTimeout(10_000)
                .withRequestTimeout(60_000)
                .build();

        // 4) 클라이언트 반환
        return new DefaultKubernetesClient(config);
    }

    /**
     * (멀티-클러스터용) 직접 DTO를 주입할 때 쓰는 헬퍼 메서드
     */
    public KubernetesClient buildClient(ClusterRequestDto dto) {
        // 위와 동일한 로직을 중복 없이 호출하고 싶다면
        return kubernetesClient();
    }
}
