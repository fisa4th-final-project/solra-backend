package com.fisa.solra.domain.deployment.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class DeploymentCreateResponseDto {
    private String name;
    private Integer replicas;
    private Map<String,String> labels;
    private DeploymentCreateRequestDto.ContainerDto container;

}