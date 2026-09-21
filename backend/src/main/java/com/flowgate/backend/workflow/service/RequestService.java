package com.flowgate.backend.workflow.service;

import com.flowgate.backend.workflow.dto.CreateRequestRequest;
import com.flowgate.backend.workflow.dto.DashboardStatsDto;
import com.flowgate.backend.workflow.dto.RequestDetailDto;
import com.flowgate.backend.workflow.dto.RequestDto;

import java.util.List;
import java.util.UUID;

public interface RequestService {
    RequestDto createRequest(UUID employeeId, CreateRequestRequest request);
    List<RequestDto> listRequestsForUser(UUID userId);
    List<RequestDto> listRequestsForApproval(UUID approverId);
    RequestDetailDto getRequestDetail(UUID requestId);
    RequestDetailDto getRequestDetail(UUID requestId, UUID actorId);
    RequestDto getRequest(UUID requestId);
    RequestDto approveRequest(UUID requestId, UUID actorId, String comment);
    RequestDto rejectRequest(UUID requestId, UUID actorId, String comment);
    DashboardStatsDto getDashboardStats(UUID userId);

    // additional CRUD
    List<RequestDto> listAllRequests();
    RequestDto updateRequest(UUID requestId, UUID actorId, CreateRequestRequest request);
    RequestDto cancelRequest(UUID requestId, UUID actorId, String comment);
    void deleteRequest(UUID requestId);

    // history: requests where the user acted (approved/rejected/submitted)
    List<RequestDto> getHistoryForUser(UUID userId, String requestTypeId, String status, String from, String to, Integer page, Integer size);
}
