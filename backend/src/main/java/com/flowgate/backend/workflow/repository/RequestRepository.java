package com.flowgate.backend.workflow.repository;

import com.flowgate.backend.user.entity.User;
import com.flowgate.backend.workflow.entity.Request;
import com.flowgate.backend.workflow.entity.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RequestRepository extends JpaRepository<Request, UUID> {
    List<Request> findBySubmittedBy(User user);
    List<Request> findByStatus(RequestStatus status);
    List<Request> findBySubmittedByAndStatus(User user, RequestStatus status);
}
