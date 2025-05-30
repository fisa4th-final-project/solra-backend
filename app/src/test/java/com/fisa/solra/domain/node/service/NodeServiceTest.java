package com.fisa.solra.domain.node.service;

import com.fisa.solra.domain.node.dto.NodeInfoResponseDto;
import com.fisa.solra.global.config.KubernetesClientProvider;
import com.fisa.solra.global.exception.BusinessException;
import com.fisa.solra.global.exception.ErrorCode;
import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.api.model.NodeCondition;
import io.fabric8.kubernetes.api.model.NodeList;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static java.util.Map.of;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NodeServiceTest {

    @InjectMocks
    private NodeService nodeService;

    @Mock
    private KubernetesClientProvider clientProvider;

    @Mock
    private KubernetesClient client;

    @Mock
    private NonNamespaceOperation<Node, NodeList, Resource<Node>> nodeOperation;

    @Mock
    private Resource<Node> nodeResource;

    private final Long clusterId = 1L;

    @BeforeEach
    void setup() {
        when(clientProvider.getClient(clusterId)).thenReturn(client);
        when(client.nodes()).thenReturn(nodeOperation);
    }

    // ✅ TC_NODE_01_01: 전체 노드 목록 조회 → 성공
    @Test
    @DisplayName("TC_NODE_01_01: 전체 노드 목록 조회 → 성공")
    void getAllNodes_success() {
        Node mockNode = createMockNode("node1", "True");
        NodeList nodeList = new NodeList();
        nodeList.setItems(List.of(mockNode));

        when(nodeOperation.list()).thenReturn(nodeList);

        List<NodeInfoResponseDto> result = nodeService.getAllNodes(clusterId);

        System.out.println("✅ TC_NODE_01_01 - 전체 노드 수: " + result.size());
        result.forEach(n -> System.out.println(" - " + n.getName()));

        assertEquals(1, result.size());
        assertEquals("node1", result.get(0).getName());
        assertEquals("Ready", result.get(0).getStatus());
    }

    // ✅ TC_NODE_02_01: 단일 노드 상세 조회 → 성공
    @Test
    @DisplayName("TC_NODE_02_01: 단일 노드 상세 조회 → 성공")
    void getNodeDetail_success() {
        Node mockNode = createMockNode("node2", "True");

        when(nodeOperation.withName("node2")).thenReturn(nodeResource);
        when(nodeResource.get()).thenReturn(mockNode);

        NodeInfoResponseDto result = nodeService.getNodeDetail(clusterId, "node2");

        System.out.println("✅ TC_NODE_02_01 - 조회된 노드 이름: " + result.getName());
        assertEquals("node2", result.getName());
        assertEquals("Ready", result.getStatus());
    }

    // ⚠️ TC_NODE_02_02: 존재하지 않는 노드 조회 → 예외 발생 (NODE_NOT_FOUND)
    @Test
    @DisplayName("TC_NODE_02_02: 존재하지 않는 노드 조회 → 예외 발생 (NODE_NOT_FOUND)")
    void getNodeDetail_notFound_throwsException() {
        when(nodeOperation.withName("ghost")).thenReturn(nodeResource);
        when(nodeResource.get()).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> nodeService.getNodeDetail(clusterId, "ghost"));

        System.out.println("⚠️ TC_NODE_02_02 - 발생한 예외 코드: " + ex.getErrorCode());
        assertEquals(ErrorCode.NODE_NOT_FOUND, ex.getErrorCode());
    }

    // 🔧 테스트용 노드 객체 생성
    private Node createMockNode(String name, String readyStatus) {
        Node node = new Node();
        node.setMetadata(new io.fabric8.kubernetes.api.model.ObjectMeta());
        node.getMetadata().setName(name);

        NodeCondition condition = new NodeCondition();
        condition.setType("Ready");
        condition.setStatus(readyStatus);

        node.setStatus(new io.fabric8.kubernetes.api.model.NodeStatus());
        node.getStatus().setConditions(List.of(condition));
        node.getStatus().setCapacity(of("cpu", new Quantity("2"), "memory", new Quantity("8Gi")));
        node.getStatus().setAllocatable(of("cpu", new Quantity("1.5"), "memory", new Quantity("6Gi")));

        return node;
    }
}