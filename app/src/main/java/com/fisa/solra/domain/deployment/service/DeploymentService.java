package com.fisa.solra.domain.deployment.service;

import com.fisa.solra.domain.deployment.dto.DeploymentCreateRequestDto;
import com.fisa.solra.domain.deployment.dto.DeploymentCreateResponseDto;
import com.fisa.solra.domain.deployment.dto.DeploymentResponseDto;
import com.fisa.solra.global.exception.BusinessException;
import com.fisa.solra.global.exception.ErrorCode;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import jakarta.validation.Valid;
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

        // 2) 디플로이먼트 목록 조회
        List<Deployment> items = k8sClient.apps()
                .deployments()
                .inNamespace(namespace)
                .list()
                .getItems();

        // 3) 조회 결과가 비어 있으면 에러
        if (items.isEmpty()) {
            throw new BusinessException(ErrorCode.DEPLOYMENT_NOT_FOUND);
        }

        // 4) DTO 변환 후 반환
        return items.stream()
                .map(DeploymentResponseDto::from)
                .collect(Collectors.toList());
    }

    // ✅ 단일 조회
    public DeploymentResponseDto getDeployment(String namespace, String name) {
        Deployment dp = k8sClient.apps().deployments().inNamespace(namespace).withName(name).get();
        if (dp == null) throw new BusinessException(ErrorCode.DEPLOYMENT_NOT_FOUND);
        return DeploymentResponseDto.from(dp);
    }

    // ✅ 생성
    public DeploymentCreateResponseDto createDeployment(
            String namespace,
            DeploymentCreateRequestDto dto) {

        // 1) 중복 체크
        if (k8sClient.apps().deployments()
                .inNamespace(namespace)
                .withName(dto.getName())
                .get() != null) {
            throw new BusinessException(ErrorCode.DUPLICATED_DEPLOYMENT_NAME);
        }

        // 2) Deployment 모델 빌드
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

        // 3) 실제 생성 호출
        Deployment created = k8sClient.apps()
                .deployments()
                .inNamespace(namespace)
                .resource(deployment)
                .create();

        if (created == null) {
            throw new BusinessException(ErrorCode.DEPLOYMENT_CREATION_FAILED);
        }

        // 4) 생성 전용 응답 DTO 로 변환하여 반환
        return DeploymentCreateResponseDto.builder()
                .name(dto.getName())
                .replicas(dto.getReplicas())
                .labels(dto.getLabels())
                .container(dto.getContainer())  // 필요에 따라 포함
                .build();
    }
}