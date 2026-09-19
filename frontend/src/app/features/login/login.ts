import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { NavigationEnd, Router } from '@angular/router';
import { Subject, filter, takeUntil } from 'rxjs';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-login-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class LoginPageComponent implements OnInit, OnDestroy {
  readonly form: FormGroup;
  private readonly destroy$ = new Subject<void>();

  isRegisterMode = false;
  isSubmitting = false;
  errorMessage = '';

  constructor(
    private readonly fb: FormBuilder,
    private readonly authService: AuthService,
    private readonly router: Router,
  ) {
    this.form = this.fb.nonNullable.group({
      username: ['', [Validators.required]],
      email: [''],
      fullName: [''],
      password: ['', [Validators.required]],
    });
  }

  ngOnInit(): void {
    this.syncModeFromUrl();

    this.router.events
      .pipe(
        filter((event): event is NavigationEnd => event instanceof NavigationEnd),
        takeUntil(this.destroy$),
      )
      .subscribe(() => this.syncModeFromUrl());
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  toggleMode(): void {
    const target = this.isRegisterMode ? '/login' : '/register';
    this.router.navigateByUrl(target);
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSubmitting = true;
    this.errorMessage = '';

    const { username, email, fullName, password } = this.form.getRawValue();

    const request$ = this.isRegisterMode
      ? this.authService.register(username, email, fullName, password)
      : this.authService.login(username, password);

    request$.subscribe({
      next: () => {
        this.isSubmitting = false;
        if (!this.isRegisterMode) {
          this.router.navigateByUrl(this.authService.getDashboardRoute());
        }
      },
      error: (error) => {
        this.isSubmitting = false;
        this.errorMessage = error?.error?.message ?? (this.isRegisterMode ? 'Unable to create your account.' : 'Invalid username or password.');
      },
    });
  }

  private syncModeFromUrl(): void {
    this.isRegisterMode = this.router.url.includes('/register');

    if (this.isRegisterMode) {
      this.form.get('email')?.setValidators([Validators.required, Validators.email]);
      this.form.get('fullName')?.setValidators([Validators.required]);
    } else {
      this.form.get('email')?.clearValidators();
      this.form.get('fullName')?.clearValidators();
      this.form.get('email')?.setValue('');
      this.form.get('fullName')?.setValue('');
    }

    this.form.get('email')?.updateValueAndValidity();
    this.form.get('fullName')?.updateValueAndValidity();
  }
}
