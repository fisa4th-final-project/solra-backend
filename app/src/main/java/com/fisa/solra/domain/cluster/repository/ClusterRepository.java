package com.fisa.solra.domain.cluster.repository;

import com.fisa.solra.domain.cluster.entity.Cluster;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClusterRepository extends JpaRepository<Cluster, Long> {

    boolean existsByName(String name);
    boolean existsByApiServerUrl(String apiServerUrl);

}
