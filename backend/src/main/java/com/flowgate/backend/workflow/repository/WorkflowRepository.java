package com.flowgate.backend.workflow.repository;

import com.flowgate.backend.workflow.entity.Workflow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WorkflowRepository extends JpaRepository<Workflow, UUID> {
    Optional<Workflow> findFirstByRequestTypeIdAndActiveTrue(UUID requestTypeId);
    Optional<Workflow> findFirstByRequestTypeId(UUID requestTypeId);
}
