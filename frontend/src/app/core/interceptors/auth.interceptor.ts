import { HttpInterceptorFn } from '@angular/common/http';
import { Router, CanActivateFn } from '@angular/router';
import { inject } from '@angular/core';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const reqConCookies = req.clone({
    withCredentials: true
  });
  return next(reqConCookies);
};

export const getCookie = (name: string): string | null => {
  const match = document.cookie.match(new RegExp('(^| )' + name + '=([^;]+)'));
  return match ? match[2] : null;
};

export const authGuard: CanActivateFn = (route, state) => {
  const router = inject(Router);
  const token = getCookie('auth_token');

  if (token) {
    return true;
  } else {
    router.navigate(['/']);
    return false;
  }
};