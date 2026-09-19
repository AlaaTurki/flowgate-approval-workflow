import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

export interface RequestTypeDto {
  id: string;
  name: string;
  description?: string;
  active: boolean;
  createdAt?: string;
}

export interface WorkflowStepDto {
  id: string;
  name: string;
  orderIndex: number;
  approverRole?: string;
  approverUserId?: string | null;
  requiresComment?: boolean;
}

export interface WorkflowDto {
  id: string;
  name: string;
  requestTypeId?: string | null;
  active?: boolean;
  createdAt?: string;
  steps?: WorkflowStepDto[];
}

export interface UserDto {
  id: string;
  username: string;
  email: string;
  fullName: string;
  enabled: boolean;
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

export interface ApprovalActionDto {
  id: string;
  actorName: string;
  actionType: string;
  comment?: string;
  createdAt?: string;
}

export interface RequestDetailDto extends RequestDto {
  actions: ApprovalActionDto[];
}

export interface DashboardStatsDto {
  openRequests: number;
  approvedRequests: number;
  rejectedRequests: number;
  pendingApprovals: number;
  averageApprovalDays: number;
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

  getUsers() {
    return this.http.get<UserDto[]>(`${this.baseUrl}/api/users`);
  }

  createUser(payload: Partial<UserDto> & { password: string; roles?: string[] }) {
    return this.http.post<UserDto>(`${this.baseUrl}/api/users`, payload);
  }

  updateUser(id: string, payload: Partial<UserDto>) {
    return this.http.put<UserDto>(`${this.baseUrl}/api/users/${id}`, payload);
  }

  deleteUser(id: string) {
    return this.http.delete<void>(`${this.baseUrl}/api/users/${id}`);
  }

  createRequestType(payload: CreateRequestTypePayload) {
    return this.http.post<RequestTypeDto>(`${this.baseUrl}/api/workflows/request-types`, payload);
  }

  updateRequestType(id: string, payload: CreateRequestTypePayload) {
    return this.http.put<RequestTypeDto>(`${this.baseUrl}/api/workflows/request-types/${id}`, payload);
  }

  deleteRequestType(id: string) {
    return this.http.delete<void>(`${this.baseUrl}/api/workflows/request-types/${id}`);
  }

  createWorkflow(requestTypeId: string, payload: CreateWorkflowPayload) {
    return this.http.post<any>(`${this.baseUrl}/api/workflows/request-types/${requestTypeId}`, payload);
  }

  deleteWorkflow(id: string) {
    return this.http.delete<void>(`${this.baseUrl}/api/workflows/workflows/${id}`);
  }

  searchWorkflows(query?: string) {
    const params = query ? `?query=${encodeURIComponent(query)}` : '';
    return this.http.get<WorkflowDto[]>(`${this.baseUrl}/api/workflows/workflows${params}`);
  }

  createRequest(payload: CreateRequestPayload) {
    return this.http.post<RequestDto>(`${this.baseUrl}/api/requests`, payload);
  }

  getMyRequests() {
    return this.http.get<RequestDto[]>(`${this.baseUrl}/api/requests/mine`);
  }

  getRequestDetail(requestId: string) {
    return this.http.get<RequestDetailDto>(`${this.baseUrl}/api/requests/${requestId}`);
  }

  updateRequest(requestId: string, payload: CreateRequestPayload) {
    return this.http.put<RequestDto>(`${this.baseUrl}/api/requests/${requestId}`, payload);
  }

  deleteRequest(requestId: string) {
    return this.http.delete<void>(`${this.baseUrl}/api/requests/${requestId}`);
  }

  getDashboardStats() {
    return this.http.get<DashboardStatsDto>(`${this.baseUrl}/api/requests/dashboard`);
  }

  getPendingApprovals() {
    return this.http.get<RequestDto[]>(`${this.baseUrl}/api/requests/pending`);
  }

  getManagerHistory(params?: { requestTypeId?: string; status?: string; from?: string; to?: string; page?: number; size?: number }) {
    const qs: string[] = [];
    if (params) {
      if (params.requestTypeId) qs.push(`requestTypeId=${encodeURIComponent(params.requestTypeId)}`);
      if (params.status) qs.push(`status=${encodeURIComponent(params.status)}`);
      if (params.from) qs.push(`from=${encodeURIComponent(params.from)}`);
      if (params.to) qs.push(`to=${encodeURIComponent(params.to)}`);
      if (params.page != null) qs.push(`page=${params.page}`);
      if (params.size != null) qs.push(`size=${params.size}`);
    }
    const q = qs.length ? `?${qs.join('&')}` : '';
    return this.http.get<RequestDto[]>(`${this.baseUrl}/api/requests/history${q}`);
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
