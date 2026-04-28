import { Component, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
	selector: 'app-forgot-password',
	standalone: true,
	imports: [CommonModule, FormsModule, RouterLink],
	templateUrl: './forgot-password.component.html',
	styleUrl: './forgot-password.component.css'
})
export class ForgotPasswordComponent {
	private auth = inject(AuthService);
	private cdr = inject(ChangeDetectorRef);

	email = '';
	isLoading = false;
	successMessage = '';
	errorMessage = '';

	sendResetLink(): void {
		if (!this.email) return;

		this.isLoading = true;
		this.errorMessage = '';
		this.successMessage = '';

		this.auth.recoverPassword({ email: this.email }).subscribe({
			next: () => {
				this.isLoading = false;
				this.successMessage = 'If an account exists for this email, you will receive a reset link shortly.';
			},
			error: (err: Error) => {
				this.isLoading = false;
				this.errorMessage = err.message;
				this.cdr.detectChanges();
			}
		});
	}
}