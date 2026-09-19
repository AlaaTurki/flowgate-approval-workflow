package com.flowgate.backend.workflow.controller;

import com.flowgate.backend.user.entity.User;
import com.flowgate.backend.user.repository.UserRepository;
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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/requests")
public class RequestController {

    private final RequestService requestService;
    private final UserRepository userRepository;

    public RequestController(RequestService requestService, UserRepository userRepository) {
        this.requestService = requestService;
        this.userRepository = userRepository;
    }

    @PostMapping
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<RequestDto> createRequest(@Valid @RequestBody CreateRequestRequest request,
                                                  @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(requestService.createRequest(currentUser.getId(), request));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public List<RequestDto> myRequests(@AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return requestService.listRequestsForUser(currentUser.getId());
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public List<RequestDto> pendingApprovals(@AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return requestService.listRequestsForApproval(currentUser.getId());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN')")
    public RequestDetailDto getRequest(@PathVariable UUID id) {
        return requestService.getRequestDetail(id);
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
                                                     @AuthenticationPrincipal UserDetails userDetails) {
        // for simplicity allow owner or admin to update; service enforces closed-state rule
        RequestDto updated = requestService.updateRequest(id, requestBody);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
    public ResponseEntity<Void> deleteRequest(@PathVariable UUID id,
                                              @AuthenticationPrincipal UserDetails userDetails) {
        // basic delete; production should check ownership or admin privileges
        requestService.deleteRequest(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN')")
    public DashboardStatsDto getDashboardStats(@AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return requestService.getDashboardStats(currentUser.getId());
    }

    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public List<RequestDto> getHistory(@AuthenticationPrincipal UserDetails userDetails,
                                       @RequestParam(required = false) String requestTypeId,
                                       @RequestParam(required = false) String status,
                                       @RequestParam(required = false) String from,
                                       @RequestParam(required = false) String to,
                                       @RequestParam(required = false, defaultValue = "0") Integer page,
                                       @RequestParam(required = false, defaultValue = "20") Integer size) {
        User currentUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return requestService.getHistoryForUser(currentUser.getId(), requestTypeId, status, from, to, page, size);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public RequestDto approve(@PathVariable UUID id,
                             @Valid @RequestBody ApprovalDecisionRequest request,
                             @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return requestService.approveRequest(id, currentUser.getId(), request.getComment());
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public RequestDto reject(@PathVariable UUID id,
                             @Valid @RequestBody ApprovalDecisionRequest request,
                             @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return requestService.rejectRequest(id, currentUser.getId(), request.getComment());
    }
}
