package com.fisa.solra.domain.cluster.service;

import com.fisa.solra.domain.cluster.dto.ClusterResponseDto;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.VersionInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClusterService {

    private final KubernetesClient k8sClient;

    // ✅ 전체 클러스터 정보 조회 (단일 클러스터 환경 기준)
    public List<ClusterResponseDto> getAllClusterInfos() {
        ClusterResponseDto dto = ClusterResponseDto.fromClient(k8sClient, "local-cluster");
        return List.of(dto);
    }
}