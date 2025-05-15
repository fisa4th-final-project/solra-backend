package com.fisa.solra.domain.deployment.dto;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
public class DeploymentResponseDto {
    private String name;
    private Integer replicas;
    private Integer readyReplicas;
    private Map<String, String> selector;
    private List<String> images;

    public static DeploymentResponseDto from(Deployment d) {
        return DeploymentResponseDto.builder()
                .name(d.getMetadata().getName())
                .replicas(d.getSpec().getReplicas())
                .readyReplicas(d.getStatus() != null ? d.getStatus().getReadyReplicas() : 0)
                .selector(d.getSpec().getSelector().getMatchLabels())
                .images(d.getSpec().getTemplate().getSpec().getContainers().stream().map(c -> c.getImage()).toList())
                .build();
    }
}