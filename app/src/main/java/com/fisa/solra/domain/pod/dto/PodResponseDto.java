package com.fisa.solra.domain.pod.dto;

import io.fabric8.kubernetes.api.model.Pod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PodResponseDto {
    private String name;
    private String phase;
    private String podIP;
    private String nodeName;

    public static PodResponseDto from(Pod pod) {
        return PodResponseDto.builder()
                .name(pod.getMetadata().getName())
                .phase(pod.getStatus().getPhase())
                .podIP(pod.getStatus().getPodIP())
                .nodeName(pod.getSpec().getNodeName())
                .build();
    }
}
