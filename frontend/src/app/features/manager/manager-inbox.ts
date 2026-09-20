import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/auth.service';
import { FlowgateApiService, RequestDto, RequestDetailDto } from '../../core/flowgate-api.service';

interface ApprovalItem {
  id: string;
  employee: string;
  request: string;
  type: string;
  amount: string;
  approverLabel?: string;
  priority: 'High' | 'Normal';
  due: string;
}

@Component({
  selector: 'app-manager-inbox',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './manager-inbox.html',
  styleUrl: './manager-inbox.scss',
})
export class ManagerInboxComponent implements OnInit {
  readonly username: string;
  activeNav = 'Inbox';
  queue: ApprovalItem[] = [];
  selectedDetail: RequestDetailDto | null = null;
  readonly stats = {
   pending: 0,
   approvedToday: 0,
   avgSla: '0.0d',
  };
  workflowTemplates: Array<{ id: string; name: string }> = [];

  constructor(
   private readonly authService: AuthService,
   private readonly api: FlowgateApiService,
  ) {
   this.username = this.authService.getUsername();
  }

  get visibleQueue(): ApprovalItem[] {
   if (this.activeNav === 'Team requests') {
     return this.queue.slice(0, Math.max(1, Math.min(this.queue.length, 3)));
   }
   return this.queue;
  }

  get reportCards(): Array<{ label: string; value: string; hint: string }> {
   return [
     { label: 'Open approvals', value: String(this.stats.pending), hint: 'requests awaiting action' },
     { label: 'Approved today', value: String(this.stats.approvedToday), hint: 'decisions completed' },
     { label: 'SLA average', value: this.stats.avgSla, hint: 'turnaround target' },
   ];
  }

  history: RequestDto[] = [];
  filter = {
   requestTypeId: '',
   status: '',
   from: '',
   to: '',
  };
  page = 0;
  size = 10;

  ngOnInit(): void {
   this.loadQueue();
   this.loadDashboardStats();
   this.loadRequestTypes();
   this.loadHistory();
  }

  private loadRequestTypes(): void {
   this.api.getRequestTypes().subscribe({
     next: (types) => {
       this.workflowTemplates = types.map((type) => ({ id: type.id, name: type.name }));
     },
     error: () => {
       this.workflowTemplates = [];
     },
   });
  }

  private loadHistory(): void {
   this.api.getManagerHistory({
     requestTypeId: this.filter.requestTypeId || undefined,
     status: this.filter.status || undefined,
     from: this.filter.from || undefined,
     to: this.filter.to || undefined,
     page: this.page,
     size: this.size,
   }).subscribe({
     next: (items) => {
       this.history = items;
     },
     error: () => {
       this.history = [];
     },
   });
  }

  applyHistoryFilters(): void {
   this.page = 0;
   this.loadHistory();
  }

  clearHistoryFilters(): void {
   this.filter = { requestTypeId: '', status: '', from: '', to: '' };
   this.page = 0;
   this.loadHistory();
  }

  prevPage(): void {
   if (this.page > 0) {
     this.page--;
     this.loadHistory();
   }
  }

  nextPage(): void {
   this.page++;
   this.loadHistory();
  }

  selectNav(name: string): void {
   this.activeNav = name;
  }

  exportReview(): void {
   console.info('Review export requested.');
  }

  approve(item: ApprovalItem): void {
   const comment = window.prompt('Approval comment (optional)', 'Approved');
   this.api.approveRequest(item.id, comment ?? '').subscribe({
     next: () => {
       this.loadQueue();
       this.loadDashboardStats();
       this.loadHistory();
     },
     error: () => console.error('Unable to approve this request.'),
   });
  }

  approveById(id: string): void {
   const comment = window.prompt('Approval comment (optional)', 'Approved');
   if (!id) return;
   this.api.approveRequest(id, comment ?? '').subscribe({
     next: () => {
       this.loadQueue();
       this.loadDashboardStats();
       this.loadHistory();
       this.viewDetail(id);
     },
     error: () => console.error('Unable to approve this request.'),
   });
  }

  reject(item: ApprovalItem): void {
   const comment = window.prompt('Reason for rejection', 'Please review the details and resubmit.');
   if (!comment || !comment.trim()) {
     console.warn('A rejection comment is required.');
     return;
   }

   this.api.rejectRequest(item.id, comment).subscribe({
     next: () => {
       this.loadQueue();
       this.loadDashboardStats();
       this.loadHistory();
     },
     error: () => console.error('Unable to reject this request.'),
   });
  }

  rejectById(id: string): void {
   const comment = window.prompt('Reason for rejection', 'Please review the details and resubmit.');
   if (!comment || !comment.trim()) {
     console.warn('A rejection comment is required.');
     return;
   }
   if (!id) return;
   this.api.rejectRequest(id, comment).subscribe({
     next: () => {
       this.loadQueue();
       this.loadDashboardStats();
       this.loadHistory();
       this.viewDetail(id);
     },
     error: () => console.error('Unable to reject this request.'),
   });
  }

  viewDetail(id: string): void {
   this.selectedDetail = null;
   this.api.getRequestDetail(id).subscribe({
     next: (detail) => {
       this.selectedDetail = detail;
     },
     error: () => console.error('Unable to load request details.'),
   });
  }

  closeDetail(): void {
   this.selectedDetail = null;
  }

  logout(): void {
   this.authService.logout();
  }

  private loadQueue(): void {
   this.api.getPendingApprovals().subscribe({
     next: (items) => {
       this.queue = items.map((item) => ({
         id: item.id,
         employee: item.submittedByUsername ?? 'Employee',
         request: item.title,
         type: item.requestTypeName,
         amount: item.amount != null ? `$${Number(item.amount).toFixed(2)}` : '$0.00',
         approverLabel: item.currentApproverUsername ? item.currentApproverUsername : (item.currentApproverRole ?? '—'),
         priority: 'Normal',
         due: item.updatedAt ? new Date(item.updatedAt).toLocaleDateString() : 'Today',
       }));
       this.stats.pending = this.queue.length;
     },
     error: () => {
       this.queue = [];
       this.stats.pending = 0;
     },
   });
  }

  private loadDashboardStats(): void {
   this.api.getDashboardStats().subscribe({
     next: (stats) => {
       this.stats.approvedToday = Number(stats.approvedRequests ?? 0);
       this.stats.avgSla = `${Number(stats.averageApprovalDays ?? 0).toFixed(1)}d`;
     },
     error: () => {
       this.stats.approvedToday = 0;
       this.stats.avgSla = '0.0d';
     },
   });
  }
}
