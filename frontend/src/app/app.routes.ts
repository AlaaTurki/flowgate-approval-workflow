import { Routes } from '@angular/router';
import { authGuard, roleGuard } from './core/auth.guard';
import { AdminWorkflowComponent } from './features/admin/admin-workflow';
import { EmployeeDashboardComponent } from './features/employee/employee-dashboard';
import { LoginPageComponent } from './features/login/login';
import { ManagerInboxComponent } from './features/manager/manager-inbox';

export const routes: Routes = [
  { path: 'login', component: LoginPageComponent },
  { path: 'register', component: LoginPageComponent },
  { path: 'employee', component: EmployeeDashboardComponent, canActivate: [authGuard] },
  { path: 'manager', component: ManagerInboxComponent, canActivate: [roleGuard('MANAGER')] },
  { path: 'admin', component: AdminWorkflowComponent, canActivate: [roleGuard('ADMIN')] },
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  { path: '**', redirectTo: '/login' },
];
