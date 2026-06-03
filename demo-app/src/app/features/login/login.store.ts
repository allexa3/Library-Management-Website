import { computed, inject, Injectable, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { catchError, finalize, map, Observable, of, tap } from 'rxjs';
import { LoginRequest, LoginResponse, LoginService } from '../../services/login.service';

interface AuthSnapshot {
  role: string | null;
  token: string;
}

const STORAGE_KEY = 'demo-app-auth';

@Injectable({ providedIn: 'root' })
export class LoginStore {
  private readonly loginService = inject(LoginService);

  readonly isSubmitting = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly token = signal<string | null>(null);
  readonly isAuthenticated = computed(() => this.token() !== null);
  readonly role = signal<string | null>(null);

  readonly currentUserId = computed(() => {
    const t = this.token();
    if (!t) return null;
    try {
      const parts = t.split('.');
      if (parts.length < 2) return null;
      const payload = JSON.parse(atob(parts[1]));
      return (payload.userId as string) || null;
    } catch {
      return null;
    }
  });

  constructor() {
    this.restoreAuthState();
  }

  login(request: LoginRequest): Observable<LoginResponse> {
    this.errorMessage.set(null);
    this.isSubmitting.set(true);

    return this.loginService.login(request).pipe(
      tap((response) => this.applyResponse(response)),
      catchError((error: unknown) => {
        const response = this.normalizeError(error);
        this.applyResponse(response);
        return of(response);
      }),
      finalize(() => this.isSubmitting.set(false)),
    );
  }

  logout(): void {
    this.clearSession();
  }

  private applyResponse(response: LoginResponse): void {
    if (response.success && response.token) {
      this.token.set(response.token);
      this.role.set(response.role ?? null);
      this.errorMessage.set(null);
      this.persistAuthState();
      return;
    }

    this.clearSession(response.errorMessage ?? 'Login failed. Please try again.');
  }

  private normalizeError(error: unknown): LoginResponse {
    if (error instanceof HttpErrorResponse) {
      const body = error.error as Partial<LoginResponse> | null;
      if (body && typeof body.success === 'boolean') {
        return {
          success: false,
          role: body.role ?? null,
          token: body.token ?? null,
          errorMessage:
            body.errorMessage ??
            (error.status === 401
              ? 'Invalid email or password.'
              : 'Unable to complete login. Please try again.'),
        };
      }

      if (error.status === 0) {
        return {
          success: false,
          role: null,
          token: null,
          errorMessage: 'Cannot reach the server. Make sure the backend is running on port 8080.',
        };
      }

      if (error.status === 401) {
        return {
          success: false,
          role: null,
          token: null,
          errorMessage: 'Invalid email or password.',
        };
      }
    }

    return {
      success: false,
      role: null,
      token: null,
      errorMessage: 'Unable to complete login. Please try again.',
    };
  }

  private restoreAuthState(): void {
    const stored = sessionStorage.getItem(STORAGE_KEY);
    if (!stored) return;

    try {
      const snapshot = JSON.parse(stored) as AuthSnapshot;
      if (!snapshot.token) {
        this.clearSession();
        return;
      }
      this.token.set(snapshot.token);
      this.role.set(snapshot.role ?? null);
    } catch {
      this.clearSession();
    }
  }

  private persistAuthState(): void {
    const token = this.token();
    if (!token) return;

    const snapshot: AuthSnapshot = {
      role: this.role(),
      token,
    };
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(snapshot));
  }

  private clearSession(errorMessage: string | null = null): void {
    this.token.set(null);
    this.role.set(null);
    this.errorMessage.set(errorMessage);
    sessionStorage.removeItem(STORAGE_KEY);
  }
}