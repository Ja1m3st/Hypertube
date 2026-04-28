import { Component, OnInit, OnDestroy, inject, ChangeDetectorRef } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
	selector: 'app-oauth2-redirect',
	standalone: true,
	imports: [],
	templateUrl: './oauth2-redirect.component.html',
	styleUrl: './oauth2-redirect.component.css'
})
export class Oauth2RedirectComponent implements OnInit, OnDestroy {
	private route = inject(ActivatedRoute);
	private router = inject(Router);
	private authService = inject(AuthService);
	private cdr = inject(ChangeDetectorRef);

	dots = '';
	hourglassIcon = '⧗';

	private dotsInterval?: ReturnType<typeof setInterval>;
	private hourglassInterval?: ReturnType<typeof setInterval>;

	ngOnInit(): void {
		const dotStates = ['', '.', '..', '...'];
		let d = 0;
		this.dotsInterval = setInterval(() => {
			this.dots = dotStates[d++ % dotStates.length];
			this.cdr.detectChanges();
		}, 400);

		const icons = ['⧗', '⧖'];
		let i = 0;
		this.hourglassInterval = setInterval(() => {
			this.hourglassIcon = icons[i++ % icons.length];
			this.cdr.detectChanges();
		}, 500);

		this.route.queryParams.subscribe(params => {
			const token = params['token'];
			const needsSetup = params['setup'] === 'true';

			if (token) {
				this.authService.saveToken(token);
				
				setTimeout(() => {
						if (needsSetup) {
							this.router.navigate(['/choose-username']); 
						} else {
							this.router.navigate(['/home']);
						}
				}, 2000); 
			} else {
					this.router.navigate(['/login']);
			}
		});
	}

	ngOnDestroy(): void {
		clearInterval(this.dotsInterval);
		clearInterval(this.hourglassInterval);
	}
}