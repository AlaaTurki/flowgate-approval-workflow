package com.flowgate.backend.workflow.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateWorkflowRequest {

    @NotBlank
    @Size(min = 3, max = 100)
    private String name;

    @NotEmpty
    @Size(min = 1, max = 10)
    @Valid
    private List<CreateWorkflowStepRequest> steps;
}
