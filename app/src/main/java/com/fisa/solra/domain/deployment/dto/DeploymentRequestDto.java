package com.fisa.solra.domain.deployment.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class DeploymentRequestDto {
    /** Deployment 리소스 이름 (optional) */
    private String name;

    /** 복제본 수 (optional) */
    private Integer replicas;

    /** Deployment에 적용할 라벨 (optional) */
    private Map<String, String> labels;

    /** 수정할 컨테이너 설정 (optional) */
    private ContainerDto container;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ContainerDto {
        /** 컨테이너 이름 (optional) */
        private String name;

        /** 사용할 이미지 (optional) */
        private String image;

        /** 컨테이너 포트 (optional) */
        private Integer port;
    }
}