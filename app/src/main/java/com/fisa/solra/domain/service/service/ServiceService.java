// src/main/java/com/fisa/solra/domain/service/service/ServiceService.java
package com.fisa.solra.domain.service.service;

import com.fisa.solra.domain.cluster.dto.ClusterRequestDto;
import com.fisa.solra.domain.cluster.entity.Cluster;
import com.fisa.solra.domain.cluster.repository.ClusterRepository;
import com.fisa.solra.domain.service.dto.ServiceRequestDto;
import com.fisa.solra.domain.service.dto.ServiceResponseDto;
import com.fisa.solra.global.config.Fabric8K8sConfig;
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

    private final ClusterRepository clusterRepository;
    private final Fabric8K8sConfig k8sConfig;
    // 페브릭8 Service 타입 참조용
    private final Class<io.fabric8.kubernetes.api.model.Service> k8sServiceType = io.fabric8.kubernetes.api.model.Service.class;

    /**
     * DB에서 등록된 첫 클러스터 메타를 조회하여
     * KubernetesClient를 생성합니다.
     */
    private KubernetesClient getClient() {
        Cluster cluster = clusterRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.CLUSTER_NOT_FOUND));
        ClusterRequestDto dto = ClusterRequestDto.fromEntity(cluster);
        return k8sConfig.buildClient(dto);
    }

    // ✅ 네임스페이스 내 서비스 전체 조회
    public List<ServiceResponseDto> getServices(String namespace) {
        KubernetesClient client = getClient();
        Namespace ns = client.namespaces().withName(namespace).get();
        if (ns == null) throw new BusinessException(ErrorCode.NAMESPACE_NOT_FOUND);

        return client.resources(k8sServiceType)
                .inNamespace(namespace)
                .list()
                .getItems()
                .stream()
                .map(svc -> ServiceResponseDto.from(svc))
                .collect(Collectors.toList());
    }

    // ✅ 단일 서비스 조회
    public ServiceResponseDto getService(String namespace, String name) {
        KubernetesClient client = getClient();
        io.fabric8.kubernetes.api.model.Service svc = client.resources(k8sServiceType)
                .inNamespace(namespace)
                .withName(name)
                .get();
        if (svc == null) throw new BusinessException(ErrorCode.SERVICE_NOT_FOUND);
        return ServiceResponseDto.from(svc);
    }

    // ✅ 서비스 생성
    public ServiceResponseDto createService(String namespace, ServiceRequestDto dto) {
        KubernetesClient client = getClient();
        if (client.resources(k8sServiceType).inNamespace(namespace).withName(dto.getName()).get() != null) {
            throw new BusinessException(ErrorCode.DUPLICATED_SERVICE_NAME);
        }
        // Service 리소스 구성
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
        io.fabric8.kubernetes.api.model.Service created = client.resources(k8sServiceType)
                .inNamespace(namespace)
                .resource(service)
                .create();
        if (created == null) throw new BusinessException(ErrorCode.SERVICE_CREATION_FAILED);
        return ServiceResponseDto.from(created);
    }

    // ✅ 서비스 수정
    public ServiceResponseDto updateService(String namespace, String name, ServiceRequestDto dto) {
        KubernetesClient client = getClient();
        // existing service
        io.fabric8.kubernetes.api.model.Service svc = client.resources(k8sServiceType)
                .inNamespace(namespace)
                .withName(name)
                .get();
        if (svc == null) throw new BusinessException(ErrorCode.SERVICE_NOT_FOUND);

        // nodePort 사용중 목록
        List<Integer> usedNodePorts = client.resources(k8sServiceType)
                .inNamespace(namespace)
                .list()
                .getItems().stream()
                .flatMap(s -> s.getSpec().getPorts().stream())
                .map(p -> p.getNodePort())
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        boolean conflict = dto.getPorts().stream()
                .map(p -> p.getNodePort())
                .filter(Objects::nonNull)
                .anyMatch(usedNodePorts::contains);
        if (conflict) throw new BusinessException(ErrorCode.POD_NODEPORT_CONFLICT);

        boolean samePorts = svc.getSpec().getPorts().equals(dto.getPorts().stream()
                .map(p -> new io.fabric8.kubernetes.api.model.ServicePortBuilder()
                        .withPort(p.getPort())
                        .withTargetPort(new IntOrString(p.getTargetPort()))
                        .withProtocol(p.getProtocol())
                        .withNodePort(p.getNodePort())
                        .build())
                .collect(Collectors.toList()));
        boolean sameSelector = svc.getSpec().getSelector().equals(dto.getSelector());
        boolean sameType = svc.getSpec().getType().equals(dto.getType());
        if (samePorts && sameSelector && sameType) {
            throw new BusinessException(ErrorCode.SERVICE_UPDATE_FAILED);
        }

        io.fabric8.kubernetes.api.model.Service updated = client.resources(k8sServiceType)
                .inNamespace(namespace)
                .withName(name)
                .edit(s -> {
                    s.getSpec().setPorts(dto.getPorts().stream()
                            .map(p -> new io.fabric8.kubernetes.api.model.ServicePortBuilder()
                                    .withPort(p.getPort())
                                    .withTargetPort(new IntOrString(p.getTargetPort()))
                                    .withProtocol(p.getProtocol())
                                    .withNodePort(p.getNodePort())
                                    .build())
                            .collect(Collectors.toList()));
                    s.getSpec().setSelector(dto.getSelector());
                    if (dto.getType() != null) s.getSpec().setType(dto.getType());
                    return s;
                });
        if (updated == null) throw new BusinessException(ErrorCode.SERVICE_UPDATE_FAILED);
        return ServiceResponseDto.from(updated);
    }

    // ✅ 서비스 삭제
    public void deleteService(String namespace, String name) {
        KubernetesClient client = getClient();
        io.fabric8.kubernetes.api.model.Service svc = client.resources(k8sServiceType)
                .inNamespace(namespace)
                .withName(name)
                .get();
        if (svc == null) throw new BusinessException(ErrorCode.SERVICE_NOT_FOUND);
        List<StatusDetails> result = client.resources(k8sServiceType)
                .inNamespace(namespace)
                .resource(svc)
                .delete();
        if (result == null || result.isEmpty()) throw new BusinessException(ErrorCode.SERVICE_DELETION_FAILED);
    }
}
