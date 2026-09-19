package com.flowgate.backend.workflow.repository;

import com.flowgate.backend.user.entity.User;
import com.flowgate.backend.workflow.entity.Request;
import com.flowgate.backend.workflow.entity.RequestStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RequestRepository extends JpaRepository<Request, UUID> {
    @Override
    @EntityGraph(attributePaths = {"requestType", "submittedBy", "actions", "actions.actor"})
    Optional<Request> findById(UUID id);

    @EntityGraph(attributePaths = {"requestType", "submittedBy", "actions", "actions.actor"})
    List<Request> findBySubmittedBy(User user);

    @EntityGraph(attributePaths = {"requestType", "submittedBy", "actions", "actions.actor"})
    List<Request> findByStatus(RequestStatus status);

    @EntityGraph(attributePaths = {"requestType", "submittedBy"})
    List<Request> findBySubmittedByAndStatus(User user, RequestStatus status);
}
