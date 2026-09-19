package com.flowgate.backend.workflow.repository;

import com.flowgate.backend.workflow.entity.Workflow;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WorkflowRepository extends JpaRepository<Workflow, UUID> {
    @EntityGraph(attributePaths = "steps")
    Optional<Workflow> findFirstByRequestTypeIdAndActiveTrue(UUID requestTypeId);

    @EntityGraph(attributePaths = "steps")
    Optional<Workflow> findFirstByRequestTypeId(UUID requestTypeId);
}
