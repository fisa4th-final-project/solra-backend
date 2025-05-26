package com.fisa.solra.domain.deployment.dto;

import lombok.Getter;

import java.util.Map;

@Getter
public class DeploymentRequestDto {
    private String name;
    private Integer replicas;
    private Map<String, String> labels;
    private ContainerDto container;

    @Getter
    public static class ContainerDto {
        private String name;
        private String image;
        private int port;
    }
}
