import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';
import { DashboardPageComponent } from './features/dashboard/dashboard';
import { LoginPageComponent } from './features/login/login';

export const routes: Routes = [
  { path: 'login', component: LoginPageComponent },
  { path: 'dashboard', component: DashboardPageComponent, canActivate: [authGuard] },
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  { path: '**', redirectTo: '/login' },
];
