package com.fisa.solra.domain.namespace.dto;

import io.fabric8.kubernetes.api.model.Namespace;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
public class NamespaceResponseDto {
    private Long clusterId;              // 추가
    private String name;
    private String status;
    private Map<String, String> labels;
    private Map<String, String> annotations;
    private OffsetDateTime createdAt;

    public static NamespaceResponseDto from(Long clusterId, Namespace ns) {
        return NamespaceResponseDto.builder()
                .clusterId(clusterId)                                           // 세팅
                .name(ns.getMetadata().getName())
                .status(ns.getStatus().getPhase())
                .labels(ns.getMetadata().getLabels())
                .annotations(ns.getMetadata().getAnnotations())
                .createdAt(ns.getMetadata().getCreationTimestamp() != null
                        ? OffsetDateTime.parse(ns.getMetadata().getCreationTimestamp())
                        : null)
                .build();
    }
}