package com.flowgate.backend.workflow.service;

import com.flowgate.backend.common.exception.ConflictException;
import com.flowgate.backend.common.exception.InvalidStateException;
import com.flowgate.backend.common.exception.NotFoundException;
import com.flowgate.backend.user.repository.RoleRepository;
import com.flowgate.backend.workflow.dto.CreateRequestTypeRequest;
import com.flowgate.backend.workflow.dto.CreateWorkflowRequest;
import com.flowgate.backend.workflow.dto.CreateWorkflowStepRequest;
import com.flowgate.backend.workflow.dto.RequestTypeDto;
import com.flowgate.backend.workflow.dto.WorkflowDto;
import com.flowgate.backend.workflow.dto.WorkflowStepDto;
import com.flowgate.backend.workflow.entity.RequestType;
import com.flowgate.backend.workflow.entity.Workflow;
import com.flowgate.backend.workflow.entity.WorkflowStep;
import com.flowgate.backend.workflow.repository.RequestRepository;
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
    private final RoleRepository roleRepository;
    private final RequestRepository requestRepository;

    public WorkflowServiceImpl(RequestTypeRepository requestTypeRepository, WorkflowRepository workflowRepository, RoleRepository roleRepository, RequestRepository requestRepository) {
        this.requestTypeRepository = requestTypeRepository;
        this.workflowRepository = workflowRepository;
        this.roleRepository = roleRepository;
        this.requestRepository = requestRepository;
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
    @Transactional(readOnly = true)
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
                .orElseThrow(() -> new NotFoundException("Request type not found"));

        if (request.getSteps() == null || request.getSteps().isEmpty()) {
            throw new InvalidStateException("A workflow requires at least one approval step");
        }

        List<CreateWorkflowStepRequest> sortedSteps = request.getSteps().stream()
                .sorted(Comparator.comparingInt(CreateWorkflowStepRequest::getOrderIndex))
                .toList();

        for (int i = 0; i < sortedSteps.size(); i++) {
            CreateWorkflowStepRequest stepRequest = sortedSteps.get(i);
            if (stepRequest.getOrderIndex() != i) {
                throw new InvalidStateException("Workflow step order indices must be contiguous starting at 0");
            }
            if (stepRequest.getApproverRole() != null && !stepRequest.getApproverRole().isBlank()) {
                String normalized = stepRequest.getApproverRole().trim();
                if (!roleRepository.findByName(normalized.startsWith("ROLE_") ? normalized : "ROLE_" + normalized).isPresent()) {
                    throw new InvalidStateException("approverRole must reference an existing role: " + stepRequest.getApproverRole());
                }
            }
        }

        List<Workflow> existing = workflowRepository.findByRequestTypeIdAndActiveTrue(requestTypeId);
        for (Workflow activeWorkflow : existing) {
            activeWorkflow.setActive(false);
        }

        Workflow workflow = Workflow.builder()
                .name(request.getName())
                .requestType(requestType)
                .active(true)
                .createdAt(OffsetDateTime.now())
                .build();

        List<WorkflowStep> steps = sortedSteps.stream()
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
    @Transactional(readOnly = true)
    public List<WorkflowDto> getWorkflowsForType(UUID requestTypeId) {
        return workflowRepository.findAll().stream()
                .filter(workflow -> workflow.getRequestType() != null && workflow.getRequestType().getId().equals(requestTypeId))
                .sorted(Comparator.comparing(Workflow::getCreatedAt).reversed())
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
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
                .orElseThrow(() -> new NotFoundException("Request type not found"));
        long requestCount = requestRepository.countByRequestType(type);
        if (requestCount > 0) {
            throw new ConflictException("Request type has requests; deactivate it instead");
        }
        requestTypeRepository.delete(type);
    }

    @Override
    @Transactional
    public void deleteWorkflow(UUID workflowId) {
        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new NotFoundException("Workflow not found"));
        long requestCount = requestRepository.countByWorkflow(workflow);
        if (requestCount > 0) {
            workflow.setActive(false);
            return;
        }
        workflowRepository.delete(workflow);
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
