package com.flowgate.backend.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalActionDto {
    private UUID id;
    private String actorName;
    private String actionType;
    private String comment;
    private OffsetDateTime createdAt;
}
