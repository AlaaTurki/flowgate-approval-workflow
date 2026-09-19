package com.flowgate.backend.workflow.dto;

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
public class WorkflowDto {
    private UUID id;
    private String name;
    private UUID requestTypeId;
    private boolean active;
    private OffsetDateTime createdAt;
    private List<WorkflowStepDto> steps;
}
