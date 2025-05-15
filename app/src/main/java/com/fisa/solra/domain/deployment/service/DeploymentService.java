// src/main/java/com/fisa/solra/domain/deployment/service/DeploymentService.java
package com.fisa.solra.domain.deployment.service;

import com.fisa.solra.domain.deployment.dto.DeploymentCreateRequestDto;
import com.fisa.solra.domain.deployment.dto.DeploymentCreateResponseDto;
import com.fisa.solra.domain.deployment.dto.DeploymentRequestDto;
import com.fisa.solra.domain.deployment.dto.DeploymentResponseDto;
import com.fisa.solra.global.exception.BusinessException;
import com.fisa.solra.global.exception.ErrorCode;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeploymentService {

    private final KubernetesClient k8sClient;

    // ✅ 전체 조회
    public List<DeploymentResponseDto> getDeployments(String namespace) {
        // 1) 네임스페이스 존재 여부 확인
        Namespace ns = k8sClient.namespaces().withName(namespace).get();
        if (ns == null) {
            throw new BusinessException(ErrorCode.NAMESPACE_NOT_FOUND);
        }

        // 2) 디플로이먼트 목록 조회 (빈 리스트여도 에러 NOT_THROW)
        List<Deployment> items = k8sClient.apps()
                .deployments()
                .inNamespace(namespace)
                .list()
                .getItems();

        // 3) DTO 변환 후 반환
        return items.stream()
                .map(DeploymentResponseDto::from)
                .collect(Collectors.toList());
    }

    // ✅ 단일 조회
    public DeploymentResponseDto getDeployment(String namespace, String name) {
        Deployment dp = k8sClient.apps()
                .deployments()
                .inNamespace(namespace)
                .withName(name)
                .get();
        if (dp == null) throw new BusinessException(ErrorCode.DEPLOYMENT_NOT_FOUND);
        return DeploymentResponseDto.from(dp);
    }

    // ✅ 생성
    public DeploymentCreateResponseDto createDeployment(
            String namespace,
            DeploymentCreateRequestDto dto) {

        if (k8sClient.apps().deployments()
                .inNamespace(namespace)
                .withName(dto.getName())
                .get() != null) {
            throw new BusinessException(ErrorCode.DUPLICATED_DEPLOYMENT_NAME);
        }

        Deployment deployment = new DeploymentBuilder()
                .withNewMetadata()
                .withName(dto.getName())
                .endMetadata()
                .withNewSpec()
                .withReplicas(dto.getReplicas())
                .withNewSelector()
                .addToMatchLabels(dto.getLabels())
                .endSelector()
                .withNewTemplate()
                .withNewMetadata()
                .addToLabels(dto.getLabels())
                .endMetadata()
                .withNewSpec()
                .addNewContainer()
                .withName(dto.getContainer().getName())
                .withImage(dto.getContainer().getImage())
                .addNewPort()
                .withContainerPort(dto.getContainer().getPort())
                .endPort()
                .endContainer()
                .endSpec()
                .endTemplate()
                .endSpec()
                .build();

        Deployment created = k8sClient.apps()
                .deployments()
                .inNamespace(namespace)
                .resource(deployment)
                .create();
        if (created == null) {
            throw new BusinessException(ErrorCode.DEPLOYMENT_CREATION_FAILED);
        }

        // 4) 생성 전용 응답 DTO 빌드
        return DeploymentCreateResponseDto.builder()
                .name(dto.getName())
                .replicas(dto.getReplicas())
                .labels(dto.getLabels())
                .container(dto.getContainer())
                .build();
    }

    // ✅ 수정
    public DeploymentResponseDto updateDeployment(
            String namespace,
            String name,
            DeploymentRequestDto dto) {

        // 1) 기존 디플로이먼트 조회
        Deployment existing = k8sClient.apps()
                .deployments()
                .inNamespace(namespace)
                .withName(name)
                .get();
        if (existing == null) {
            throw new BusinessException(ErrorCode.DEPLOYMENT_NOT_FOUND);
        }

        // 2) edit(lambda) 방식으로 spec.replicas만 부분 수정
        Deployment updated = k8sClient.apps()
                .deployments()
                .inNamespace(namespace)
                .withName(name)
                .edit(d -> new DeploymentBuilder(d)
                        .editSpec()
                        .withReplicas(dto.getReplicas())
                        .endSpec()
                        .build());

        // 3) 실패 체크
        if (updated == null) {
            throw new BusinessException(ErrorCode.DEPLOYMENT_UPDATE_FAILED);
        }

        // 4) DTO 변환 후 반환
        return DeploymentResponseDto.from(updated);
    }
}
