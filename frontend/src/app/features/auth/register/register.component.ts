import { Component, inject, ChangeDetectorRef} from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../core/services/auth.service';

@Component({
	selector: 'app-register',
	standalone: true,
	imports: [ReactiveFormsModule, RouterLink, CommonModule],
	templateUrl: './register.component.html',
	styleUrl: './register.component.css'
})
export class RegisterComponent {
	private fb = inject(FormBuilder);
	private authService = inject(AuthService);
	private router = inject(Router);
	private cdr = inject(ChangeDetectorRef);

	registerForm: FormGroup;
	errorMessage: string | null = null;
	isLoading = false;
	passwordStrength = 0;
	strengthColor = 'rgba(255,255,255,0.06)';
	strengthLabel = '';

	constructor() {
		this.registerForm = this.fb.group({
		firstName: ['', Validators.required],
		lastName:  ['', Validators.required],
		email:     ['', [Validators.required, Validators.email]],
		username:  ['', [Validators.required, Validators.minLength(3)]],
		password:  ['', [Validators.required, Validators.minLength(8)]]
		});
	}

	onPasswordInput(event: Event): void {
		const val = (event.target as HTMLInputElement).value;
		let score = 0;
		if (val.length >= 8)                          score++;
		if (val.length >= 10)                         score++;
		if (/[A-Z]/.test(val) && /[0-9]/.test(val))  score++;
		if (/[^A-Za-z0-9]/.test(val))                score++;

		this.passwordStrength = score;
		const colors = ['', '#ef4444', '#f59e0b', '#22c55e', '#3b82f6'];
		const labels = ['', 'Weak', 'Fair', 'Good', 'Strong'];
		this.strengthColor = val.length > 0 ? colors[score] : 'rgba(255,255,255,0.06)';
		this.strengthLabel = val.length > 0 ? labels[score] : '';
	}

	onSubmit(): void {
		if (this.registerForm.invalid) return;

		this.isLoading = true;
		this.errorMessage = null;

		this.authService.register(this.registerForm.value).subscribe({
			next: () => {
				this.isLoading = false;
				this.router.navigate(['/login']);
			},
			error: (err: Error) => {
				this.isLoading = false;
				this.errorMessage = err.message;
				this.cdr.detectChanges();
			}
		});
	}
}