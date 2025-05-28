// src/main/java/com/fisa/solra/domain/service/service/ServiceService.java
package com.fisa.solra.domain.service.service;


import com.fisa.solra.domain.service.dto.ServiceRequestDto;
import com.fisa.solra.domain.service.dto.ServiceResponseDto;
import com.fisa.solra.global.config.KubernetesClientProvider;
import com.fisa.solra.global.exception.BusinessException;
import com.fisa.solra.global.exception.ErrorCode;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
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
        var client = clientProvider.getClient(clusterId);
        Namespace ns = client.namespaces().withName(namespace).get();
        if (ns == null) throw new BusinessException(ErrorCode.NAMESPACE_NOT_FOUND);

        if (client.resources(k8sServiceType)
                .inNamespace(namespace)
                .withName(dto.getName()).get() != null) {
            throw new BusinessException(ErrorCode.DUPLICATED_SERVICE_NAME);
        }

        io.fabric8.kubernetes.api.model.Service svc = new io.fabric8.kubernetes.api.model.ServiceBuilder()
                .withNewMetadata().withName(dto.getName()).endMetadata()
                .withNewSpec()
                .withType(dto.getType())
                .withSelector(dto.getSelector())
                .withPorts(dto.getPorts().stream()
                        .map(p -> new io.fabric8.kubernetes.api.model.ServicePortBuilder()
                                .withName(p.getName())
                                .withPort(p.getPort())
                                .withTargetPort(new IntOrString(p.getTargetPort()))
                                .withProtocol(p.getProtocol())
                                .withNodePort(p.getNodePort())
                                .build())
                        .collect(Collectors.toList()))
                .endSpec()
                .build();

        var created = client.resources(k8sServiceType)
                .inNamespace(namespace)
                .resource(svc)
                .create();
        if (created == null) throw new BusinessException(ErrorCode.SERVICE_CREATION_FAILED);
        return ServiceResponseDto.from(created);
    }

    // ✅ 서비스 수정
    public ServiceResponseDto updateService(Long clusterId, String namespace, String name, ServiceRequestDto dto) {
        var client = clientProvider.getClient(clusterId);
        var existing = client.resources(k8sServiceType)
                .inNamespace(namespace)
                .withName(name)
                .get();
        if (existing == null) throw new BusinessException(ErrorCode.SERVICE_NOT_FOUND);

        List<Integer> used = client.resources(k8sServiceType)
                .inNamespace(namespace)
                .list().getItems().stream()
                .flatMap(s -> s.getSpec().getPorts().stream())
                .map(p -> p.getNodePort()).filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (dto.getPorts().stream().map(ServiceRequestDto.Port::getNodePort)
                .filter(Objects::nonNull).anyMatch(used::contains)) {
            throw new BusinessException(ErrorCode.POD_NODEPORT_CONFLICT);
        }

        var updated = client.resources(k8sServiceType)
                .inNamespace(namespace)
                .withName(name)
                .edit(svc -> {
                    svc.getSpec().setSelector(dto.getSelector());
                    if (dto.getType() != null) svc.getSpec().setType(dto.getType());
                    svc.getSpec().setPorts(dto.getPorts().stream()
                            .map(p -> new io.fabric8.kubernetes.api.model.ServicePortBuilder()
                                    .withName(p.getName())
                                    .withPort(p.getPort())
                                    .withTargetPort(new IntOrString(p.getTargetPort()))
                                    .withProtocol(p.getProtocol())
                                    .withNodePort(p.getNodePort())
                                    .build())
                            .collect(Collectors.toList()));
                    return svc;
                });
        if (updated == null) throw new BusinessException(ErrorCode.SERVICE_UPDATE_FAILED);
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