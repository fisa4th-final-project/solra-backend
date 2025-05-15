package com.fisa.solra.domain.deployment.service;

import com.fisa.solra.domain.deployment.dto.DeploymentResponseDto;
import com.fisa.solra.global.exception.BusinessException;
import com.fisa.solra.global.exception.ErrorCode;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.apps.Deployment;
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
}