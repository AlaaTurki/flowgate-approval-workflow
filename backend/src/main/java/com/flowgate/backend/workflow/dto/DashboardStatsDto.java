package com.flowgate.backend.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsDto {
    private long openRequests;
    private long approvedRequests;
    private long rejectedRequests;
    private long pendingApprovals;
    private double averageApprovalDays;
}
