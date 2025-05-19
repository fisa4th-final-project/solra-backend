// src/main/java/com/fisa/solra/domain/service/service/ServiceService.java
package com.fisa.solra.domain.service.service;


import com.fisa.solra.domain.service.dto.ServiceRequestDto;
import com.fisa.solra.domain.service.dto.ServiceResponseDto;
import com.fisa.solra.global.config.KubernetesClientProvider;
import com.fisa.solra.global.exception.BusinessException;
import com.fisa.solra.global.exception.ErrorCode;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.StatusDetails;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServiceService {

    private final KubernetesClientProvider clientProvider;
    // Fabric8 Service 타입 참조용
    private final Class<io.fabric8.kubernetes.api.model.Service> k8sServiceType =
            io.fabric8.kubernetes.api.model.Service.class;

    // ✅ 네임스페이스 내 서비스 전체 조회
    public List<ServiceResponseDto> getServices(Long clusterId, String namespace) {
        KubernetesClient client = clientProvider.getClient(clusterId);

        // 네임스페이스 검사
        Namespace ns = client.namespaces().withName(namespace).get();
        if (ns == null) {
            throw new BusinessException(ErrorCode.NAMESPACE_NOT_FOUND);
        }

        return client.resources(k8sServiceType)
                .inNamespace(namespace)
                .list()
                .getItems()
                .stream()
                .map(ServiceResponseDto::from)
                .collect(Collectors.toList());
    }

    // ✅ 단일 서비스 조회
    public ServiceResponseDto getService(Long clusterId, String namespace, String name) {
        KubernetesClient client = clientProvider.getClient(clusterId);

        var svc = client.resources(k8sServiceType)
                .inNamespace(namespace)
                .withName(name)
                .get();
        if (svc == null) {
            throw new BusinessException(ErrorCode.SERVICE_NOT_FOUND);
        }
        return ServiceResponseDto.from(svc);
    }

    // ✅ 서비스 생성
    public ServiceResponseDto createService(Long clusterId, String namespace, ServiceRequestDto dto) {
        KubernetesClient client = clientProvider.getClient(clusterId);

        // 중복 검사
        if (client.resources(k8sServiceType)
                .inNamespace(namespace)
                .withName(dto.getName())
                .get() != null) {
            throw new BusinessException(ErrorCode.DUPLICATED_SERVICE_NAME);
        }

        // 리소스 빌드
        io.fabric8.kubernetes.api.model.Service service = new io.fabric8.kubernetes.api.model.ServiceBuilder()
                .withNewMetadata().withName(dto.getName()).endMetadata()
                .withNewSpec()
                .withSelector(dto.getSelector())
                .withType(dto.getType())
                .withPorts(dto.getPorts().stream()
                        .map(p -> new io.fabric8.kubernetes.api.model.ServicePortBuilder()
                                .withPort(p.getPort())
                                .withTargetPort(new IntOrString(p.getTargetPort()))
                                .withProtocol(p.getProtocol())
                                .build())
                        .collect(Collectors.toList()))
                .endSpec()
                .build();

        var created = client.resources(k8sServiceType)
                .inNamespace(namespace)
                .resource(service)
                .create();
        if (created == null) {
            throw new BusinessException(ErrorCode.SERVICE_CREATION_FAILED);
        }
        return ServiceResponseDto.from(created);
    }

    // ✅ 서비스 수정
    public ServiceResponseDto updateService(
            Long clusterId,
            String namespace,
            String name,
            ServiceRequestDto dto) {

        KubernetesClient client = clientProvider.getClient(clusterId);

        // 기존 조회
        var existing = client.resources(k8sServiceType)
                .inNamespace(namespace)
                .withName(name)
                .get();
        if (existing == null) {
            throw new BusinessException(ErrorCode.SERVICE_NOT_FOUND);
        }

        // nodePort 충돌 검사
        List<Integer> usedNodePorts = client.resources(k8sServiceType)
                .inNamespace(namespace)
                .list().getItems().stream()
                .flatMap(s -> s.getSpec().getPorts().stream())
                .map(p -> p.getNodePort())
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (dto.getPorts().stream()
                .map(p -> p.getNodePort())
                .filter(Objects::nonNull)
                .anyMatch(usedNodePorts::contains)) {
            throw new BusinessException(ErrorCode.POD_NODEPORT_CONFLICT);
        }

        // 변화 없는지 검사
        boolean samePorts = existing.getSpec().getPorts().equals(dto.getPorts().stream()
                .map(p -> new io.fabric8.kubernetes.api.model.ServicePortBuilder()
                        .withPort(p.getPort())
                        .withTargetPort(new IntOrString(p.getTargetPort()))
                        .withProtocol(p.getProtocol())
                        .build())
                .collect(Collectors.toList()));
        boolean sameSelector = existing.getSpec().getSelector().equals(dto.getSelector());
        boolean sameType = existing.getSpec().getType().equals(dto.getType());

        if (samePorts && sameSelector && sameType) {
            throw new BusinessException(ErrorCode.SERVICE_UPDATE_FAILED);
        }

        // edit() 로 패치
        var updated = client.resources(k8sServiceType)
                .inNamespace(namespace)
                .withName(name)
                .edit(svc -> {
                    svc.getSpec().setPorts(dto.getPorts().stream()
                            .map(p -> new io.fabric8.kubernetes.api.model.ServicePortBuilder()
                                    .withPort(p.getPort())
                                    .withTargetPort(new IntOrString(p.getTargetPort()))
                                    .withProtocol(p.getProtocol())
                                    .withNodePort(p.getNodePort())
                                    .build())
                            .collect(Collectors.toList()));
                    svc.getSpec().setSelector(dto.getSelector());
                    if (dto.getType() != null) {
                        svc.getSpec().setType(dto.getType());
                    }
                    return svc;
                });

        if (updated == null) {
            throw new BusinessException(ErrorCode.SERVICE_UPDATE_FAILED);
        }

        return ServiceResponseDto.from(updated);
    }

    // ✅ 서비스 삭제
    public void deleteService(Long clusterId, String namespace, String name) {
        KubernetesClient client = clientProvider.getClient(clusterId);

        var svc = client.resources(k8sServiceType)
                .inNamespace(namespace)
                .withName(name)
                .get();
        if (svc == null) {
            throw new BusinessException(ErrorCode.SERVICE_NOT_FOUND);
        }

        List<StatusDetails> result = client.resources(k8sServiceType)
                .inNamespace(namespace)
                .resource(svc)
                .delete();
        if (result == null || result.isEmpty()) {
            throw new BusinessException(ErrorCode.SERVICE_DELETION_FAILED);
        }
    }
}