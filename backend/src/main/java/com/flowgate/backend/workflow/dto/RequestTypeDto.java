package com.flowgate.backend.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequestTypeDto {
    private UUID id;
    private String name;
    private String description;
    private boolean active;
    private OffsetDateTime createdAt;
}
