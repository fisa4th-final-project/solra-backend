package com.fisa.solra.global.config;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;


@Slf4j
@Configuration
public class Fabric8K8sConfig {

    @Bean
    public KubernetesClient kubernetesClient() {
        try {
            if (Files.exists(Paths.get("/var/run/secrets/kubernetes.io/serviceaccount/token"))) {
                log.debug("Running in-cluster mode");
                return new DefaultKubernetesClient();
            } else {
                log.debug("Running out-of-cluster mode");
                String masterUrl = System.getenv("SPRING_K8S_CLUSTER");
                String caCert   = new String(Base64.getDecoder()
                        .decode(System.getenv("SPRING_K8S_SOLRA_ROOT_CA_CRT")));
                String token    = new String(Base64.getDecoder()
                        .decode(System.getenv("SPRING_K8S_SOLRA_ROOT_OAUTH_TOKEN")));

                Config config = new ConfigBuilder()
                        .withMasterUrl(masterUrl)
                        .withOauthToken(token)
                        .withCaCertData(caCert)
                        .withTrustCerts(true)
                        .withDisableHostnameVerification(true)
                        .withConnectionTimeout(10_000)
                        .withRequestTimeout(60_000)
                        .build();

                // ← 여기에 로깅 추가
                log.debug("Fabric8 Config → masterUrl={}, trustCerts={}, disableHostVerify={}",
                        config.getMasterUrl(),
                        config.isTrustCerts(),
                        config.isDisableHostnameVerification());

                return new DefaultKubernetesClient(config);
            }
        } catch (Exception e) {
            log.error("Failed to initialize KubernetesClient", e);
            throw new IllegalStateException("Kubernetes client 초기화 실패", e);
        }
    }
}

