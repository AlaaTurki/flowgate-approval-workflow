package com.flowgate.backend.workflow.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateWorkflowStepRequest {

    @NotBlank
    @Size(min = 2, max = 100)
    private String name;

    @Min(0)
    private int orderIndex;

    @NotBlank
    private String approverRole;

    private UUID approverUserId;

    private boolean requiresComment;
}
