import { HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';

export const authErrorInterceptor: HttpInterceptorFn = (req, next) => {
	return next(req).pipe(
		catchError((error) => {
			const message = error?.error?.message || 'An unexpected error occurred.';
			return throwError(() => new Error(message));
		})
	);
};