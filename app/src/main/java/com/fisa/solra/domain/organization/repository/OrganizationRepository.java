package com.fisa.solra.domain.organization.repository;

import com.fisa.solra.domain.organization.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    boolean existsByOrgName(String orgName);
}