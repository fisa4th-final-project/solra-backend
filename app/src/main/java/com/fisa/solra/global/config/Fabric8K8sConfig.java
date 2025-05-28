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
        String caCertData = new String(Base64.getDecoder().decode("LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSURCVENDQWUyZ0F3SUJBZ0lJQmR6TGswanhzdWd3RFFZSktvWklodmNOQVFFTEJRQXdGVEVUTUJFR0ExVUUKQXhNS2EzVmlaWEp1WlhSbGN6QWVGdzB5TlRBMU1qVXlNelF5TURGYUZ3MHpOVEExTWpNeU16UTNNREZhTUJVeApFekFSQmdOVkJBTVRDbXQxWW1WeWJtVjBaWE13Z2dFaU1BMEdDU3FHU0liM0RRRUJBUVVBQTRJQkR3QXdnZ0VLCkFvSUJBUUNzdDVXTEtmU2FhRjVERGxFYXp3Nld3THZ3d0pBQzhpM3JpZ0JFVENweEIvYVFkWC85UzJ6eG5HS2wKbUsxenllR3lhVVV2WE1GbzZZTGVUNlVmQTdoY3NkV2piamdKSGxlQXJxTFJVVFZMSktBcUw5V2syanNHYmZZRwpoSVFycCtMYVJMQWhIWHNPQS9LOTJSR3N4SnI0QlAxSHI1UmhQaE1PUUU1NG1ZN3I0VERBdDBhMmFmellUSnMrClNMMFVYTkplZ0xIbVhpU2RSbGhncXQ3MmM1NC8xNi96QnNTUzFnQ3BNd3RHQW5MS2FYcW1CL0piZjJDT0phQ2EKWXVNN1V1WU1GSTVhNDVBTGtQaS9VREF2ejVMOVFKeGJTWUR3eithYnk4a1FVcCtrTGUwMlVFTnBRMHJKTWRJVgp4WG05YU5PR0V5Z0FqaSszdmdPTWZlV0E5by9CQWdNQkFBR2pXVEJYTUE0R0ExVWREd0VCL3dRRUF3SUNwREFQCkJnTlZIUk1CQWY4RUJUQURBUUgvTUIwR0ExVWREZ1FXQkJTQ3lkT2g0TDNraUx2djY5dE9SenM0WWw0TWZEQVYKQmdOVkhSRUVEakFNZ2dwcmRXSmxjbTVsZEdWek1BMEdDU3FHU0liM0RRRUJDd1VBQTRJQkFRQis0QnBWYnF5VQpFazZsdWZKU00yTUErOWRMOUNiOERuNkd4UGtDY0NVQWJ6Ym1wRjg1Ly8yeTB2aERhdXhNNEhoTE13SUljRzdvCkhCVHdEa0VBdVRPNFZnSldYQXVPWXQ2Y2pVaGt0dEZOcXErL3RCVVBSUTk5U3ZsYWgzc2lDalgwMGZlY2dBT24KY0xIQjBkOHltRlR3ZUw5cHR3WlhZRGNFb3JLVjFqVTJRV2JCdWEyVlNkS1lKd09WTEorNC9ySnRDbVhoOEErTwo5SWV2SU9walY3ZUM5R1RxdXV0d1dUbWJxdTVkT0V1dnlBdE02WjdLV0k5REpYWVJKNnYrK2FxSEErbDVQWUxFCnlDVFZpdU1NOHh6cGhKV1EzS3FZY1hQWEF0Z1hRdnVTOEYzUTA4cEZCMk8vbTZ1S0hoWDhSbzRRNiswMG5GWnEKWUtISWpYYzRWVTJvCi0tLS0tRU5EIENFUlRJRklDQVRFLS0tLS0K"));
        String token      = "ZXlKaGJHY2lPaUpTVXpJMU5pSXNJbXRwWkNJNklrMUxaM3A0ZDAxaFozQTNabGM0Tm1rNVRsRlpkRmR6UjJaQ1MwczVSRFJRWmsxTFMwTTNWRXRoWkVFaWZRLmV5SnBjM01pT2lKcmRXSmxjbTVsZEdWekwzTmxjblpwWTJWaFkyTnZkVzUwSWl3aWEzVmlaWEp1WlhSbGN5NXBieTl6WlhKMmFXTmxZV05qYjNWdWRDOXVZVzFsYzNCaFkyVWlPaUp6YjJ4eVlTMWhZMk5sYzNNaUxDSnJkV0psY201bGRHVnpMbWx2TDNObGNuWnBZMlZoWTJOdmRXNTBMM05sWTNKbGRDNXVZVzFsSWpvaWMyOXNjbUV0WVdSdGFXNHRkRzlyWlc0aUxDSnJkV0psY201bGRHVnpMbWx2TDNObGNuWnBZMlZoWTJOdmRXNTBMM05sY25acFkyVXRZV05qYjNWdWRDNXVZVzFsSWpvaWMyOXNjbUV0WVdSdGFXNGlMQ0pyZFdKbGNtNWxkR1Z6TG1sdkwzTmxjblpwWTJWaFkyTnZkVzUwTDNObGNuWnBZMlV0WVdOamIzVnVkQzUxYVdRaU9pSTRaRGhsWm1WaU9DMW1ZemhsTFRRMU9HRXRZV1V3TWkwNE1qVmhPVGN5TVRrek9EQWlMQ0p6ZFdJaU9pSnplWE4wWlcwNmMyVnlkbWxqWldGalkyOTFiblE2YzI5c2NtRXRZV05qWlhOek9uTnZiSEpoTFdGa2JXbHVJbjAuemhGeG14MC1NUlF6Z19YNzNMbHdHd2JTRTR6U0x5dzczVnF4U3RoaVA0anZaRGMxUzVjcDRKUC14SDdobXRvVW96ZnhreV8yLVBDaGp3emlidE9md1BZZzNoZGJPYWZGUDhCUW1XcFdXc0dqQ3ZYZ0JHdHJGQnVHSi1XSzVQVUZZWVhvQnhNY01MX0dVLS1RZVVqZkdsWk04emdJdTFwR21ZTTZTaDY5RjR1WWZuUE8zNUduY3VCUm1nWEtodzZMSW1Vbm11bUNzeVBxRXpkQktpRGlseEJsYlJiZFh6WU9WZFlFdmlCZlI1QW42YmxsUEROeUhDNHU3ZmFXZC1HSEhpaGoxeHVLRjc3cVJ4YTEwVnNEMHdIdXgzSFdpem9JbG5Dd3Y2Q3E0a1AyYVlEbHBLc1QxVGk3WVRvYzNvcnEyalplUDA4ejhsR1FrYWE3ZFJpeG5n";
        String masterUrl  = "https://C816FC6A38159C4A884D79DDDFF601CA.gr7.ap-northeast-2.eks.amazonaws.com";

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
