// src/main/java/com/fisa/solra/domain/deployment/service/DeploymentService.java
package com.fisa.solra.domain.deployment.service;


import com.fisa.solra.domain.deployment.dto.DeploymentCreateRequestDto;
import com.fisa.solra.domain.deployment.dto.DeploymentCreateResponseDto;
import com.fisa.solra.domain.deployment.dto.DeploymentRequestDto;
import com.fisa.solra.domain.deployment.dto.DeploymentResponseDto;
import com.fisa.solra.global.config.KubernetesClientProvider;
import com.fisa.solra.global.exception.BusinessException;
import com.fisa.solra.global.exception.ErrorCode;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.StatusDetails;
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

    private final KubernetesClientProvider clientProvider;

    // ✅ 전체 디플로이먼트 조회
    public List<DeploymentResponseDto> getDeployments(Long clusterId, String namespace) {
        KubernetesClient client = clientProvider.getClient(clusterId);

        // (1) 네임스페이스 존재 여부 검사
        Namespace ns = client.namespaces().withName(namespace).get();
        if (ns == null) {
            throw new BusinessException(ErrorCode.NAMESPACE_NOT_FOUND);
        }

        // (2) Deployment 목록 조회 & DTO 변환
        return client.apps().deployments()
                .inNamespace(namespace)
                .list()
                .getItems()
                .stream()
                .map(DeploymentResponseDto::from)
                .collect(Collectors.toList());
    }

    // ✅ 단일 디플로이먼트 조회
    public DeploymentResponseDto getDeployment(Long clusterId, String namespace, String name) {
        KubernetesClient client = clientProvider.getClient(clusterId);
        Deployment dp = client.apps().deployments()
                .inNamespace(namespace)
                .withName(name)
                .get();
        if (dp == null) {
            throw new BusinessException(ErrorCode.DEPLOYMENT_NOT_FOUND);
        }
        return DeploymentResponseDto.from(dp);
    }

    // ✅ 디플로이먼트 생성
    public DeploymentCreateResponseDto createDeployment(
            Long clusterId,
            String namespace,
            DeploymentCreateRequestDto dto) {

        KubernetesClient client = clientProvider.getClient(clusterId);

        // 중복 체크
        if (client.apps().deployments()
                .inNamespace(namespace)
                .withName(dto.getName())
                .get() != null) {
            throw new BusinessException(ErrorCode.DUPLICATED_DEPLOYMENT_NAME);
        }

        // Deployment 모델 빌드
        Deployment deployment = new DeploymentBuilder()
                .withNewMetadata().withName(dto.getName()).endMetadata()
                .withNewSpec()
                .withReplicas(dto.getReplicas())
                .withNewSelector().addToMatchLabels(dto.getLabels()).endSelector()
                .withNewTemplate()
                .withNewMetadata().addToLabels(dto.getLabels()).endMetadata()
                .withNewSpec()
                .addNewContainer()
                .withName(dto.getContainer().getName())
                .withImage(dto.getContainer().getImage())
                .addNewPort().withContainerPort(dto.getContainer().getPort()).endPort()
                .endContainer()
                .endSpec()
                .endTemplate()
                .endSpec()
                .build();

        // 생성 호출
        Deployment created = client.apps().deployments()
                .inNamespace(namespace)
                .resource(deployment)
                .create();
        if (created == null) {
            throw new BusinessException(ErrorCode.DEPLOYMENT_CREATION_FAILED);
        }

        // 생성 응답 DTO
        return DeploymentCreateResponseDto.builder()
                .name(dto.getName())
                .replicas(dto.getReplicas())
                .labels(dto.getLabels())
                .container(dto.getContainer())
                .build();
    }

    // ✅ 디플로이먼트 수정
    public DeploymentResponseDto updateDeployment(
            Long clusterId,
            String namespace,
            String name,
            DeploymentRequestDto dto) {

        KubernetesClient client = clientProvider.getClient(clusterId);

        // (1) 기존 Deployment 조회
        Deployment existing = client.apps().deployments()
                .inNamespace(namespace)
                .withName(name)
                .get();
        if (existing == null) {
            throw new BusinessException(ErrorCode.DEPLOYMENT_NOT_FOUND);
        }

        // (2) replicas 필드만 수정
        Deployment updated = client.apps().deployments()
                .inNamespace(namespace)
                .withName(name)
                .edit(d -> new DeploymentBuilder(d)
                        .editSpec()
                        .withReplicas(dto.getReplicas())
                        .endSpec()
                        .build()
                );
        if (updated == null) {
            throw new BusinessException(ErrorCode.DEPLOYMENT_UPDATE_FAILED);
        }

        return DeploymentResponseDto.from(updated);
    }

    // ✅ 디플로이먼트 삭제
    public void deleteDeployment(Long clusterId, String namespace, String name) {
        KubernetesClient client = clientProvider.getClient(clusterId);

        // (1) 존재 여부 확인
        Deployment existing = client.apps().deployments()
                .inNamespace(namespace)
                .withName(name)
                .get();
        if (existing == null) {
            throw new BusinessException(ErrorCode.DEPLOYMENT_NOT_FOUND);
        }

        // (2) 삭제
        List<StatusDetails> status = client
                .resource(existing)
                .delete();
        if (status == null || status.isEmpty()) {
            throw new BusinessException(ErrorCode.DEPLOYMENT_DELETION_FAILED);
        }
    }
}