package com.fisa.solra.domain.cluster.service;

import com.fisa.solra.domain.cluster.dto.ClusterRequestDto;
import com.fisa.solra.domain.cluster.dto.ClusterResponseDto;
import com.fisa.solra.domain.cluster.entity.Cluster;
import com.fisa.solra.domain.cluster.repository.ClusterRepository;
import com.fisa.solra.domain.organization.entity.Organization;
import com.fisa.solra.domain.organization.repository.OrganizationRepository;
import com.fisa.solra.global.config.Fabric8K8sConfig;
import com.fisa.solra.global.exception.BusinessException;
import com.fisa.solra.global.exception.ErrorCode;
import com.fisa.solra.global.util.SecurityUtil;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ClusterService {

    private final Fabric8K8sConfig k8sConfig;
    private final ClusterRepository clusterRepository;
    private final OrganizationRepository organizationRepository;

    // ✅ 클러스터 전체 조회
    public List<ClusterResponseDto> getClusters(Long orgId) {
        List<Cluster> clusters;

        // 1) 권한은 컨트롤러에서 @PreAuthorize("CLUSTER_READ")로 검사됨
        // 2) 현재 사용자의 조직 ID 조회
        Long currentOrgId = SecurityUtil.getOrgId();
        boolean isRoot = SecurityUtil.hasRole("ROOT");

        // 3) 조회 조건 분기
        if (isRoot) {
            // ROOT는 모든 조직의 클러스터 조회 가능
            clusters = (orgId != null)
                    ? clusterRepository.findByOrganization_OrgId(orgId)
                    : clusterRepository.findAll();
        } else {
            // 일반 사용자는 자신의 조직에 속한 클러스터만 조회 가능
            if (orgId != null && !Objects.equals(orgId, currentOrgId)) {
                throw new BusinessException(ErrorCode.ACCESS_DENIED);
            }
            clusters = clusterRepository.findByOrganization_OrgId(currentOrgId);
        }

        // 4) Entity → DTO 변환
        return clusters.stream()
                .map(ClusterResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    // ✅ 클러스터 단일 조회
    public ClusterResponseDto getCluster(Long clusterId) {
        // 1) 클러스터 조회
        Cluster c = clusterRepository.findById(clusterId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CLUSTER_NOT_FOUND));

        // 2) 조직 일치 검사 (ROOT는 우회)
        if (!SecurityUtil.hasRole("ROOT") &&
                !Objects.equals(c.getOrganization().getOrgId(), SecurityUtil.getOrgId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        // 3) 응답 반환
        return ClusterResponseDto.fromEntity(c);
    }

    // ✅ 클러스터 등록
    public ClusterResponseDto createCluster(ClusterRequestDto dto) {

        // 1) name 중복 검사
        if (clusterRepository.existsByName(dto.getName())) {
            throw new BusinessException(ErrorCode.DUPLICATED_CLUSTER_NAME);
        }
        // 2) apiServerUrl 중복 검사
        if (clusterRepository.existsByApiServerUrl(dto.getApiServerUrl())) {
            throw new BusinessException(ErrorCode.CLUSTER_APISERVER_DUPLICATE);
        }

        // 3) orgId로 Organization 엔티티 조회
        Organization organization = organizationRepository.findById(dto.getOrgId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ORGANIZATION_NOT_FOUND));

        // 4) DB에 저장
        Cluster saved = clusterRepository.save(dto.toEntity(organization));

        // 5) 정상 등록 응답
        return ClusterResponseDto.fromEntity(saved);
    }

    // ✅ 클러스터 수정
    public ClusterResponseDto updateCluster(Long clusterId, ClusterRequestDto dto) {
        // 1) 기존 클러스터 조회
        Cluster existing = clusterRepository.findById(clusterId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CLUSTER_NOT_FOUND));

        // 2) 조직 일치 검사 (ROOT는 우회)
        if (!SecurityUtil.hasRole("ROOT") &&
                !Objects.equals(existing.getOrganization().getOrgId(), SecurityUtil.getOrgId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        // 3) 중복 검사(name, apiServerUrl)
        if (dto.getName() != null
                && !dto.getName().equals(existing.getName())
                && clusterRepository.existsByName(dto.getName())) {
            throw new BusinessException(ErrorCode.DUPLICATED_CLUSTER_NAME);
        }
        if (dto.getApiServerUrl() != null
                && !dto.getApiServerUrl().equals(existing.getApiServerUrl())
                && clusterRepository.existsByApiServerUrl(dto.getApiServerUrl())) {
            throw new BusinessException(ErrorCode.CLUSTER_APISERVER_DUPLICATE);
        }

        // 4) 변경된 필드가 하나도 없으면 no-change 예외
        boolean noChange =
                (dto.getName()          == null || Objects.equals(dto.getName(), existing.getName())) &&
                        (dto.getEnv()           == null || Objects.equals(dto.getEnv(), existing.getEnv())) &&
                        (dto.getCaCert()        == null || Objects.equals(dto.getCaCert(), existing.getCaCert())) &&
                        (dto.getSaToken()       == null || Objects.equals(dto.getSaToken(), existing.getSaToken())) &&
                        (dto.getApiServerUrl()  == null || Objects.equals(dto.getApiServerUrl(), existing.getApiServerUrl()));
        if (noChange) {
            throw new BusinessException(ErrorCode.CLUSTER_UPDATE_NO_CHANGE);
        }

        // 5) 실제 엔티티에 업데이트
        existing.update(dto);
        Cluster saved = clusterRepository.save(existing);
        return ClusterResponseDto.fromEntity(saved);
    }

    // ✅클러스터 삭제
    public void delete(Long clusterId) {
        // 1) 클러스터 조회
        Cluster cluster = clusterRepository.findById(clusterId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CLUSTER_NOT_FOUND));

        // 2) 조직 일치 검사 (ROOT 우회)
        if (!SecurityUtil.hasRole("ROOT") &&
                !Objects.equals(cluster.getOrganization().getOrgId(), SecurityUtil.getOrgId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        // 3) 삭제 처리
        clusterRepository.deleteById(clusterId);
    }

    // 클러스터 연결 검증
    public void testConnection(Long clusterId) {
        // 1) 클러스터 조회
        Cluster cluster = clusterRepository.findById(clusterId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CLUSTER_NOT_FOUND));

        // 2) 조직 일치 검사 (ROOT 우회)
        if (!SecurityUtil.hasRole("ROOT") &&
                !Objects.equals(cluster.getOrganization().getOrgId(), SecurityUtil.getOrgId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        // 3) Kubernetes Client 생성 및 연결 테스트
        try {
            KubernetesClient client = k8sConfig.buildClient(ClusterRequestDto.fromEntity(cluster));
            client.getVersion(); // 연결 시도
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.CLUSTER_CONNECTION_FAILED);
        }
    }
}