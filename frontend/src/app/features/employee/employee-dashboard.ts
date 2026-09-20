import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../core/auth.service';
import { FlowgateApiService, RequestDetailDto, RequestDto, RequestTypeDto } from '../../core/flowgate-api.service';

interface EmployeeRequest {
 id: string;
 title: string;
 type: string;
 amount: string;
 approver?: string;
 status: 'Draft' | 'In review' | 'Approved' | 'Rejected';
 submitted: string;
 rawId?: string;
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
 selectedDetail: RequestDetailDto | null = null;
 isLoading = false;
 detailLoading = false;
 editingId: string | null = null;
 readonly stats = {
   open: 0,
   approved: 0,
   avgDays: '0.0d',
 };

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
   this.loadStats();
 }

 selectNav(name: string): void {
   this.activeNav = name;
 }

 get visibleRequests(): EmployeeRequest[] {
   if (this.activeNav === 'Approvals') {
     return this.requests.filter((request) => request.status === 'In review');
   }
   if (this.activeNav === 'History') {
     return this.requests.filter((request) => request.status === 'Approved' || request.status === 'Rejected');
   }
   return this.requests;
 }

 focusNewRequest(): void {
   this.activeNav = 'My requests';
   document.getElementById('new-request-form')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
 }

 viewDetails(requestId: string): void {
   const target = this.requests.find((request) => request.rawId === requestId || request.id === requestId);
   const rawId = target?.rawId ?? requestId;
   this.detailLoading = true;
   this.api.getRequestDetail(rawId).subscribe({
     next: (detail) => {
       this.selectedDetail = detail;
       this.detailLoading = false;
     },
     error: () => {
       this.detailLoading = false;
       window.alert('Unable to load request details.');
     },
   });
 }

 closeDetail(): void {
   this.selectedDetail = null;
 }

 submit(): void {
   if (this.form.invalid) {
     this.form.markAllAsTouched();
     return;
   }

   const { title, requestTypeId, amount, description } = this.form.getRawValue() as {
     title: string;
     requestTypeId: string;
     amount: string;
     description: string;
   };

   if (!requestTypeId) {
     window.alert('Please choose a request type.');
     return;
   }

   this.isLoading = true;
   if (this.editingId) {
     this.api.updateRequest(this.editingId, { requestTypeId, title, description, amount: Number(amount) }).subscribe({
       next: () => {
         this.editingId = null;
         this.form.reset({
           title: '',
           requestTypeId: this.requestTypes[0]?.id ?? '',
           amount: '250.00',
           description: '',
         });
         this.loadRequests();
         this.loadStats();
         this.isLoading = false;
       },
       error: () => {
         this.isLoading = false;
         window.alert('Unable to update the request.');
       },
     });
     return;
   }

   this.api.createRequest({ requestTypeId, title, description, amount: Number(amount) }).subscribe({
     next: () => {
       this.form.reset({
         title: '',
         requestTypeId: this.requestTypes[0]?.id ?? '',
         amount: '250.00',
         description: '',
       });
       this.loadRequests();
       this.loadStats();
       this.isLoading = false;
     },
     error: () => {
       this.isLoading = false;
       window.alert('Unable to submit the request.');
     },
   });
 }

 editRequest(item: EmployeeRequest): void {
   // populate form for inline edit (title/description)
   this.editingId = item.rawId ?? item.id;
   this.form.patchValue({ title: item.title, description: '' });
   // focus the form
   this.activeNav = 'My requests';
   document.getElementById('new-request-form')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
   window.alert('Edit mode: adjust form and submit to update the request.');
 }

 confirmDelete(item: EmployeeRequest): void {
   if (!confirm('Cancel this request? It will stay in the audit trail.')) return;
   this.api.cancelRequest(item.rawId ?? item.id, 'Cancelled by employee').subscribe({
     next: () => {
       this.loadRequests();
       this.loadStats();
     },
     error: () => console.error('Unable to cancel the request.'),
   });
 }

 getOpenRequestsCount(): number {
   return this.requests.filter((request) => request.status !== 'Approved' && request.status !== 'Rejected').length;
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

 private loadStats(): void {
   this.api.getDashboardStats().subscribe({
     next: (stats) => {
       this.stats.open = Number(stats.openRequests ?? 0);
       this.stats.approved = Number(stats.approvedRequests ?? 0);
       this.stats.avgDays = `${Number(stats.averageApprovalDays ?? 0).toFixed(1)}d`;
     },
     error: () => {
       this.stats.open = 0;
       this.stats.approved = 0;
       this.stats.avgDays = '0.0d';
     },
   });
 }

 private mapRequest(item: RequestDto): EmployeeRequest {
   return {
     id: item.id.slice(0, 8).toUpperCase(),
     rawId: item.id,
     title: item.title,
     type: item.requestTypeName,
     amount: item.amount != null ? `$${Number(item.amount).toFixed(2)}` : '$0.00',
     approver: item.currentApproverUsername ? item.currentApproverUsername : (item.currentApproverRole ?? '—'),
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
     case 'SUBMITTED':
       return 'In review';
     default:
       return 'Draft';
   }
 }
}
