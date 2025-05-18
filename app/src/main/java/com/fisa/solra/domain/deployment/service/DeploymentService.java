// src/main/java/com/fisa/solra/domain/deployment/service/DeploymentService.java
package com.fisa.solra.domain.deployment.service;

import com.fisa.solra.domain.cluster.dto.ClusterRequestDto;
import com.fisa.solra.domain.cluster.entity.Cluster;
import com.fisa.solra.domain.cluster.repository.ClusterRepository;
import com.fisa.solra.domain.deployment.dto.DeploymentCreateRequestDto;
import com.fisa.solra.domain.deployment.dto.DeploymentCreateResponseDto;
import com.fisa.solra.domain.deployment.dto.DeploymentRequestDto;
import com.fisa.solra.domain.deployment.dto.DeploymentResponseDto;
import com.fisa.solra.global.config.Fabric8K8sConfig;
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

    private final ClusterRepository clusterRepository;
    private final Fabric8K8sConfig k8sConfig;

    /**
     * DB에서 등록된 첫 클러스터 메타를 조회하여
     * Fabric8 KubernetesClient를 생성합니다.
     */
    private KubernetesClient getClient() {
        Cluster cluster = clusterRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.CLUSTER_NOT_FOUND));
        ClusterRequestDto dto = ClusterRequestDto.fromEntity(cluster);
        return k8sConfig.buildClient(dto);
    }

    // ✅ 전체 디플로이먼트 조회
    public List<DeploymentResponseDto> getDeployments(String namespace) {
        KubernetesClient client = getClient();
        // 네임스페이스 존재 여부
        Namespace ns = client.namespaces().withName(namespace).get();
        if (ns == null) {
            throw new BusinessException(ErrorCode.NAMESPACE_NOT_FOUND);
        }
        // Deployment 목록 조회
        List<Deployment> items = client.apps()
                .deployments()
                .inNamespace(namespace)
                .list()
                .getItems();
        return items.stream()
                .map(DeploymentResponseDto::from)
                .collect(Collectors.toList());
    }

    // ✅ 단일 디플로이먼트 조회
    public DeploymentResponseDto getDeployment(String namespace, String name) {
        KubernetesClient client = getClient();
        Deployment dp = client.apps()
                .deployments()
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
            String namespace,
            DeploymentCreateRequestDto dto) {
        KubernetesClient client = getClient();
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
        Deployment created = client.apps()
                .deployments()
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
            String namespace,
            String name,
            DeploymentRequestDto dto) {
        KubernetesClient client = getClient();
        Deployment existing = client.apps()
                .deployments()
                .inNamespace(namespace)
                .withName(name)
                .get();
        if (existing == null) {
            throw new BusinessException(ErrorCode.DEPLOYMENT_NOT_FOUND);
        }
        Deployment updated = client.apps()
                .deployments()
                .inNamespace(namespace)
                .withName(name)
                .edit(d -> new DeploymentBuilder(d)
                        .editSpec().withReplicas(dto.getReplicas()).endSpec()
                        .build());
        if (updated == null) {
            throw new BusinessException(ErrorCode.DEPLOYMENT_UPDATE_FAILED);
        }
        return DeploymentResponseDto.from(updated);
    }

    // ✅ 디플로이먼트 삭제
    public void deleteDeployment(String namespace, String name) {
        KubernetesClient client = getClient();
        Deployment existing = client.apps()
                .deployments()
                .inNamespace(namespace)
                .withName(name)
                .get();
        if (existing == null) {
            throw new BusinessException(ErrorCode.DEPLOYMENT_NOT_FOUND);
        }
        List<StatusDetails> status = client
                .resource(existing)
                .delete();
        if (status == null || status.isEmpty()) {
            throw new BusinessException(ErrorCode.DEPLOYMENT_DELETION_FAILED);
        }
    }
}
