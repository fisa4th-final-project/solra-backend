package com.fisa.solra.domain.cluster.dto;

import com.fisa.solra.domain.cluster.entity.Cluster;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClusterResponseDto {
    private Long clusterId;
    private Long orgId;
    private String name;
    private String env;
    private String caCert;
    private String saToken;
    private String apiServerUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Entity → DTO 변환
    public static ClusterResponseDto fromEntity(Cluster entity) {
        return ClusterResponseDto.builder()
                .clusterId(entity.getClusterId())
                .orgId(entity.getOrgId())
                .name(entity.getName())
                .env(entity.getEnv())
                .caCert(entity.getCaCert())
                .saToken(entity.getSaToken())
                .apiServerUrl(entity.getApiServerUrl())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}