import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../core/auth.service';
import { FlowgateApiService, RequestDto, RequestTypeDto } from '../../core/flowgate-api.service';

interface EmployeeRequest {
  id: string;
  title: string;
  type: string;
  amount: string;
  status: 'Draft' | 'In review' | 'Approved' | 'Rejected';
  submitted: string;
}

@Component({
  selector: 'app-employee-dashboard',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './employee-dashboard.html',
  styleUrl: './employee-dashboard.scss',
})
export class EmployeeDashboardComponent implements OnInit {
  readonly username: string;
  readonly role: string;
  readonly form: FormGroup<{
    title: FormControl<string>;
    requestTypeId: FormControl<string>;
    amount: FormControl<string>;
    description: FormControl<string>;
  }>;

  activeNav = 'My requests';
  requestTypes: RequestTypeDto[] = [];
  requests: EmployeeRequest[] = [];
  isLoading = false;

  constructor(
    private readonly fb: FormBuilder,
    private readonly authService: AuthService,
    private readonly api: FlowgateApiService,
  ) {
    this.username = this.authService.getUsername();
    this.role = this.authService.getCurrentRole();
    this.form = this.fb.nonNullable.group({
      title: ['', Validators.required],
      requestTypeId: ['', Validators.required],
      amount: ['250.00', Validators.required],
      description: ['', Validators.required],
    });
  }

  ngOnInit(): void {
    this.loadRequestTypes();
    this.loadRequests();
  }

  selectNav(name: string): void {
    this.activeNav = name;
  }

  focusNewRequest(): void {
    document.getElementById('new-request-form')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const { title, requestTypeId, description } = this.form.getRawValue() as {
      title: string;
      requestTypeId: string;
      description: string;
    };

    this.isLoading = true;
    this.api.createRequest({ requestTypeId, title, description }).subscribe({
      next: () => {
        this.form.reset({
          title: '',
          requestTypeId: this.requestTypes[0]?.id ?? '',
          amount: '250.00',
          description: '',
        });
        this.loadRequests();
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
        window.alert('Unable to submit the request.');
      },
    });
  }

  logout(): void {
    this.authService.logout();
  }

  private loadRequestTypes(): void {
    this.api.getRequestTypes().subscribe({
      next: (types) => {
        this.requestTypes = types;
        if (types.length && !this.form.get('requestTypeId')?.value) {
          this.form.patchValue({ requestTypeId: types[0].id });
        }
      },
      error: () => {
        this.requestTypes = [];
      },
    });
  }

  private loadRequests(): void {
    this.api.getMyRequests().subscribe({
      next: (apiRequests) => {
        this.requests = apiRequests.map((item) => this.mapRequest(item));
      },
      error: () => {
        this.requests = [];
      },
    });
  }

  private mapRequest(item: RequestDto): EmployeeRequest {
    return {
      id: item.id.slice(0, 8).toUpperCase(),
      title: item.title,
      type: item.requestTypeName,
      amount: '$0.00',
      status: this.toStatusLabel(item.status),
      submitted: item.createdAt ? new Date(item.createdAt).toLocaleDateString() : 'Recently',
    };
  }

  private toStatusLabel(status: RequestDto['status']): EmployeeRequest['status'] {
    switch (status) {
      case 'APPROVED':
        return 'Approved';
      case 'REJECTED':
        return 'Rejected';
      case 'IN_REVIEW':
        return 'In review';
      default:
        return 'Draft';
    }
  }
}
