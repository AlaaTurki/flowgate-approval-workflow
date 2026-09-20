package com.flowgate.backend.workflow.repository;

import com.flowgate.backend.workflow.entity.Workflow;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkflowRepository extends JpaRepository<Workflow, UUID> {
    @Override
    @EntityGraph(attributePaths = {"requestType", "steps"})
    List<Workflow> findAll();

    @EntityGraph(attributePaths = {"requestType", "steps"})
    Optional<Workflow> findFirstByRequestTypeIdAndActiveTrue(UUID requestTypeId);

    @EntityGraph(attributePaths = {"requestType", "steps"})
    List<Workflow> findByRequestTypeIdAndActiveTrue(UUID requestTypeId);

    @EntityGraph(attributePaths = {"requestType", "steps"})
    Optional<Workflow> findFirstByRequestTypeId(UUID requestTypeId);

    @EntityGraph(attributePaths = {"requestType", "steps"})
    List<Workflow> findByRequestTypeId(UUID requestTypeId);
}
