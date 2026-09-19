package com.flowgate.backend.workflow.controller;

import com.flowgate.backend.workflow.dto.CreateRequestTypeRequest;
import com.flowgate.backend.workflow.dto.CreateWorkflowRequest;
import com.flowgate.backend.workflow.dto.RequestTypeDto;
import com.flowgate.backend.workflow.dto.WorkflowDto;
import com.flowgate.backend.workflow.service.WorkflowService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workflows")
public class WorkflowController {

    private final WorkflowService workflowService;

    public WorkflowController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @GetMapping("/request-types")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public List<RequestTypeDto> getRequestTypes() {
        return workflowService.getRequestTypes();
    }

    @PostMapping("/request-types")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RequestTypeDto> createRequestType(@Valid @RequestBody CreateRequestTypeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workflowService.createRequestType(request));
    }

    @PutMapping("/request-types/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RequestTypeDto> updateRequestType(@PathVariable UUID id, @Valid @RequestBody CreateRequestTypeRequest request) {
        return ResponseEntity.ok(workflowService.updateRequestType(id, request));
    }

    @DeleteMapping("/request-types/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteRequestType(@PathVariable UUID id) {
        workflowService.deleteRequestType(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/request-types/{requestTypeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WorkflowDto> createWorkflow(@PathVariable UUID requestTypeId,
                                                    @Valid @RequestBody CreateWorkflowRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workflowService.createWorkflow(requestTypeId, request));
    }

    @GetMapping("/workflows")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE')")
    public List<WorkflowDto> searchWorkflows(@RequestParam(required = false) String query) {
        return workflowService.searchWorkflows(query);
    }

    @DeleteMapping("/workflows/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteWorkflow(@PathVariable UUID id) {
        workflowService.deleteWorkflow(id);
        return ResponseEntity.noContent().build();
    }
}
