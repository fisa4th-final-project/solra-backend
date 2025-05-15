package com.fisa.solra.domain.deployment.dto;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Collections;
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
        // spec.replicas
        Integer replicas = d.getSpec() != null && d.getSpec().getReplicas() != null
                ? d.getSpec().getReplicas()
                : 0;

        // status.readyReplicas — null safe
        Integer readyReplicas = 0;
        if (d.getStatus() != null && d.getStatus().getReadyReplicas() != null) {
            readyReplicas = d.getStatus().getReadyReplicas();
        }

        // selector
        Map<String,String> selector = d.getSpec() != null
                && d.getSpec().getSelector() != null
                && d.getSpec().getSelector().getMatchLabels() != null
                ? d.getSpec().getSelector().getMatchLabels()
                : Collections.emptyMap();

        // images
        List<String> images = d.getSpec() != null
                && d.getSpec().getTemplate() != null
                && d.getSpec().getTemplate().getSpec() != null
                && d.getSpec().getTemplate().getSpec().getContainers() != null
                ? d.getSpec().getTemplate().getSpec().getContainers()
                .stream().map(c -> c.getImage()).toList()
                : Collections.emptyList();

        return DeploymentResponseDto.builder()
                .name(d.getMetadata().getName())
                .replicas(replicas)
                .readyReplicas(readyReplicas)
                .selector(selector)
                .images(images)
                .build();
    }

}