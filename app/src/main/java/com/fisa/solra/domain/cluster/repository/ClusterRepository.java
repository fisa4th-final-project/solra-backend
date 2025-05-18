package com.fisa.solra.domain.cluster.repository;

import com.fisa.solra.domain.cluster.entity.Cluster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClusterRepository extends JpaRepository<Cluster, Long> {
    Optional<Cluster> findByClusterId(Long clusterId);
    boolean existsByClusterId(Long clusterId);
    // 클러스터 이름(name)이 이미 존재하는지 확인
    boolean existsByName(String name);
    // 예: name + env 조합도 유니크하게 관리하고 싶다면
    boolean existsByNameAndEnv(String name, String env);
    // 만약 apiServerUrl도 유니크라면
    boolean existsByApiServerUrl(String apiServerUrl);
}
