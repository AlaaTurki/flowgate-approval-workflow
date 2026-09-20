package com.flowgate.backend.workflow.dto;

import com.flowgate.backend.workflow.entity.RequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequestDetailDto {
    private UUID id;
    private UUID requestTypeId;
    private String requestTypeName;
    private UUID submittedById;
    private String submittedByUsername;
    private String title;
    private String description;
    private RequestStatus status;
    private int currentStepIndex;
    private String currentApproverRole;
    private java.util.UUID currentApproverUserId;
    private String currentApproverUsername;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime resolvedAt;
    private String lastComment;
    private java.math.BigDecimal amount;
    private List<ApprovalActionDto> actions;
}
