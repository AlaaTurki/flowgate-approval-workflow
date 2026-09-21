package com.flowgate.backend.workflow.controller;

import com.flowgate.backend.security.AuthenticatedUser;
import com.flowgate.backend.workflow.dto.ApprovalDecisionRequest;
import com.flowgate.backend.workflow.dto.CreateRequestRequest;
import com.flowgate.backend.workflow.dto.DashboardStatsDto;
import com.flowgate.backend.workflow.dto.RequestDetailDto;
import com.flowgate.backend.workflow.dto.RequestDto;
import com.flowgate.backend.workflow.service.RequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/requests")
public class RequestController {

    private final RequestService requestService;

    public RequestController(RequestService requestService) {
        this.requestService = requestService;
    }

    @PostMapping
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<RequestDto> createRequest(@Valid @RequestBody CreateRequestRequest request,
                                                 @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(requestService.createRequest(currentUser.getId(), request));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public List<RequestDto> myRequests(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return requestService.listRequestsForUser(currentUser.getId());
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public List<RequestDto> pendingApprovals(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return requestService.listRequestsForApproval(currentUser.getId());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN')")
    public RequestDetailDto getRequest(@PathVariable UUID id,
                                      @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return requestService.getRequestDetail(id, currentUser.getId());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public List<RequestDto> listAllRequests() {
        return requestService.listAllRequests();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
    public ResponseEntity<RequestDto> updateRequest(@PathVariable UUID id,
                                                 @Valid @RequestBody CreateRequestRequest requestBody,
                                                 @AuthenticationPrincipal AuthenticatedUser currentUser) {
        RequestDto updated = requestService.updateRequest(id, currentUser.getId(), requestBody);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
    public ResponseEntity<RequestDto> cancelRequest(@PathVariable UUID id,
                                                  @RequestBody(required = false) Map<String, String> payload,
                                                  @AuthenticationPrincipal AuthenticatedUser currentUser) {
        String comment = payload != null ? payload.getOrDefault("comment", "Cancelled") : "Cancelled";
        return ResponseEntity.ok(requestService.cancelRequest(id, currentUser.getId(), comment));
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN')")
    public DashboardStatsDto getDashboardStats(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return requestService.getDashboardStats(currentUser.getId());
    }

    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public List<RequestDto> getHistory(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                     @RequestParam(required = false) String requestTypeId,
                                     @RequestParam(required = false) String status,
                                     @RequestParam(required = false) String from,
                                     @RequestParam(required = false) String to,
                                     @RequestParam(required = false, defaultValue = "0") Integer page,
                                     @RequestParam(required = false, defaultValue = "20") Integer size) {
        return requestService.getHistoryForUser(currentUser.getId(), requestTypeId, status, from, to, page, size);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public RequestDto approve(@PathVariable UUID id,
                             @Valid @RequestBody ApprovalDecisionRequest request,
                             @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return requestService.approveRequest(id, currentUser.getId(), request.getComment());
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public RequestDto reject(@PathVariable UUID id,
                             @Valid @RequestBody ApprovalDecisionRequest request,
                             @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return requestService.rejectRequest(id, currentUser.getId(), request.getComment());
    }
}
