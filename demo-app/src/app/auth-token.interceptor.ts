import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { LoginStore } from './features/login/login.store';

export const authTokenInterceptor: HttpInterceptorFn = (request, next) => {
  const loginStore = inject(LoginStore);
  const token = loginStore.token();

  const isPublic =
    request.url.endsWith('/login') ||
    request.url.includes('/password/forgot') ||
    request.url.includes('/password/reset');

  if (!token || isPublic) {
    return next(request);
  }

  return next(
    request.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`,
      },
    }),
  );
};