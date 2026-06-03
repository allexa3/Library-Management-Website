import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { LoginStore } from '../features/login/login.store';

/**
 * Prevents authenticated users from visiting login/forgot-password.
 * NOTE: LoginStore persists auth in sessionStorage (key: 'demo-app-auth').
 * The old guard incorrectly read localStorage — fixed here.
 */
export const guestGuard: CanActivateFn = () => {
  const loginStore = inject(LoginStore);
  const router = inject(Router);

  // LoginStore restores from sessionStorage on construction, so isAuthenticated()
  // is already correct by the time guards run.
  if (loginStore.isAuthenticated()) {
    const role = loginStore.role();
    return router.parseUrl(role === 'ADMIN' ? '/people' : '/books');
  }

  return true;
};

export const authGuard: CanActivateFn = () => {
  const loginStore = inject(LoginStore);
  const router = inject(Router);

  return loginStore.isAuthenticated() ? true : router.createUrlTree(['/login']);
};

export const adminGuard: CanActivateFn = () => {
  const loginStore = inject(LoginStore);
  const router = inject(Router);

  return loginStore.role() === 'ADMIN' ? true : router.createUrlTree(['/books']);
};