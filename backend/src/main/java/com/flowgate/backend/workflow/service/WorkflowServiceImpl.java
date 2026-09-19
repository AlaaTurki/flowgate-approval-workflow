package com.flowgate.backend.workflow.service;

import com.flowgate.backend.workflow.dto.CreateRequestTypeRequest;
import com.flowgate.backend.workflow.dto.CreateWorkflowRequest;
import com.flowgate.backend.workflow.dto.CreateWorkflowStepRequest;
import com.flowgate.backend.workflow.dto.RequestTypeDto;
import com.flowgate.backend.workflow.dto.WorkflowDto;
import com.flowgate.backend.workflow.dto.WorkflowStepDto;
import com.flowgate.backend.workflow.entity.RequestType;
import com.flowgate.backend.workflow.entity.Workflow;
import com.flowgate.backend.workflow.entity.WorkflowStep;
import com.flowgate.backend.workflow.repository.RequestTypeRepository;
import com.flowgate.backend.workflow.repository.WorkflowRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class WorkflowServiceImpl implements WorkflowService {

    private final RequestTypeRepository requestTypeRepository;
    private final WorkflowRepository workflowRepository;

    public WorkflowServiceImpl(RequestTypeRepository requestTypeRepository, WorkflowRepository workflowRepository) {
        this.requestTypeRepository = requestTypeRepository;
        this.workflowRepository = workflowRepository;
    }

    @Override
    @Transactional
    public RequestTypeDto createRequestType(CreateRequestTypeRequest request) {
        if (requestTypeRepository.findByName(request.getName()).isPresent()) {
            throw new IllegalArgumentException("Request type already exists");
        }

        RequestType type = RequestType.builder()
                .name(request.getName())
                .description(request.getDescription())
                .active(true)
                .createdAt(OffsetDateTime.now())
                .build();

        return toDto(requestTypeRepository.save(type));
    }

    @Override
    public List<RequestTypeDto> getRequestTypes() {
        return requestTypeRepository.findAll().stream()
                .sorted(Comparator.comparing(RequestType::getCreatedAt).reversed())
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public WorkflowDto createWorkflow(UUID requestTypeId, CreateWorkflowRequest request) {
        RequestType requestType = requestTypeRepository.findById(requestTypeId)
                .orElseThrow(() -> new IllegalArgumentException("Request type not found"));

        if (request.getSteps() == null || request.getSteps().isEmpty()) {
            throw new IllegalArgumentException("A workflow requires at least one approval step");
        }

        Workflow workflow = Workflow.builder()
                .name(request.getName())
                .requestType(requestType)
                .active(true)
                .createdAt(OffsetDateTime.now())
                .build();

        List<WorkflowStep> steps = request.getSteps().stream()
                .sorted(Comparator.comparingInt(CreateWorkflowStepRequest::getOrderIndex))
                .map(stepRequest -> WorkflowStep.builder()
                        .workflow(workflow)
                        .name(stepRequest.getName())
                        .orderIndex(stepRequest.getOrderIndex())
                        .approverRole(stepRequest.getApproverRole())
                        .approverUserId(stepRequest.getApproverUserId())
                        .requiresComment(stepRequest.isRequiresComment())
                        .build())
                .toList();

        workflow.setSteps(steps);
        return toDto(workflowRepository.save(workflow));
    }

    @Override
    public List<WorkflowDto> getWorkflowsForType(UUID requestTypeId) {
        return workflowRepository.findAll().stream()
                .filter(workflow -> workflow.getRequestType() != null && workflow.getRequestType().getId().equals(requestTypeId))
                .sorted(Comparator.comparing(Workflow::getCreatedAt).reversed())
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<WorkflowDto> searchWorkflows(String query) {
        if (query == null || query.isBlank()) {
            return workflowRepository.findAll().stream()
                    .sorted(Comparator.comparing(Workflow::getCreatedAt).reversed())
                    .map(this::toDto)
                    .collect(Collectors.toList());
        }
        String q = query.toLowerCase();
        return workflowRepository.findAll().stream()
                .filter(w -> (w.getName() != null && w.getName().toLowerCase().contains(q))
                        || (w.getRequestType() != null && w.getRequestType().getName() != null && w.getRequestType().getName().toLowerCase().contains(q)))
                .sorted(Comparator.comparing(Workflow::getCreatedAt).reversed())
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RequestTypeDto updateRequestType(UUID id, CreateRequestTypeRequest request) {
        RequestType type = requestTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Request type not found"));
        type.setName(request.getName());
        type.setDescription(request.getDescription());
        RequestType saved = requestTypeRepository.save(type);
        return toDto(saved);
    }

    @Override
    @Transactional
    public void deleteRequestType(UUID id) {
        RequestType type = requestTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Request type not found"));
        // remove related workflows first
        List<Workflow> workflows = workflowRepository.findByRequestTypeId(id);
        if (workflows != null && !workflows.isEmpty()) {
            workflowRepository.deleteAll(workflows);
        }
        requestTypeRepository.delete(type);
    }

    @Override
    @Transactional
    public void deleteWorkflow(UUID workflowId) {
        workflowRepository.deleteById(workflowId);
    }

    private RequestTypeDto toDto(RequestType requestType) {
        return RequestTypeDto.builder()
                .id(requestType.getId())
                .name(requestType.getName())
                .description(requestType.getDescription())
                .active(requestType.isActive())
                .createdAt(requestType.getCreatedAt())
                .build();
    }

    private WorkflowDto toDto(Workflow workflow) {
        return WorkflowDto.builder()
                .id(workflow.getId())
                .name(workflow.getName())
                .requestTypeId(workflow.getRequestType() != null ? workflow.getRequestType().getId() : null)
                .active(workflow.isActive())
                .createdAt(workflow.getCreatedAt())
                .steps(workflow.getSteps() == null ? List.of() : workflow.getSteps().stream()
                        .sorted(Comparator.comparingInt(WorkflowStep::getOrderIndex))
                        .map(this::toDto)
                        .toList())
                .build();
    }

    private WorkflowStepDto toDto(WorkflowStep step) {
        return WorkflowStepDto.builder()
                .id(step.getId())
                .name(step.getName())
                .orderIndex(step.getOrderIndex())
                .approverRole(step.getApproverRole())
                .approverUserId(step.getApproverUserId())
                .requiresComment(step.isRequiresComment())
                .build();
    }
}
