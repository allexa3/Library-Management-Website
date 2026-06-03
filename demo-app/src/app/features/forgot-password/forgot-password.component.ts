import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import {
  AbstractControl,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';

type Step = 'request' | 'verify' | 'done';

function passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
  const a = control.get('newPassword')?.value;
  const b = control.get('confirmPassword')?.value;
  return a && b && a !== b ? { passwordMismatch: true } : null;
}

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './forgot-password.component.html',
  styleUrl: './forgot-password.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ForgotPasswordComponent {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly http = inject(HttpClient);

  protected readonly step = signal<Step>('request');
  protected readonly isSubmitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly submittedEmail = signal('');
  protected readonly showNew = signal(false);
  protected readonly showConfirm = signal(false);

  protected readonly requestForm = this.fb.group(
    {
      email: ['', [Validators.required, Validators.email]],
      newPassword: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', [Validators.required]],
    },
    { validators: passwordMatchValidator },
  );

  protected readonly verifyForm = this.fb.group({
    code: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]],
  });

  protected toggleNew(): void { this.showNew.update(v => !v); }
  protected toggleConfirm(): void { this.showConfirm.update(v => !v); }

  protected submitRequest(): void {
    if (this.requestForm.invalid || this.isSubmitting()) {
      this.requestForm.markAllAsTouched();
      return;
    }
    this.errorMessage.set(null);
    this.isSubmitting.set(true);
    const { email, newPassword, confirmPassword } = this.requestForm.getRawValue();
    this.http
      .post<{ message: string }>('http://localhost:8080/password/forgot', { email, newPassword, confirmPassword })
      .subscribe({
        next: () => {
          this.submittedEmail.set(email);
          this.isSubmitting.set(false);
          this.step.set('verify');
        },
        error: err => {
          this.isSubmitting.set(false);
          const body = err?.error;
          this.errorMessage.set(body?.error ?? body?.message ?? 'Failed to send reset code. Please try again.');
        },
      });
  }

  protected submitVerify(): void {
    if (this.verifyForm.invalid || this.isSubmitting()) {
      this.verifyForm.markAllAsTouched();
      return;
    }
    this.errorMessage.set(null);
    this.isSubmitting.set(true);
    const { code } = this.verifyForm.getRawValue();
    const { email, newPassword, confirmPassword } = this.requestForm.getRawValue();
    this.http
      .post<{ message: string }>('http://localhost:8080/password/reset', { email, code, newPassword, confirmPassword })
      .subscribe({
        next: () => { this.isSubmitting.set(false); this.step.set('done'); },
        error: err => {
          this.isSubmitting.set(false);
          const body = err?.error;
          this.errorMessage.set(body?.error ?? body?.message ?? 'Invalid or expired code. Please try again.');
        },
      });
  }

  protected goBack(): void {
    this.errorMessage.set(null);
    this.step.set('request');
  }
}