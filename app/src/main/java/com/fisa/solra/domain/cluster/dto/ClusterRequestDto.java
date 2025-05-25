package com.fisa.solra.domain.cluster.dto;

import com.fisa.solra.domain.cluster.entity.Cluster;
import com.fisa.solra.domain.organization.entity.Organization;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClusterRequestDto {
    private Long clusterId;
    private Long orgId;
    private String name;
    private String env;
    private String caCert;
    private String saToken;
    private String apiServerUrl;

    // DTO → Entity 변환
    public Cluster toEntity(Organization organization) {
        return Cluster.builder()
                .clusterId(clusterId)
                .organization(organization)
                .name(name)
                .env(env)
                .caCert(caCert)
                .saToken(saToken)
                .apiServerUrl(apiServerUrl)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // Entity → DTO 변환 메서드 추가
    public static ClusterRequestDto fromEntity(Cluster entity) {
        return ClusterRequestDto.builder()
                .clusterId(entity.getClusterId())
                .orgId(entity.getOrganization().getOrgId())
                .name(entity.getName())
                .env(entity.getEnv())
                .caCert(entity.getCaCert())
                .saToken(entity.getSaToken())
                .apiServerUrl(entity.getApiServerUrl())
                .build();
    }
}