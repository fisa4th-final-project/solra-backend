package com.fisa.solra.domain.deployment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeploymentCreateRequestDto {
    /** 디플로이먼트 이름 (필수) */
    @NotBlank
    private String name;

    /** replica 수 (필수, 최소 1) */
    @NotNull
    @Min(1)
    private Integer replicas;

    /** selector·template 에 쓰일 labels (필수, 최소 하나) */
    @NotEmpty
    private Map<@NotBlank String, @NotBlank String> labels;

    /** 컨테이너 정보 (필수) */
    @Valid
    @NotNull
    private ContainerDto container;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ContainerDto {
        @NotBlank
        private String name;

        @NotBlank
        private String image;

        /** 컨테이너 포트 (필수, 1–65535) */
        @NotNull
        @Min(1) @Max(65535)
        private Integer port;
    }
}