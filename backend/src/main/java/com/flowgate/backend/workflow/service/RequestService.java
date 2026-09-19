package com.flowgate.backend.workflow.service;

import com.flowgate.backend.workflow.dto.CreateRequestRequest;
import com.flowgate.backend.workflow.dto.RequestDto;

import java.util.List;
import java.util.UUID;

public interface RequestService {
    RequestDto createRequest(UUID employeeId, CreateRequestRequest request);
    List<RequestDto> listRequestsForUser(UUID userId);
    List<RequestDto> listRequestsForApproval(UUID approverId);
    RequestDto getRequest(UUID requestId);
    RequestDto approveRequest(UUID requestId, UUID actorId, String comment);
    RequestDto rejectRequest(UUID requestId, UUID actorId, String comment);
}
