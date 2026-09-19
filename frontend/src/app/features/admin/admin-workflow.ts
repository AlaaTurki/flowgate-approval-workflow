import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormArray, FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../core/auth.service';
import { CreateWorkflowPayload, FlowgateApiService, RequestTypeDto, UserDto } from '../../core/flowgate-api.service';

interface WorkflowSummary {
  name: string;
  steps: string[];
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
      requiredComment: FormControl<boolean>;
    }>>;
  }>;

  activeNav = 'Workflow builder';
  workflowTemplates: WorkflowSummary[] = [];
  users: UserDto[] = [];
  selectedWorkflowName: string | null = null;

  constructor(
    private readonly fb: FormBuilder,
    private readonly authService: AuthService,
    private readonly api: FlowgateApiService,
  ) {
    this.username = this.authService.getUsername();
    this.form = this.fb.nonNullable.group({
      typeName: ['', Validators.required],
      description: [''],
      steps: this.fb.array([
        this.fb.nonNullable.group({
          name: ['Manager approval', Validators.required],
          approver: ['ROLE_MANAGER', Validators.required],
          requiredComment: [true],
        }),
        this.fb.nonNullable.group({
          name: ['Finance review', Validators.required],
          approver: ['ROLE_ADMIN', Validators.required],
          requiredComment: [true],
        }),
      ]),
    });
  }

  ngOnInit(): void {
    this.loadRequestTypes();
    this.loadUsers();
  }

  searchWorkflows(query: string | null | undefined): void {
    this.api.searchWorkflows(query ?? '').subscribe({
      next: (workflows) => {
        this.workflowTemplates = workflows.map((w) => ({ name: w.name, steps: (w.steps || []).map((s) => s.name) }));
      },
      error: () => {
        // leave existing list unchanged on error
      },
    });
  }

  createNewWorkflow(): void {
    this.selectedWorkflowName = null;
    this.form.reset({
      typeName: '',
      description: '',
      steps: [
        { name: 'Manager approval', approver: 'ROLE_MANAGER', requiredComment: true },
      ],
    });
    this.activeNav = 'Workflow builder';
  }

  // user modal state
  showUserModal = false;
  editingUser: UserDto | null = null;
  selectedRoles: string[] = [];
  userForm = this.fb.nonNullable.group({
    username: ['', Validators.required],
    fullName: [''],
    email: ['', [Validators.required, Validators.email]],
    password: [''],
  });

  openCreateUser(): void {
    this.editingUser = null;
    this.selectedRoles = ['ROLE_EMPLOYEE'];
    this.userForm.reset({ username: '', fullName: '', email: '', password: '' });
    this.showUserModal = true;
  }

  openEditUser(user: UserDto): void {
    this.editingUser = user;
    this.selectedRoles = (user as any).roles && Array.isArray((user as any).roles) ? (user as any).roles : ['ROLE_EMPLOYEE'];
    // basic patch - allow changing full name and email and enabled status via form
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
    const payload: any = { username: data.username, fullName: data.fullName, email: data.email };
    if (!this.editingUser) payload.password = data.password || 'changeme';
    payload.roles = this.selectedRoles;

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

  // lightweight stubs to extract roles from user DTO if present
  
  impersonate(user: UserDto): void {
    // lightweight admin impersonation: set username in local storage and reload
    if (!confirm(`Impersonate ${user.username}? You will be logged out as ${this.username}.`)) return;
    localStorage.setItem('impersonate', user.username);
    window.alert('Impersonation flag set (dev). Please log out and log in as the impersonated user.');
  }

  // keep existing helper functions below
  

  selectNav(name: string): void {
    this.activeNav = name;
  }

  saveDraft(): void {
    this.form.markAsDirty();
    window.alert('Workflow draft saved locally.');
  }

  handleDetails(type: WorkflowSummary): void {
    this.selectedWorkflowName = type.name;
    this.form.patchValue({
      typeName: type.name,
      description: type.steps.join(', '),
    });
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
        requiredComment: [false],
      }),
    );
  }

  removeStep(index: number): void {
    if (this.steps.length > 1) {
      this.steps.removeAt(index);
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
      steps: Array<{ name: string; approver: string; requiredComment: boolean }>;
    };

    this.api.createRequestType({ name: value.typeName, description: value.description }).subscribe({
      next: (requestType) => {
        const workflowPayload: CreateWorkflowPayload = {
          name: `${value.typeName} flow`,
          steps: value.steps.map((step, index) => ({
            name: step.name,
            orderIndex: index,
            approverRole: step.approver,
            requiresComment: step.requiredComment,
          })),
        };

        this.api.createWorkflow(requestType.id, workflowPayload).subscribe({
          next: () => {
            this.workflowTemplates.unshift({
              name: value.typeName,
              steps: value.steps.map((step) => step.name),
            });
            this.form.reset({
              typeName: '',
              description: '',
              steps: [
                { name: 'Manager approval', approver: 'ROLE_MANAGER', requiredComment: true },
              ],
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

  private loadRequestTypes(): void {
    this.api.getRequestTypes().subscribe({
      next: (types) => {
        this.workflowTemplates = types.map((type) => ({
          name: type.name,
          steps: [type.description ?? 'Configured approval'],
        }));
      },
      error: () => {
        this.workflowTemplates = [
          { name: 'Expense Reimbursement', steps: ['Manager approval', 'Finance review'] },
          { name: 'Purchase Request', steps: ['Department head', 'Finance review'] },
        ];
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
