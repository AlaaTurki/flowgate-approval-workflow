package com.flowgate.backend.workflow.service;

import com.flowgate.backend.user.entity.Role;
import com.flowgate.backend.user.entity.User;
import com.flowgate.backend.user.repository.UserRepository;
import com.flowgate.backend.workflow.dto.*;
import com.flowgate.backend.workflow.entity.*;
import com.flowgate.backend.workflow.repository.RequestRepository;
import com.flowgate.backend.workflow.repository.RequestTypeRepository;
import com.flowgate.backend.workflow.repository.WorkflowRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
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
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));

        RequestType requestType = requestTypeRepository.findById(request.getRequestTypeId())
                .orElseThrow(() -> new IllegalArgumentException("Request type not found"));

        Workflow workflow = workflowRepository.findFirstByRequestTypeIdAndActiveTrue(requestType.getId())
                .orElseThrow(() -> new IllegalArgumentException("No active workflow configured for this request type"));

        if (workflow.getSteps() == null || workflow.getSteps().isEmpty()) {
            throw new IllegalArgumentException("Workflow has no approval steps configured");
        }

        Request newRequest = Request.builder()
                .requestType(requestType)
                .submittedBy(employee)
                .title(request.getTitle())
                .description(request.getDescription())
                .status(RequestStatus.SUBMITTED)
                .currentStepIndex(0)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        ApprovalAction action = ApprovalAction.builder()
                .request(newRequest)
                .actor(employee)
                .actionType(ApprovalActionType.SUBMITTED)
                .comment("Request submitted")
                .createdAt(OffsetDateTime.now())
                .build();

        newRequest.getActions().add(action);
        newRequest.setStatus(RequestStatus.IN_REVIEW);

        return toDto(requestRepository.save(newRequest));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RequestDto> listRequestsForUser(UUID userId) {
        User worker = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return requestRepository.findBySubmittedBy(worker).stream()
                .sorted(Comparator.comparing(Request::getCreatedAt).reversed())
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RequestDto> listRequestsForApproval(UUID approverId) {
        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new IllegalArgumentException("Approver not found"));

        return requestRepository.findByStatus(RequestStatus.IN_REVIEW).stream()
                .filter(request -> canAct(approver, request))
                .sorted(Comparator.comparing(Request::getUpdatedAt).reversed())
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public RequestDetailDto getRequestDetail(UUID requestId) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));
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
    public RequestDto updateRequest(UUID requestId, CreateRequestRequest request) {
        Request existing = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));

        if (existing.getStatus() == RequestStatus.APPROVED || existing.getStatus() == RequestStatus.REJECTED || existing.getStatus() == RequestStatus.CANCELLED) {
            throw new IllegalStateException("Cannot modify a closed request");
        }

        existing.setTitle(request.getTitle());
        existing.setDescription(request.getDescription());
        existing.setUpdatedAt(OffsetDateTime.now());

        return toDto(requestRepository.save(existing));
    }

    @Override
    @Transactional
    public void deleteRequest(UUID requestId) {
        requestRepository.deleteById(requestId);
    }

    private RequestDto processDecision(UUID requestId, UUID actorId, String comment, ApprovalActionType actionType, boolean rejection) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));

        if (request.getStatus() == RequestStatus.APPROVED || request.getStatus() == RequestStatus.REJECTED || request.getStatus() == RequestStatus.CANCELLED) {
            throw new IllegalStateException("This request is already closed");
        }

        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Workflow workflow = workflowRepository.findFirstByRequestTypeIdAndActiveTrue(request.getRequestType().getId())
                .orElseThrow(() -> new IllegalArgumentException("No workflow configured for the request type"));

        List<WorkflowStep> steps = workflow.getSteps().stream()
                .sorted(Comparator.comparingInt(WorkflowStep::getOrderIndex))
                .toList();

        if (request.getCurrentStepIndex() >= steps.size()) {
            throw new IllegalStateException("Request has already reached the end of the workflow");
        }

        WorkflowStep step = steps.get(request.getCurrentStepIndex());
        if (!canAct(actor, step)) {
            throw new AccessDeniedException("Only the current approval step can act on this request");
        }

        if (rejection && (comment == null || comment.isBlank())) {
            throw new IllegalArgumentException("A rejection comment is required");
        }

        if (actionType == ApprovalActionType.APPROVED) {
            if (comment != null && !comment.isBlank() && step.isRequiresComment()) {
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
            request.setLastComment(comment);
        }

        request.setUpdatedAt(OffsetDateTime.now());
        request.getActions().add(ApprovalAction.builder()
                .request(request)
                .actor(actor)
                .actionType(actionType)
                .comment(comment)
                .createdAt(OffsetDateTime.now())
                .build());

        return toDto(requestRepository.save(request));
    }

    private boolean canAct(User actor, Request request) {
        Workflow workflow = workflowRepository.findFirstByRequestTypeIdAndActiveTrue(request.getRequestType().getId())
                .orElse(null);
        if (workflow == null || workflow.getSteps() == null || workflow.getSteps().isEmpty()) {
            return false;
        }
        List<WorkflowStep> steps = workflow.getSteps().stream()
                .sorted(Comparator.comparingInt(WorkflowStep::getOrderIndex))
                .toList();

        if (request.getCurrentStepIndex() >= steps.size()) {
            return false;
        }

        return canAct(actor, steps.get(request.getCurrentStepIndex()));
    }

    private boolean canAct(User actor, WorkflowStep step) {
        if (actor == null || actor.getRoles() == null) {
            return false;
        }

        boolean hasRequiredRole = actor.getRoles().stream()
                .map(Role::getName)
                .anyMatch(roleName -> roleName.equalsIgnoreCase(step.getApproverRole())
                        || roleName.equalsIgnoreCase("ROLE_ADMIN"));

        if (step.getApproverUserId() != null) {
            return step.getApproverUserId().equals(actor.getId()) || hasRequiredRole;
        }

        return hasRequiredRole;
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
                    return Duration.between(request.getCreatedAt().toInstant(), request.getResolvedAt().toInstant()).toDays();
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
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .resolvedAt(request.getResolvedAt())
                .lastComment(request.getLastComment())
                .build();
    }

    private RequestDetailDto toDetailDto(Request request) {
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
