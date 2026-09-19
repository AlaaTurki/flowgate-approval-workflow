package com.flowgate.backend.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowStepDto {
    private UUID id;
    private String name;
    private int orderIndex;
    private String approverRole;
    private UUID approverUserId;
    private boolean requiresComment;
}
