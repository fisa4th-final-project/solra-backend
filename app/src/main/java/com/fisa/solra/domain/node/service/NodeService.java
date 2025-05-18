// src/main/java/com/fisa/solra/domain/node/service/NodeService.java
package com.fisa.solra.domain.node.service;

import com.fisa.solra.domain.cluster.dto.ClusterRequestDto;
import com.fisa.solra.domain.cluster.entity.Cluster;
import com.fisa.solra.domain.cluster.repository.ClusterRepository;
import com.fisa.solra.domain.node.dto.NodeInfoResponseDto;
import com.fisa.solra.global.config.Fabric8K8sConfig;
import com.fisa.solra.global.exception.BusinessException;
import com.fisa.solra.global.exception.ErrorCode;
import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.api.model.NodeCondition;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NodeService {

    private final ClusterRepository clusterRepository;
    private final Fabric8K8sConfig k8sConfig;

    /**
     * DB에서 등록된 첫 클러스터 메타를 조회하여
     * KubernetesClient를 생성합니다.
     */
    private KubernetesClient getClient() {
        Cluster cluster = clusterRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.CLUSTER_NOT_FOUND));
        ClusterRequestDto dto = ClusterRequestDto.fromEntity(cluster);
        return k8sConfig.buildClient(dto);
    }

    // ✅ 전체 노드 조회 후 DTO 변환
    public List<NodeInfoResponseDto> getAllNodes() {
        KubernetesClient client = getClient();
        return client.nodes().list().getItems().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // ✅ 특정 노드 상세 조회
    public NodeInfoResponseDto getNodeDetail(String nodeName) {
        KubernetesClient client = getClient();
        Node node = client.nodes().withName(nodeName).get();
        if (node == null) {
            throw new BusinessException(ErrorCode.NODE_NOT_FOUND);
        }
        return toDto(node);
    }

    // ✅ Node 객체를 NodeInfoResponseDto로 변환
    private NodeInfoResponseDto toDto(Node node) {
        Map<String, Quantity> capacity = node.getStatus().getCapacity();
        Map<String, Quantity> allocatable = node.getStatus().getAllocatable();

        String status = node.getStatus().getConditions().stream()
                .filter(c -> "Ready".equals(c.getType()))
                .map(NodeCondition::getStatus)
                .anyMatch("True"::equals) ? "Ready" : "NotReady";

        return NodeInfoResponseDto.builder()
                .name(node.getMetadata().getName())
                .status(status)
                .capacity(capacity)
                .allocatable(allocatable)
                .build();
    }
}
