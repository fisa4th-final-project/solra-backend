package com.fisa.solra.domain.node.service;

import com.fisa.solra.domain.node.dto.NodeInfoResponseDto;
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

    private final KubernetesClient k8sClient;


    //전체 노드 조회 후 DTO 변환
    public List<NodeInfoResponseDto> getAllNodes() {
        return k8sClient.nodes().list().getItems().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    //특정 노드 이름으로 단일 노드 상세 조회
    public NodeInfoResponseDto getNodeDetail(String nodeName) {
        Node node = k8sClient.nodes().withName(nodeName).get();

        // ❗ 노드를 찾을 수 없는 경우 커스텀 예외 반환
        if (node == null) throw new BusinessException(ErrorCode.NODE_NOT_FOUND);

        return toDto(node);
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
