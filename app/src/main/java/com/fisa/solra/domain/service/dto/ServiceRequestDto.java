package com.fisa.solra.domain.service.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class ServiceRequestDto {
    private String name;
    private String type;
    private Map<String, String> selector;
    private List<Port> ports;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Port {
        /** ServicePort name (required if multiple ports) */
        private String name;
        private Integer port;
        private Integer targetPort;
        private String protocol;
        private Integer nodePort;
    }
}