package com.fisa.solra.domain.service.dto;

import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
public class ServiceRequestDto {
    private String name;
    private String type;
    private Map<String, String> selector;
    private List<Port> ports;

    @Getter
    public static class Port {
        private int port;
        private int targetPort;
        private String protocol;
    }
}