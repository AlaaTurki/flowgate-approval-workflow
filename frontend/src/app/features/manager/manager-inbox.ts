import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { AuthService } from '../../core/auth.service';
import { FlowgateApiService, RequestDto } from '../../core/flowgate-api.service';

interface ApprovalItem {
  id: string;
  employee: string;
  request: string;
  type: string;
  amount: string;
  priority: 'High' | 'Normal';
  due: string;
}

@Component({
  selector: 'app-manager-inbox',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './manager-inbox.html',
  styleUrl: './manager-inbox.scss',
})
export class ManagerInboxComponent implements OnInit {
  readonly username: string;
  activeNav = 'Inbox';
  queue: ApprovalItem[] = [];

  constructor(
    private readonly authService: AuthService,
    private readonly api: FlowgateApiService,
  ) {
    this.username = this.authService.getUsername();
  }

  ngOnInit(): void {
    this.loadQueue();
  }

  selectNav(name: string): void {
    this.activeNav = name;
  }

  exportReview(): void {
    window.alert('Review export started.');
  }

  approve(item: ApprovalItem): void {
    const comment = window.prompt('Approval comment (optional)', 'Approved');
    this.api.approveRequest(item.id, comment ?? '').subscribe({
      next: () => this.loadQueue(),
      error: () => window.alert('Unable to approve this request.'),
    });
  }

  reject(item: ApprovalItem): void {
    const comment = window.prompt('Reason for rejection', 'Please review the details and resubmit.');
    if (!comment || !comment.trim()) {
      window.alert('A rejection comment is required.');
      return;
    }

    this.api.rejectRequest(item.id, comment).subscribe({
      next: () => this.loadQueue(),
      error: () => window.alert('Unable to reject this request.'),
    });
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
          amount: '$0.00',
          priority: 'Normal',
          due: item.updatedAt ? new Date(item.updatedAt).toLocaleDateString() : 'Today',
        }));
      },
      error: () => {
        this.queue = [];
      },
    });
  }
}
