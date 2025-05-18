package com.fisa.solra.domain.cluster.entity;

import com.fisa.solra.domain.cluster.dto.ClusterRequestDto;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Cluster {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long clusterId;

    @Column(nullable = false)
    private Long orgId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 10)
    private String env;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String caCert;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String saToken;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String apiServerUrl;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // ✅ 일부 필드 값만 조건부 업데이트
    public void update(ClusterRequestDto dto) {
        if (dto.getName() != null) this.name = dto.getName();
        if (dto.getEnv() != null) this.env = dto.getEnv();
        if (dto.getCaCert() != null) this.caCert = dto.getCaCert();
        if (dto.getSaToken() != null) this.saToken = dto.getSaToken();
        if (dto.getApiServerUrl() != null) this.apiServerUrl = dto.getApiServerUrl();
        this.updatedAt = LocalDateTime.now();
    }
}