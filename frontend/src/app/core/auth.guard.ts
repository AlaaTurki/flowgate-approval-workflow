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

export const roleGuard = (...expectedRoles: Array<'ADMIN' | 'MANAGER' | 'EMPLOYEE'>): CanActivateFn => {
  return () => {
    const authService = inject(AuthService);
    const router = inject(Router);

    if (!authService.isAuthenticated() || !authService.hasValidToken()) {
      authService.logout();
      return router.createUrlTree(['/login']);
    }

    const currentRole = authService.getCurrentRole();
    const allows = expectedRoles.some((role) => {
      if (role === 'ADMIN') return currentRole === 'ADMIN';
      if (role === 'MANAGER') return currentRole === 'MANAGER' || currentRole === 'ADMIN';
      return currentRole === 'EMPLOYEE' || currentRole === 'MANAGER' || currentRole === 'ADMIN';
    });

    return allows ? true : router.createUrlTree([authService.getDashboardRoute()]);
  };
};
