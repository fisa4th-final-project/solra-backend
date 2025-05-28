package com.fisa.solra.domain.service.dto;

import io.fabric8.kubernetes.api.model.Service;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@Builder
@AllArgsConstructor
public class ServiceResponseDto {
    private String name;
    private String type;
    private String clusterIP;
    private Map<String, String> selector;
    private List<Port> ports;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Port {
        private String name;
        private Integer port;
        private Integer targetPort;
        private String protocol;
        private Integer nodePort;
    }

    public static ServiceResponseDto from(Service svc) {
        var spec = svc.getSpec();
        List<Port> list = spec.getPorts().stream()
                .map(p -> Port.builder()
                        .name(p.getName())
                        .port(p.getPort())
                        .targetPort(p.getTargetPort() != null ? p.getTargetPort().getIntVal() : null)
                        .protocol(p.getProtocol())
                        .nodePort(p.getNodePort())
                        .build())
                .collect(Collectors.toList());

        return ServiceResponseDto.builder()
                .name(svc.getMetadata().getName())
                .type(spec.getType())
                .clusterIP(spec.getClusterIP())
                .selector(spec.getSelector())
                .ports(list)
                .build();
    }
}
