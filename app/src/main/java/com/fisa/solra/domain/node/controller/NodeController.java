package com.fisa.solra.domain.node.controller;

import com.fisa.solra.domain.node.dto.NodeInfoResponseDto;
import com.fisa.solra.domain.node.service.NodeService;
import com.fisa.solra.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/nodes")
@RequiredArgsConstructor
public class NodeController {

    private final NodeService nodeService;

    //클러스터 내 모든 노드 정보 조회
    @GetMapping
    public ResponseEntity<ApiResponse<List<NodeInfoResponseDto>>> getNodeList() {
        return ResponseEntity.ok(
                ApiResponse.success(nodeService.getAllNodes(), "노드 리스트 조회에 성공했습니다.")
        );
    }

    //특정 노드 조회
    @GetMapping("/{nodeName}")
    public ResponseEntity<ApiResponse<NodeInfoResponseDto>> getNodeDetail(@PathVariable String nodeName) {
        return ResponseEntity.ok(
                ApiResponse.success(nodeService.getNodeDetail(nodeName), "노드 상세 조회에 성공했습니다.")
        );
    }

}
