import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { RegisterRequestDTO,RegisterResponseDTO,
		LoginResponseDTO, LoginRequestDTO,
		RecoverRequestDTO, RecoverResponseDTO,
		ResetPasswordRequestDTO, ResetPasswordResponseDTO } from '../models/userDTO.model';

// ── Service ──────────────────────────────────────────────────────────────────

@Injectable({ providedIn: 'root' })
export class AuthService {

	private readonly http = inject(HttpClient);
	private readonly API = '/api/auth';

	private userToken: string | null = null;

	register(request: RegisterRequestDTO): Observable<RegisterResponseDTO> {
		return this.http.post<RegisterResponseDTO>(`${this.API}/register`, request);
	}

	login(request: LoginRequestDTO): Observable<LoginResponseDTO> {
		return this.http.post<LoginResponseDTO>(`${this.API}/login`, request).pipe(
			tap(response => this.saveToken(response.token))
		);
	}

	recoverPassword(request: RecoverRequestDTO): Observable<RecoverResponseDTO> {
		return this.http.post<RecoverResponseDTO>(`${this.API}/recover-password`, request);
	}

	resetPassword(request: ResetPasswordRequestDTO): Observable<ResetPasswordResponseDTO> {
		return this.http.post<ResetPasswordResponseDTO>(`${this.API}/reset-password`, request);
	}

	logout(): void {
		this.userToken = null;
		document.cookie = 'auth_token=; path=/; max-age=0';
	}

	getToken(): string | null {
		return this.userToken;
	}

	isLoggedIn(): boolean {
		return this.userToken !== null;
	}

	public saveToken(token: string): void {
		this.userToken = token;
		document.cookie = `auth_token=${token}; path=/; max-age=604800; SameSite=Strict; Secure`;
	}
}