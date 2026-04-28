import { Component, inject, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
	selector: 'app-reset-password',
	standalone: true,
	imports: [CommonModule, FormsModule, RouterLink],
	templateUrl: './reset-password.component.html',
	styleUrl: './reset-password.component.css'
})
export class ResetPasswordComponent implements OnInit {
	private auth = inject(AuthService);
	private router = inject(Router);
	private route = inject(ActivatedRoute);
	private cdr = inject(ChangeDetectorRef);

	password = '';
	confirmPassword = '';
	isLoading = false;
	successMessage = '';
	errorMessage = '';
	passwordError = '';
	confirmError = '';

	private token = '';

	ngOnInit(): void {
		this.token = this.route.snapshot.queryParams['token']
			?? this.route.snapshot.params['token']
			?? '';

		if (!this.token) {
			this.errorMessage = 'Invalid or expired reset link. Please request a new one.';
		}
	}

	validatePasswords(): void {
		this.passwordError = this.password && this.password.length < 8
		? 'Password must be at least 8 characters.' : '';

		this.confirmError = this.confirmPassword && this.password !== this.confirmPassword
		? 'Passwords do not match.' : '';
	}

	resetPassword(): void {
		this.validatePasswords();
		if (this.passwordError || this.confirmError || !this.token)
			return;

		this.isLoading = true;
		this.errorMessage = '';
		this.successMessage = '';

		this.auth.resetPassword({ token: this.token, newPassword: this.password }).subscribe({
			next: () => {
				this.isLoading = false;
				this.successMessage = 'Your password has been updated. You can now log in.';
				setTimeout(() => this.router.navigate(['/login']), 3000);
			},
			error: (err: Error) => {
				this.isLoading = false;
				this.errorMessage = err.message;
				this.cdr.detectChanges();
			}
		});
	}
}