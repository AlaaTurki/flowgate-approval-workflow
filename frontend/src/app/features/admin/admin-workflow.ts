import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormArray, FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { CreateWorkflowPayload, FlowgateApiService, UserDto, WorkflowDto } from '../../core/flowgate-api.service';

interface WorkflowStepSummary {
  order: number;
  name: string;
  role: string;
  approverUser?: string | null;
  requiresComment: boolean;
}

interface WorkflowSummary {
  id: string;
  requestTypeId?: string | null;
  name: string;
  steps: WorkflowStepSummary[];
}

@Component({
  selector: 'app-admin-workflow',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './admin-workflow.html',
  styleUrl: './admin-workflow.scss',
})
export class AdminWorkflowComponent implements OnInit {
  readonly username: string;
  readonly form: FormGroup<{
    typeName: FormControl<string>;
    description: FormControl<string>;
    steps: FormArray<FormGroup<{
      name: FormControl<string>;
      approver: FormControl<string>;
      approverUserId: FormControl<string>;
      requiredComment: FormControl<boolean>;
    }>>;
  }>;

  activeNav = 'Workflow builder';
  workflowTemplates: WorkflowSummary[] = [];
  users: UserDto[] = [];
  selectedWorkflowId: string | null = null;

  constructor(
    private readonly fb: FormBuilder,
    private readonly authService: AuthService,
    private readonly api: FlowgateApiService,
    private readonly router: Router,
  ) {
    this.username = this.authService.getUsername();
    this.form = this.fb.nonNullable.group({
      typeName: ['', Validators.required],
      description: [''],
      steps: this.fb.array([
        this.fb.nonNullable.group({
          name: ['', Validators.required],
          approver: ['ROLE_MANAGER', Validators.required],
          approverUserId: [''],
          requiredComment: [false],
        }),
      ]),
    });
    this.userForm = this.fb.nonNullable.group({
      username: ['', Validators.required],
      fullName: [''],
      email: ['', [Validators.required, Validators.email]],
      password: [''],
    });
  }

  ngOnInit(): void {
    this.loadRequestTypes();
    this.loadUsers();
  }

  searchWorkflows(query: string | null | undefined): void {
    this.api.searchWorkflows(query ?? '').subscribe({
      next: (workflows) => {
        this.workflowTemplates = workflows.map((workflow) => this.mapWorkflow(workflow));
      },
      error: () => {
        this.workflowTemplates = [];
      },
    });
  }

  createNewWorkflow(): void {
    this.selectedWorkflowId = null;
    this.form.reset({
      typeName: '',
      description: '',
      steps: [{ name: '', approver: 'ROLE_MANAGER', approverUserId: '', requiredComment: false }],
    });
    this.activeNav = 'Workflow builder';
  }

  showUserModal = false;
  editingUser: UserDto | null = null;
  selectedRoles: string[] = [];
  userForm: FormGroup;

  openCreateUser(): void {
    this.editingUser = null;
    this.selectedRoles = ['ROLE_EMPLOYEE'];
    this.userForm.reset({ username: '', fullName: '', email: '', password: '' });
    this.showUserModal = true;
  }

  openEditUser(user: UserDto): void {
    this.editingUser = user;
    this.selectedRoles = (user as any).roles && Array.isArray((user as any).roles) ? (user as any).roles : ['ROLE_EMPLOYEE'];
    this.userForm.patchValue({ username: user.username, fullName: user.fullName ?? '', email: user.email ?? '', password: '' });
    this.showUserModal = true;
  }

  closeUserModal(): void {
    this.showUserModal = false;
  }

  saveUser(): void {
    if (this.userForm.invalid) {
      this.userForm.markAllAsTouched();
      return;
    }

    const data = this.userForm.getRawValue();
    const payload: any = { username: data.username, fullName: data.fullName, email: data.email, roles: this.selectedRoles };
    if (!this.editingUser) payload.password = data.password || 'changeme';

    if (this.editingUser) {
      this.api.updateUser(this.editingUser.id, payload).subscribe({
        next: (updated) => {
          const idx = this.users.findIndex((u) => u.id === updated.id);
          if (idx >= 0) this.users[idx] = updated;
          window.alert('User updated.');
          this.closeUserModal();
        },
        error: () => window.alert('Unable to update user.'),
      });
      return;
    }

    this.api.createUser(payload).subscribe({
      next: (created) => {
        this.users.unshift(created);
        window.alert('User created.');
        this.closeUserModal();
      },
      error: () => window.alert('Unable to create user.'),
    });
  }

  toggleRole(role: string, event: Event): void {
    const checked = (event.target as HTMLInputElement).checked;
    if (checked) {
      if (!this.selectedRoles.includes(role)) this.selectedRoles.push(role);
    } else {
      this.selectedRoles = this.selectedRoles.filter((r) => r !== role);
    }
  }

