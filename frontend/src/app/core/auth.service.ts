import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, catchError, tap, throwError } from 'rxjs';
import { environment } from '../../environments/environment';

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly tokenKey = 'flowgate_token';

  readonly userToken$ = new BehaviorSubject<string | null>(this.getToken());

  login(username: string, password: string) {
    return this.http
      .post<AuthResponse>(`${environment.apiUrl}/api/auth/login`, {
        username,
        password,
      })
      .pipe(
        tap((response) => {
          localStorage.setItem(this.tokenKey, response.accessToken);
          this.userToken$.next(response.accessToken);
          this.router.navigateByUrl(this.getDashboardRoute());
        }),
        catchError((error) => {
          this.clearSession();
          return throwError(() => error);
        })
      );
  }

  register(username: string, email: string, fullName: string, password: string) {
    return this.http
      .post(`${environment.apiUrl}/api/auth/register`, {
        username,
        email,
        fullName,
        password,
      })
      .pipe(
        tap(() => {
          this.clearSession();
          this.router.navigateByUrl('/login');
        }),
        catchError((error) => {
          this.clearSession();
          return throwError(() => error);
        })
      );
  }

  logout() {
    this.clearSession();
    this.router.navigateByUrl('/login');
  }

  getToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  isAuthenticated(): boolean {
    return !!this.getToken();
  }

  hasValidToken(): boolean {
    const token = this.getToken();
    if (!token) {
      return false;
    }

    try {
      const payload = token.split('.')[1];
      const normalized = payload.replace(/-/g, '+').replace(/_/g, '/');
      const decoded = JSON.parse(atob(normalized));
      const exp = Number(decoded.exp ?? 0);
      return Number.isFinite(exp) && exp * 1000 > Date.now();
    } catch {
      this.clearSession();
      return false;
    }
  }

  getUsername(): string {
    const value = this.getClaim('sub');
    return Array.isArray(value) ? value[0] ?? 'User' : (value ?? 'User');
  }

  getRoles(): string[] {
    const roles = this.getClaim('roles');
    if (!roles) {
      return [];
    }

    return Array.isArray(roles) ? roles.map((role) => String(role)) : [String(roles)];
  }

  getCurrentRole(): string {
    const roles = this.getRoles();
    if (roles.includes('ROLE_ADMIN')) return 'ADMIN';
    if (roles.includes('ROLE_MANAGER')) return 'MANAGER';
    if (roles.includes('ROLE_EMPLOYEE')) return 'EMPLOYEE';
    return 'EMPLOYEE';
  }

  getDashboardRoute(): string {
    switch (this.getCurrentRole()) {
      case 'ADMIN':
        return '/admin';
      case 'MANAGER':
        return '/manager';
      default:
        return '/employee';
    }
  }

  hasRole(role: string): boolean {
    return this.getRoles().includes(`ROLE_${role.toUpperCase()}`);
  }

  clearSession() {
    localStorage.removeItem(this.tokenKey);
    this.userToken$.next(null);
  }

  private getClaim(claimName: string): string | string[] | null {
    const token = this.getToken();
    if (!token) {
      return null;
    }

    try {
      const payload = token.split('.')[1];
      const normalized = payload.replace(/-/g, '+').replace(/_/g, '/');
      const json = decodeURIComponent(
        atob(normalized)
          .split('')
          .map((char) => `%${('00' + char.charCodeAt(0).toString(16)).slice(-2)}`)
          .join('')
      );

      const parsed = JSON.parse(json) as Record<string, unknown>;
      return parsed[claimName] as string | string[] | null;
    } catch {
      return null;
    }
  }
}
