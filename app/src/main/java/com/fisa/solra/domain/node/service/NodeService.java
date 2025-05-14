package com.fisa.solra.domain.node.service;

import com.fisa.solra.domain.node.dto.NodeInfoResponseDto;
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

    private final KubernetesClient k8sClient;


    //전체 노드 조회 후 DTO 변환
    public List<NodeInfoResponseDto> getAllNodes() {
        return k8sClient.nodes().list().getItems().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    //Node 객체를 NodeInfoResponseDto로 변환
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
