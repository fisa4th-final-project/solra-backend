package com.fisa.solra.domain.node.dto;

import io.fabric8.kubernetes.api.model.Quantity;
import lombok.*;

import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NodeInfoResponseDto {
    private Long clusterId;                    // 여기에 추가
    private String name;                       // 노드 이름
    private String status;                     // Ready / NotReady
    private Map<String, Quantity> capacity;    // 총 용량
    private Map<String, Quantity> allocatable; // 할당 가능 자원
}