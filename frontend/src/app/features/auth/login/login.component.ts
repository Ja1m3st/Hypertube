import { Component, inject, ChangeDetectorRef } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink} from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../core/services/auth.service';

@Component({
	selector: 'app-login',
	standalone: true,
	imports: [ReactiveFormsModule, RouterLink, CommonModule],
	templateUrl: './login.component.html',
	styleUrl: './login.component.css'
})
export class LoginComponent {
	private fb = inject(FormBuilder);
	private authService = inject(AuthService);
	private router = inject(Router);
	private cdr = inject(ChangeDetectorRef);


	loginForm: FormGroup;
	errorMessage: string | null = null;
	isLoading = false;

	constructor() {
		this.loginForm = this.fb.group({
			email:    ['', [Validators.required, Validators.email]],
			password: ['', [Validators.required, Validators.minLength(6)]]
		});
	}


	onSubmit(): void {
		if (this.loginForm.invalid)
			return;

		this.isLoading = true;
		this.errorMessage = null;

		this.authService.login(this.loginForm.value).subscribe({
			next: () => {
				this.isLoading = false;
				this.router.navigate(['/home']);
			},
			error: (err: Error) => {
				this.isLoading = false;
				this.errorMessage = err.message;
				this.cdr.detectChanges();
			}
		});
	}
}