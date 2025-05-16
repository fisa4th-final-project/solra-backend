package com.fisa.solra.domain.service.dto;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServicePort;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
public class ServiceResponseDto {
    private String name;
    private String type;
    private String clusterIP;
    private Map<String, String> selector;
    private List<ServicePort> ports;

    public static ServiceResponseDto from(Service svc) {
        return ServiceResponseDto.builder()
                .name(svc.getMetadata().getName())
                .type(svc.getSpec().getType())
                .clusterIP(svc.getSpec().getClusterIP())
                .selector(svc.getSpec().getSelector())
                .ports(svc.getSpec().getPorts())
                .build();
    }
}