package com.flowgate.backend.workflow.service;

import com.flowgate.backend.common.exception.ForbiddenException;
import com.flowgate.backend.common.exception.InvalidStateException;
import com.flowgate.backend.common.exception.NotFoundException;
import com.flowgate.backend.common.exception.OptimisticLockException;
import com.flowgate.backend.user.entity.Role;
import com.flowgate.backend.user.entity.User;
import com.flowgate.backend.user.repository.UserRepository;
import com.flowgate.backend.workflow.dto.*;
import com.flowgate.backend.workflow.entity.*;
import com.flowgate.backend.workflow.repository.RequestRepository;
import com.flowgate.backend.workflow.repository.RequestTypeRepository;
import com.flowgate.backend.workflow.repository.WorkflowRepository;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RequestServiceImpl implements RequestService {

    private final RequestRepository requestRepository;
    private final RequestTypeRepository requestTypeRepository;
    private final WorkflowRepository workflowRepository;
    private final UserRepository userRepository;

    public RequestServiceImpl(RequestRepository requestRepository,
                             RequestTypeRepository requestTypeRepository,
                             WorkflowRepository workflowRepository,
                             UserRepository userRepository) {
        this.requestRepository = requestRepository;
        this.requestTypeRepository = requestTypeRepository;
        this.workflowRepository = workflowRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public RequestDto createRequest(UUID employeeId, CreateRequestRequest request) {
        User employee = userRepository.findById(employeeId)
                .orElseThrow(() -> new NotFoundException("Employee not found"));

        RequestType requestType = requestTypeRepository.findById(request.getRequestTypeId())
                .orElseThrow(() -> new NotFoundException("Request type not found"));

        Workflow workflow = workflowRepository.findFirstByRequestTypeIdAndActiveTrue(requestType.getId())
                .orElseThrow(() -> new NotFoundException("No active workflow configured for this request type"));

        if (workflow.getSteps() == null || workflow.getSteps().isEmpty()) {
            throw new InvalidStateException("Workflow has no approval steps configured");
        }

        Request newRequest = Request.builder()
                .requestType(requestType)
                .submittedBy(employee)
                .workflow(workflow)
                .title(request.getTitle())
                .description(request.getDescription())
                .amount(request.getAmount())
                .status(RequestStatus.IN_REVIEW)
                .currentStepIndex(0)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        ApprovalAction action = ApprovalAction.builder()
                .request(newRequest)
                .actor(employee)
                .step(getStepAt(newRequest.getWorkflow(), 0))
                .actionType(ApprovalActionType.SUBMITTED)
                .comment("Request submitted")
                .createdAt(OffsetDateTime.now())
                .build();

        newRequest.getActions().add(action);
        return toDto(requestRepository.save(newRequest));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RequestDto> listRequestsForUser(UUID userId) {
        User worker = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        return requestRepository.findBySubmittedBy(worker).stream()
                .sorted(Comparator.comparing(Request::getCreatedAt).reversed())
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RequestDto> listRequestsForApproval(UUID approverId) {
        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new NotFoundException("Approver not found"));

        return requestRepository.findByStatus(RequestStatus.IN_REVIEW).stream()
                .filter(request -> canAct(approver, request))
                .filter(request -> request.getSubmittedBy() != null && !request.getSubmittedBy().getId().equals(approverId))
                .sorted(Comparator.comparing(Request::getUpdatedAt).reversed())
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public RequestDetailDto getRequestDetail(UUID requestId) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Request not found"));
        return toDetailDto(request);
    }

    @Override
    @Transactional(readOnly = true)
    public RequestDetailDto getRequestDetail(UUID requestId, UUID actorId) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Request not found"));
        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        boolean isSubmitter = request.getSubmittedBy() != null && request.getSubmittedBy().getId().equals(actorId);
        boolean isAdmin = isAdmin(actor);
        boolean isWorkflowActor = request.getWorkflow() != null && request.getWorkflow().getSteps() != null && request.getWorkflow().getSteps().stream()
                .anyMatch(step -> step != null && step.getApproverUserId() != null && step.getApproverUserId().equals(actorId)
                        || step != null && actor.getRoles() != null && step.getApproverRole() != null && actor.getRoles().stream()
                        .map(Role::getName)
                        .filter(Objects::nonNull)
                        .map(r -> r.trim())
                        .map(r -> r.startsWith("ROLE_") ? r.substring(5) : r)
                        .anyMatch(r -> r.equalsIgnoreCase(step.getApproverRole().trim().replaceFirst("^ROLE_", ""))));
        if (!isSubmitter && !isAdmin && !isWorkflowActor) {
            throw new ForbiddenException("You are not allowed to view this request");
        }
        return toDetailDto(request);
    }

    @Override
    @Transactional(readOnly = true)
    public RequestDto getRequest(UUID requestId) {
        return toDto(requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found")));
    }

    @Override
    @Transactional
    public RequestDto approveRequest(UUID requestId, UUID actorId, String comment) {
        return processDecision(requestId, actorId, comment, ApprovalActionType.APPROVED, false);
    }

    @Override
    @Transactional
    public RequestDto rejectRequest(UUID requestId, UUID actorId, String comment) {
        return processDecision(requestId, actorId, comment, ApprovalActionType.REJECTED, true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RequestDto> listAllRequests() {
        return requestRepository.findAll().stream()
                .sorted(Comparator.comparing(Request::getCreatedAt).reversed())
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RequestDto updateRequest(UUID requestId, UUID actorId, CreateRequestRequest request) {
        Request existing = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Request not found"));

        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        boolean isOwner = existing.getSubmittedBy() != null && existing.getSubmittedBy().getId().equals(actorId);
        boolean isAdmin = isAdmin(actor);
        if (!isOwner && !isAdmin) {
            throw new ForbiddenException("Only the owner or admin may update this request");
        }

        if (existing.getStatus() != RequestStatus.IN_REVIEW) {
            throw new InvalidStateException("Cannot modify a request that is not in review");
        }
        if (existing.getCurrentStepIndex() != 0) {
            throw new InvalidStateException("Only the initial step can be edited");
        }

        existing.setTitle(request.getTitle());
        existing.setDescription(request.getDescription());
        existing.setAmount(request.getAmount());
        existing.setUpdatedAt(OffsetDateTime.now());

        try {
            return toDto(requestRepository.save(existing));
        } catch (OptimisticLockingFailureException ex) {
            throw new OptimisticLockException("request was modified, reload");
        }
    }

    @Override
    @Transactional
    public RequestDto cancelRequest(UUID requestId, UUID actorId, String comment) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Request not found"));

        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (request.getStatus() != RequestStatus.IN_REVIEW) {
            throw new InvalidStateException("Only an in-review request can be cancelled");
        }

        boolean isOwner = request.getSubmittedBy() != null && request.getSubmittedBy().getId().equals(actorId);
        boolean isAdmin = isAdmin(actor);
        if (!isOwner && !isAdmin) {
            throw new ForbiddenException("Only the request owner or an admin can cancel this request");
        }

        request.setStatus(RequestStatus.CANCELLED);
        request.setResolvedAt(OffsetDateTime.now());
        request.setUpdatedAt(OffsetDateTime.now());
        request.setLastComment(comment == null || comment.isBlank() ? "Cancelled by requester" : comment);
        request.getActions().add(ApprovalAction.builder()
                .request(request)
                .actor(actor)
                .step(getCurrentStep(request))
                .actionType(ApprovalActionType.CANCELLED)
                .comment(request.getLastComment())
                .createdAt(OffsetDateTime.now())
                .build());
        try {
            return toDto(requestRepository.save(request));
        } catch (OptimisticLockingFailureException ex) {
            throw new OptimisticLockException("request was modified, reload");
        }
    }

    @Override
    @Transactional
    public void deleteRequest(UUID requestId) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Request not found"));
        throw new InvalidStateException("Requests are never hard-deleted; use cancel instead");
    }

    private RequestDto processDecision(UUID requestId, UUID actorId, String comment, ApprovalActionType actionType, boolean rejection) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Request not found"));

        if (request.getStatus() != RequestStatus.IN_REVIEW) {
            throw new InvalidStateException("This request is not in review");
        }

        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (request.getSubmittedBy() != null && request.getSubmittedBy().getId().equals(actorId)) {
            throw new ForbiddenException("A user cannot approve or reject their own request");
        }

        Workflow workflow = request.getWorkflow();
        if (workflow == null || workflow.getSteps() == null || workflow.getSteps().isEmpty()) {
            throw new InvalidStateException("Request is not attached to a valid workflow snapshot");
        }

        List<WorkflowStep> steps = workflow.getSteps().stream()
                .sorted(Comparator.comparingInt(WorkflowStep::getOrderIndex))
                .toList();

        if (request.getCurrentStepIndex() >= steps.size()) {
            throw new InvalidStateException("Request has already reached the end of the workflow");
        }

        WorkflowStep step = steps.get(request.getCurrentStepIndex());
        if (!canAct(actor, request)) {
            throw new ForbiddenException("Only the current approval step can act on this request");
        }

        if (step.isRequiresComment() && actionType == ApprovalActionType.APPROVED && (comment == null || comment.isBlank())) {
            throw new IllegalArgumentException("A comment is required for this approval step");
        }
        if (rejection && (comment == null || comment.isBlank())) {
            throw new IllegalArgumentException("A rejection comment is required");
        }

        if (actionType == ApprovalActionType.APPROVED) {
            if (comment != null && !comment.isBlank()) {
                request.setLastComment(comment);
            }
            if (request.getCurrentStepIndex() == steps.size() - 1) {
                request.setStatus(RequestStatus.APPROVED);
                request.setResolvedAt(OffsetDateTime.now());
            } else {
                request.setStatus(RequestStatus.IN_REVIEW);
                request.setCurrentStepIndex(request.getCurrentStepIndex() + 1);
            }
        } else {
            request.setStatus(RequestStatus.REJECTED);
            request.setResolvedAt(OffsetDateTime.now());
            request.setLastComment(comment == null ? "Rejected" : comment);
        }

        request.setUpdatedAt(OffsetDateTime.now());
        request.getActions().add(ApprovalAction.builder()
                .request(request)
                .actor(actor)
                .step(step)
                .actionType(actionType)
                .comment(comment)
                .createdAt(OffsetDateTime.now())
                .build());

        try {
            return toDto(requestRepository.save(request));
        } catch (OptimisticLockingFailureException ex) {
            throw new OptimisticLockException("request was modified, reload");
        }
    }

    private boolean canAct(User actor, Request request) {
        if (request == null || actor == null || request.getStatus() != RequestStatus.IN_REVIEW) {
            return false;
        }
        if (request.getSubmittedBy() != null && request.getSubmittedBy().getId().equals(actor.getId())) {
            return false;
        }
        Workflow workflow = request.getWorkflow();
        if (workflow == null || workflow.getSteps() == null || workflow.getSteps().isEmpty()) {
            return false;
        }
        List<WorkflowStep> steps = workflow.getSteps().stream()
                .sorted(Comparator.comparingInt(WorkflowStep::getOrderIndex))
                .toList();

        if (request.getCurrentStepIndex() < 0 || request.getCurrentStepIndex() >= steps.size()) {
            return false;
        }

        WorkflowStep step = steps.get(request.getCurrentStepIndex());
        if (isAdmin(actor)) {
            return true;
        }
        if (step.getApproverUserId() != null) {
            return step.getApproverUserId().equals(actor.getId());
        }
        if (actor.getRoles() == null || step.getApproverRole() == null || step.getApproverRole().isBlank()) {
            return false;
        }

        final String normalizedStep = step.getApproverRole().trim();
        final String normalizedStepValue = normalizedStep.startsWith("ROLE_") ? normalizedStep.substring(5) : normalizedStep;

        return actor.getRoles().stream()
                .map(Role::getName)
                .filter(Objects::nonNull)
                .map(roleName -> roleName.trim())
                .map(roleName -> roleName.startsWith("ROLE_") ? roleName.substring(5) : roleName)
                .anyMatch(normalizedActor -> normalizedActor.equalsIgnoreCase(normalizedStepValue));
    }

    private boolean isAdmin(User actor) {
        return actor != null && actor.getRoles() != null && actor.getRoles().stream()
                .map(Role::getName)
                .filter(Objects::nonNull)
                .anyMatch(name -> "ROLE_ADMIN".equalsIgnoreCase(name.trim()));
    }

    private WorkflowStep getCurrentStep(Request request) {
        if (request == null || request.getWorkflow() == null || request.getWorkflow().getSteps() == null) {
            return null;
        }
        List<WorkflowStep> steps = request.getWorkflow().getSteps().stream()
                .sorted(Comparator.comparingInt(WorkflowStep::getOrderIndex))
                .toList();
        if (request.getCurrentStepIndex() < 0 || request.getCurrentStepIndex() >= steps.size()) {
            return null;
        }
        return steps.get(request.getCurrentStepIndex());
    }

    private WorkflowStep getStepAt(Workflow workflow, int index) {
        if (workflow == null || workflow.getSteps() == null) {
            return null;
        }
        return workflow.getSteps().stream()
                .sorted(Comparator.comparingInt(WorkflowStep::getOrderIndex))
                .skip(index)
                .findFirst()
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RequestDto> getHistoryForUser(UUID userId, String requestTypeId, String status, String from, String to, Integer page, Integer size) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // stream all requests where the user acted
        java.util.stream.Stream<Request> stream = requestRepository.findAll().stream()
                .filter(req -> req.getActions() != null && req.getActions().stream().anyMatch(a -> a.getActor() != null && user.getId().equals(a.getActor().getId())));

        // filter by requestTypeId if provided
        if (requestTypeId != null && !requestTypeId.isBlank()) {
            try {
                java.util.UUID rtId = java.util.UUID.fromString(requestTypeId);
                stream = stream.filter(r -> r.getRequestType() != null && rtId.equals(r.getRequestType().getId()));
            } catch (IllegalArgumentException e) {
                // ignore invalid uuid and return empty
                return List.of();
            }
        }

        // filter by status if provided
        if (status != null && !status.isBlank()) {
            try {
                RequestStatus rs = RequestStatus.valueOf(status.toUpperCase());
                stream = stream.filter(r -> r.getStatus() == rs);
            } catch (IllegalArgumentException e) {
                // invalid status - ignore the filter
            }
        }

        // filter by date range (createdAt)
        final java.time.OffsetDateTime[] range = new java.time.OffsetDateTime[2];
        try {
            if (from != null && !from.isBlank()) {
                java.time.LocalDate ld = java.time.LocalDate.parse(from);
                range[0] = ld.atStartOfDay().atOffset(java.time.ZoneOffset.UTC);
            }
            if (to != null && !to.isBlank()) {
                java.time.LocalDate ld2 = java.time.LocalDate.parse(to);
                range[1] = ld2.plusDays(1).atStartOfDay().atOffset(java.time.ZoneOffset.UTC).minusNanos(1);
            }
        } catch (java.time.format.DateTimeParseException e) {
            // ignore invalid dates
        }

        if (range[0] != null) stream = stream.filter(r -> r.getCreatedAt() != null && !r.getCreatedAt().isBefore(range[0]));
        if (range[1] != null) stream = stream.filter(r -> r.getCreatedAt() != null && !r.getCreatedAt().isAfter(range[1]));

        // sorting
        stream = stream.sorted(Comparator.comparing(
                request -> request.getUpdatedAt() != null ? request.getUpdatedAt() : request.getCreatedAt(),
                Comparator.nullsLast(Comparator.reverseOrder())
        ));

        // pagination
        int p = (page != null && page >= 0) ? page : 0;
        int s = (size != null && size > 0) ? size : 20;
        return stream.skip((long) p * s).limit(s).map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardStatsDto getDashboardStats(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<Request> userRequests = requestRepository.findBySubmittedBy(user);
        List<Request> inReview = requestRepository.findByStatus(RequestStatus.IN_REVIEW);

        long openRequests = userRequests.stream()
                .filter(request -> request.getStatus() != RequestStatus.APPROVED && request.getStatus() != RequestStatus.REJECTED)
                .count();
        long approvedRequests = userRequests.stream()
                .filter(request -> request.getStatus() == RequestStatus.APPROVED)
                .count();
        long rejectedRequests = userRequests.stream()
                .filter(request -> request.getStatus() == RequestStatus.REJECTED)
                .count();
        long pendingApprovals = inReview.stream()
                .filter(request -> canAct(user, request))
                .count();

        double averageApprovalDays = userRequests.stream()
                .filter(request -> request.getStatus() == RequestStatus.APPROVED || request.getStatus() == RequestStatus.REJECTED)
                .mapToDouble(request -> {
                    if (request.getCreatedAt() == null || request.getResolvedAt() == null) {
                        return 0d;
                    }
                    return Duration.between(request.getCreatedAt().toInstant(), request.getResolvedAt().toInstant()).toHours() / 24.0;
                })
                .average()
                .orElse(0d);

        return DashboardStatsDto.builder()
                .openRequests(openRequests)
                .approvedRequests(approvedRequests)
                .rejectedRequests(rejectedRequests)
                .pendingApprovals(pendingApprovals)
                .averageApprovalDays(averageApprovalDays)
                .build();
    }

    private RequestDto toDto(Request request) {
        // determine current approver info if possible
        String approverRole = null;
        java.util.UUID approverUserId = null;
        String approverUsername = null;
        Workflow wf = request.getWorkflow();
        if (wf != null && wf.getSteps() != null && request.getCurrentStepIndex() >= 0 && request.getCurrentStepIndex() < wf.getSteps().size()) {
            java.util.List<WorkflowStep> steps = wf.getSteps().stream().sorted(java.util.Comparator.comparingInt(WorkflowStep::getOrderIndex)).toList();
            if (request.getCurrentStepIndex() < steps.size()) {
                WorkflowStep step = steps.get(request.getCurrentStepIndex());
                approverRole = step.getApproverRole();
                approverUserId = step.getApproverUserId();
                if (approverUserId != null) {
                    approverUsername = userRepository.findById(approverUserId).map(User::getUsername).orElse(null);
                }
            }
        }

        return RequestDto.builder()
                .id(request.getId())
                .requestTypeId(request.getRequestType() != null ? request.getRequestType().getId() : null)
                .requestTypeName(request.getRequestType() != null ? request.getRequestType().getName() : null)
                .submittedById(request.getSubmittedBy() != null ? request.getSubmittedBy().getId() : null)
                .submittedByUsername(request.getSubmittedBy() != null ? request.getSubmittedBy().getUsername() : null)
                .title(request.getTitle())
                .description(request.getDescription())
                .status(request.getStatus())
                .currentStepIndex(request.getCurrentStepIndex())
                .currentApproverRole(approverRole)
                .currentApproverUserId(approverUserId)
                .currentApproverUsername(approverUsername)
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .resolvedAt(request.getResolvedAt())
                .lastComment(request.getLastComment())
                .amount(request.getAmount())
                .build();
    }

    private RequestDetailDto toDetailDto(Request request) {
        String approverRole = null;
        java.util.UUID approverUserId = null;
        String approverUsername = null;
        Workflow wf = request.getWorkflow();
        if (wf != null && wf.getSteps() != null && request.getCurrentStepIndex() >= 0 && request.getCurrentStepIndex() < wf.getSteps().size()) {
            List<WorkflowStep> steps = wf.getSteps().stream().sorted(Comparator.comparingInt(WorkflowStep::getOrderIndex)).toList();
            if (request.getCurrentStepIndex() < steps.size()) {
                WorkflowStep step = steps.get(request.getCurrentStepIndex());
                approverRole = step.getApproverRole();
                approverUserId = step.getApproverUserId();
                if (approverUserId != null) {
                    approverUsername = userRepository.findById(approverUserId).map(User::getUsername).orElse(null);
                }
            }
        }

        return RequestDetailDto.builder()
                .id(request.getId())
                .requestTypeId(request.getRequestType() != null ? request.getRequestType().getId() : null)
                .requestTypeName(request.getRequestType() != null ? request.getRequestType().getName() : null)
                .submittedById(request.getSubmittedBy() != null ? request.getSubmittedBy().getId() : null)
                .submittedByUsername(request.getSubmittedBy() != null ? request.getSubmittedBy().getUsername() : null)
                .title(request.getTitle())
                .description(request.getDescription())
                .status(request.getStatus())
                .currentStepIndex(request.getCurrentStepIndex())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .resolvedAt(request.getResolvedAt())
                .lastComment(request.getLastComment())
                .amount(request.getAmount())
                .currentApproverRole(approverRole)
                .currentApproverUserId(approverUserId)
                .currentApproverUsername(approverUsername)
                .actions(request.getActions() == null ? List.of() : request.getActions().stream()
                        .sorted(Comparator.comparing(ApprovalAction::getCreatedAt))
                        .map(action -> ApprovalActionDto.builder()
                                .id(action.getId())
                                .actorName(action.getActor() != null ? action.getActor().getUsername() : "System")
                                .actionType(action.getActionType() != null ? action.getActionType().name() : "UNKNOWN")
                                .comment(action.getComment())
                                .createdAt(action.getCreatedAt())
                                .build())
                        .toList())
                .build();
    }
}
