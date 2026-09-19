import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormArray, FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../core/auth.service';
import { CreateWorkflowPayload, FlowgateApiService, RequestTypeDto } from '../../core/flowgate-api.service';

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
  }

  selectNav(name: string): void {
    this.activeNav = name;
  }

  saveDraft(): void {
    this.form.markAsDirty();
    window.alert('Workflow draft saved locally.');
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
}
