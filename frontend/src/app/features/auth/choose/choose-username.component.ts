import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { UserService } from '../../../core/services/user.service';

@Component({
  selector: 'app-choose-username',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './choose-username.component.html',
  styleUrl: './choose-username.component.css'
})
export class ChooseUsernameComponent {
	private userService = inject(UserService);
	private router = inject(Router);

	username: string = '';
	isLoading: boolean = false;
	errorMsg: string = '';

	saveUsername(): void {
	if (!this.username || this.username.trim() === '') {
		this.errorMsg = 'Username cannot be empty.';
		return;
	}

	this.isLoading = true;
	this.errorMsg = '';

	this.userService.updateUsername(this.username.trim()).subscribe({
		next: () => {
			this.isLoading = false;
			this.router.navigate(['/home']);
		},
		error: (err) => {
			this.isLoading = false;
			this.errorMsg = err.error?.message || err.error || 'Error saving username. Try another.';
		}
	});
	}
}