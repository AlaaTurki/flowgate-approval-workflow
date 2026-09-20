import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.isAuthenticated()) {
    return router.createUrlTree(['/login']);
  }

  return authService.hasValidToken() ? true : router.createUrlTree(['/login']);
};

export const roleGuard = (expectedRole: 'ADMIN' | 'MANAGER' | 'EMPLOYEE'): CanActivateFn => {
  return () => {
    const authService = inject(AuthService);
    const router = inject(Router);

    if (!authService.isAuthenticated() || !authService.hasValidToken()) {
      authService.logout();
      return router.createUrlTree(['/login']);
    }

    const currentRole = authService.getCurrentRole();
    const allows = expectedRole === 'ADMIN' ? currentRole === 'ADMIN' : expectedRole === 'MANAGER' ? currentRole === 'MANAGER' || currentRole === 'ADMIN' : currentRole === 'EMPLOYEE' || currentRole === 'MANAGER' || currentRole === 'ADMIN';
    return allows ? true : router.createUrlTree([authService.getDashboardRoute()]);
  };
};
