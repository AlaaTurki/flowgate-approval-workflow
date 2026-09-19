package com.flowgate.backend.workflow.service;

import com.flowgate.backend.workflow.dto.CreateRequestTypeRequest;
import com.flowgate.backend.workflow.dto.CreateWorkflowRequest;
import com.flowgate.backend.workflow.dto.RequestTypeDto;
import com.flowgate.backend.workflow.dto.WorkflowDto;

import java.util.List;
import java.util.UUID;

public interface WorkflowService {
    RequestTypeDto createRequestType(CreateRequestTypeRequest request);
    List<RequestTypeDto> getRequestTypes();
    RequestTypeDto updateRequestType(UUID id, CreateRequestTypeRequest request);
    void deleteRequestType(UUID id);
    WorkflowDto createWorkflow(UUID requestTypeId, CreateWorkflowRequest request);
    List<WorkflowDto> getWorkflowsForType(UUID requestTypeId);
    void deleteWorkflow(UUID workflowId);
}
