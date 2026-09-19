import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-dashboard-page',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class DashboardPageComponent {
  readonly authService: AuthService;
  readonly username: string;
  readonly role: string;

  readonly stats: Array<{ label: string; value: string; trend: string }>;
  readonly quickActions: Array<{ label: string; tone: 'primary' | 'neutral'; description: string }>;
  readonly requests = [
    { title: 'Expense reimbursement', owner: 'Rania K.', status: 'In Review' },
    { title: 'Annual leave request', owner: 'Ammar H.', status: 'Approved' },
    { title: 'Equipment request', owner: 'Sara M.', status: 'Pending' },
    { title: 'Travel advance', owner: 'Leen A.', status: 'Rejected' },
  ];

  constructor(authService: AuthService) {
    this.authService = authService;
    this.username = this.authService.getUsername();
    this.role = this.authService.getCurrentRole();
    this.stats = this.getStats();
    this.quickActions = this.getQuickActions();
  }

  logout(): void {
    this.authService.logout();
  }

  private getStats(): Array<{ label: string; value: string; trend: string }> {
    switch (this.role) {
      case 'ADMIN':
        return [
          { label: 'Open workflows', value: '14', trend: '+2 this week' },
          { label: 'Approvals today', value: '32', trend: '+9%' },
          { label: 'Avg. resolution', value: '1.8d', trend: '-0.7d' },
        ];
      case 'MANAGER':
        return [
          { label: 'Pending approvals', value: '12', trend: '+3 this week' },
          { label: 'Approved', value: '87', trend: '+14%' },
          { label: 'Avg. SLA', value: '2.4d', trend: '-0.6d' },
        ];
      default:
        return [
          { label: 'My requests', value: '7', trend: '2 active' },
          { label: 'Approved', value: '5', trend: '+2 this month' },
          { label: 'In review', value: '2', trend: '1 urgent' },
        ];
    }
  }

  private getQuickActions(): Array<{ label: string; tone: 'primary' | 'neutral'; description: string }> {
    switch (this.role) {
      case 'ADMIN':
        return [
          { label: 'Add workflow', tone: 'primary', description: 'Create new approval steps' },
          { label: 'Manage users', tone: 'neutral', description: 'Review roles and access' },
        ];
      case 'MANAGER':
        return [
          { label: 'Review queue', tone: 'primary', description: 'Approve pending requests' },
          { label: 'Audit trail', tone: 'neutral', description: 'Check recent decisions' },
        ];
      default:
        return [
          { label: 'New request', tone: 'primary', description: 'Start a new approval flow' },
          { label: 'My history', tone: 'neutral', description: 'Check recent submissions' },
        ];
    }
  }
}