  hasRole(role: string): boolean {
    return this.selectedRoles.includes(role);
  }

  confirmDeleteUser(user: UserDto): void {
    if (!window.confirm(`Delete user ${user.username}? This action cannot be undone.`)) return;
    this.api.deleteUser(user.id).subscribe({
      next: () => {
        this.users = this.users.filter((u) => u.id !== user.id);
        window.alert('User deleted.');
      },
      error: () => window.alert('Unable to delete user.'),
    });
  }

  goToApprovals(): void {
    this.router.navigateByUrl('/manager');
  }

  selectNav(name: string): void {
    this.activeNav = name;
  }

  saveDraft(): void {
    this.form.markAsDirty();
    console.info('Workflow draft updated locally.');
  }

  handleDetails(type: WorkflowSummary): void {
    this.selectedWorkflowId = type.id;
    this.form.patchValue({
      typeName: type.name,
      description: type.steps.map((step) => step.name).join(', '),
    });
    const stepFormGroups = type.steps.map((step) => this.fb.nonNullable.group({
      name: [step.name || '', Validators.required],
      approver: [step.role || 'ROLE_MANAGER', Validators.required],
      approverUserId: [step.approverUser ?? ''],
      requiredComment: [step.requiresComment ?? false],
    }));
    this.form.setControl('steps', this.fb.array(stepFormGroups));
    this.activeNav = 'Workflow builder';
  }

  getStepRoleLabel(role: string): string {
    switch (role) {
      case 'ROLE_ADMIN':
        return 'Admin';
      case 'ROLE_MANAGER':
        return 'Manager';
      case 'ROLE_EMPLOYEE':
        return 'Employee';
      default:
        return role;
    }
  }

  get steps(): FormArray {
    return this.form.get('steps') as FormArray;
  }

  addStep(): void {
    this.steps.push(
      this.fb.nonNullable.group({
        name: ['', Validators.required],
        approver: ['ROLE_MANAGER', Validators.required],
        approverUserId: [''],
        requiredComment: [false],
      }),
    );
  }

  removeStep(index: number): void {
    if (this.steps.length > 1) {
      this.steps.removeAt(index);
    }
  }

  onApproverChange(index: number, role: string): void {
    const stepGroup = this.steps.at(index) as FormGroup;
    const currentName = String(stepGroup.get('name')?.value ?? '').trim();
    const roleName = this.getStepRoleLabel(role).trim();
    const baseName = `${roleName} approval`;
    if (!currentName || currentName === 'Manager approval' || currentName === 'Finance review' || currentName === 'Admin approval') {
      stepGroup.patchValue({ name: baseName });
    }
  }

  saveWorkflow(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue() as {
      typeName: string;
      description: string;
      steps: Array<{ name: string; approver: string; approverUserId?: string | null; requiredComment: boolean }>;
    };

    this.api.createRequestType({ name: value.typeName, description: value.description }).subscribe({
      next: (requestType) => {
        const workflowPayload: CreateWorkflowPayload = {
          name: `${value.typeName} flow`,
          steps: value.steps.map((step, index) => ({
            name: step.name || `${this.getStepRoleLabel(step.approver)} approval`,
            orderIndex: index,
            approverRole: step.approver,
            approverUserId: step.approverUserId ? step.approverUserId : null,
            requiresComment: step.requiredComment,
          })),
        };

        this.api.createWorkflow(requestType.id, workflowPayload).subscribe({
          next: () => {
            this.loadRequestTypes();
            this.form.reset({
              typeName: '',
              description: '',
              steps: [{ name: '', approver: 'ROLE_MANAGER', approverUserId: '', requiredComment: false }],
            });
          },
          error: () => window.alert('Request type was created, but the workflow could not be saved.'),
        });
      },
      error: () => window.alert('Unable to create the request type.'),
    });
  }

  logout(): void {
    this.authService.logout();
  }

  private mapWorkflow(workflow: WorkflowDto): WorkflowSummary {
    return {
      id: workflow.id,
      requestTypeId: workflow.requestTypeId,
      name: workflow.name,
      steps: (workflow.steps ?? []).map((step, index) => ({
        order: step.orderIndex ?? index,
        name: step.name,
        role: step.approverRole ?? 'ROLE_MANAGER',
        approverUser: step.approverUserId ?? null,
        requiresComment: !!step.requiresComment,
      })),
    };
  }

  private loadRequestTypes(): void {
    this.api.searchWorkflows('').subscribe({
      next: (workflows) => {
        this.workflowTemplates = workflows.map((workflow) => this.mapWorkflow(workflow));
      },
      error: () => {
        this.workflowTemplates = [];
      },
    });
  }

  private loadUsers(): void {
    this.api.getUsers().subscribe({
      next: (users) => {
        this.users = users;
      },
      error: () => {
        this.users = [];
      },
    });
  }
}
