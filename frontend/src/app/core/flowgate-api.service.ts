import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

export interface RequestTypeDto {
  id: string;
  name: string;
  description?: string;
  active: boolean;
  createdAt?: string;
}

export interface RequestDto {
  id: string;
  requestTypeId: string;
  requestTypeName: string;
  submittedById?: string;
  submittedByUsername?: string;
  title: string;
  description: string;
  status: 'DRAFT' | 'SUBMITTED' | 'IN_REVIEW' | 'APPROVED' | 'REJECTED' | 'CANCELLED';
  currentStepIndex: number;
  createdAt?: string;
  updatedAt?: string;
  resolvedAt?: string;
  lastComment?: string;
}

export interface CreateRequestPayload {
  requestTypeId: string;
  title: string;
  description: string;
}

export interface CreateWorkflowPayload {
  name: string;
  steps: CreateWorkflowStepPayload[];
}

export interface CreateWorkflowStepPayload {
  name: string;
  orderIndex: number;
  approverRole: string;
  approverUserId?: string | null;
  requiresComment: boolean;
}

export interface CreateRequestTypePayload {
  name: string;
  description?: string;
}

@Injectable({ providedIn: 'root' })
export class FlowgateApiService {
  private readonly baseUrl = 'http://localhost:9090';

  constructor(private readonly http: HttpClient) {}

  getRequestTypes() {
    return this.http.get<RequestTypeDto[]>(`${this.baseUrl}/api/workflows/request-types`);
  }

  createRequestType(payload: CreateRequestTypePayload) {
    return this.http.post<RequestTypeDto>(`${this.baseUrl}/api/workflows/request-types`, payload);
  }

  createWorkflow(requestTypeId: string, payload: CreateWorkflowPayload) {
    return this.http.post<any>(`${this.baseUrl}/api/workflows/request-types/${requestTypeId}`, payload);
  }

  createRequest(payload: CreateRequestPayload) {
    return this.http.post<RequestDto>(`${this.baseUrl}/api/requests`, payload);
  }

  getMyRequests() {
    return this.http.get<RequestDto[]>(`${this.baseUrl}/api/requests/mine`);
  }

  getPendingApprovals() {
    return this.http.get<RequestDto[]>(`${this.baseUrl}/api/requests/pending`);
  }

  approveRequest(requestId: string, comment?: string) {
    return this.http.post<RequestDto>(`${this.baseUrl}/api/requests/${requestId}/approve`, {
      comment: comment ?? '',
    });
  }

  rejectRequest(requestId: string, comment?: string) {
    return this.http.post<RequestDto>(`${this.baseUrl}/api/requests/${requestId}/reject`, {
      comment: comment ?? '',
    });
  }
}
