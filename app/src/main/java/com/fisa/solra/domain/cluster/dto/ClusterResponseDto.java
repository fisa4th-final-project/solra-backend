package com.fisa.solra.domain.cluster.dto;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.VersionInfo;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClusterResponseDto {
    private String clusterId;
    private String masterUrl;
    private String kubernetesVersion;
    private int totalNodes;
    private int podCount;
    private int runningPodCount;
    private int namespaceCount;
    private int deploymentCount;
    private int serviceCount;

    // ✅ 클러스터 정보 변환 로직 (실시간 조회)
    public static ClusterResponseDto fromClient(KubernetesClient client, String clusterId) {
        VersionInfo version = client.getVersion();
        String gitVersion = version.getGitVersion();

        var nodes = client.nodes().list().getItems();
        var pods = client.pods().inAnyNamespace().list().getItems();
        int runningPodCount = (int) pods.stream()
                .filter(p -> "Running".equals(p.getStatus().getPhase()))
                .count();

        return ClusterResponseDto.builder()
                .clusterId(clusterId)
                .masterUrl(client.getConfiguration().getMasterUrl())
                .kubernetesVersion(gitVersion)
                .totalNodes(nodes.size())
                .podCount(pods.size())
                .runningPodCount(runningPodCount)
                .namespaceCount(client.namespaces().list().getItems().size())
                .deploymentCount(client.apps().deployments().inAnyNamespace().list().getItems().size())
                .serviceCount(client.services().inAnyNamespace().list().getItems().size())
                .build();
    }
}