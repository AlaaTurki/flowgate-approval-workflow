package com.flowgate.backend.workflow.repository;

import com.flowgate.backend.user.entity.User;
import com.flowgate.backend.workflow.entity.ApprovalAction;
import com.flowgate.backend.workflow.entity.Request;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ApprovalActionRepository extends JpaRepository<ApprovalAction, UUID> {
    List<ApprovalAction> findByRequestOrderByCreatedAtAsc(Request request);
    long countByActor(User actor);
}
