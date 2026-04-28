import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule, DecimalPipe, DatePipe } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Movie } from '../../../core/models/movie.model';
import { UserService } from '../../../core/services/user.service';
import { HistoryService } from '../../../core/services/history.service';
import { User } from '../../../core/models/user.model';
import { LanguageService } from '../../../core/services/languaje.service';
import { forkJoin } from 'rxjs';


@Component({
	selector: 'app-profile',
	standalone: true,
	imports: [CommonModule, RouterLink, FormsModule, DecimalPipe, DatePipe],
	templateUrl: './profile.component.html',
	styleUrl: './profile.component.css',
})
export class ProfileComponent implements OnInit {
	private userService = inject(UserService);
	private router = inject(Router);
	private cdr = inject(ChangeDetectorRef);
	private progress = inject(HistoryService);
	public langService = inject(LanguageService);

	user: User = { username: '', email: '', firstName: '', lastName:''};
	editForm = { username: '', email: '', password: '' };

	isEditing = false;
	watchedMovies: Movie[] = [];
	historyMovies: Movie[] = [];
	favoriteMovies: Movie[] = [];

	activeTab: 'watched' | 'history' | 'favorites' | 'settings' = 'watched';

	activeModal: 'none' | 'username' | 'email' | 'password' | 'avatar' = 'none';
    modalInput1: string = '';
    modalInput2: string = '';
	modalInput3: string = '';
    modalLoading: boolean = false;
    modalError: string = '';
	

	
	// ────────────────────────────────────────────────────────────
	ngOnInit(): void {
		this.loadUserData();
	}
	
	goBack(): void {
		this.router.navigate(['/home']);
	}


	openModal(type: 'username' | 'email' | 'password' | 'avatar'): void {
        this.activeModal = type;
        this.modalError = '';

        if (type === 'username') this.modalInput1 = this.user.username;
        else if (type === 'email') this.modalInput1 = this.user.email;
        else if (type === 'avatar') this.modalInput1 = this.user.profilePictureUrl || '';
        else if (type === 'password') {
			this.modalInput1 = '';
       		this.modalInput3 = '';
		}
    }

    closeModal(): void {
        this.activeModal = 'none';
        this.modalInput1 = '';
        this.modalInput2 = '';
		this.modalInput3 = '';
        this.modalError = '';
    }

	saveModalChanges(): void {
        if (!this.modalInput1.trim() && this.activeModal !== 'password' && this.activeModal !== 'avatar') {
            this.modalError = 'Field cannot be empty.';
            return;
        }

        this.modalLoading = true;
        this.modalError = '';

        if (this.activeModal === 'username') {
            this.userService.updateUsername(this.modalInput1.trim()).subscribe({
                next: () => {
                    this.finishModalSuccess();
                },
                error: (err) => this.handleModalError(err)
            });
        } 
        else if (this.activeModal === 'email') {
            this.userService.updateEmail(this.modalInput1.trim()).subscribe({
                next: () => {
                    this.finishModalSuccess();
                },
                error: (err) => this.handleModalError(err)
            });
        } 
        else if (this.activeModal === 'avatar') {
            this.userService.updatePicture(this.modalInput1.trim()).subscribe({
                next: () => {
                    this.finishModalSuccess();
                },
                error: (err) => this.handleModalError(err)
            });
        } 
		else if (this.activeModal === 'password') {
            if (!this.modalInput1 || !this.modalInput2 || !this.modalInput3) {
                this.modalError = 'All fields are required.';
                this.modalLoading = false;
                return;
            }
            if (this.modalInput2 !== this.modalInput3) {
                this.modalError = 'New passwords do not match.';
                this.modalLoading = false;
                return;
            }
            
            this.userService.updatePassword(this.modalInput1, this.modalInput2).subscribe({
                next: () => {
                    this.finishModalSuccess();
                },
                error: (err) => this.handleModalError(err)
            });
        }
    }

	private finishModalSuccess(): void {
        this.modalLoading = false;
        this.closeModal();
		this.loadUserData();
        this.cdr.detectChanges();
    }

	private handleModalError(err: any): void {
        this.modalLoading = false; 

        let extractedError = 'An error occurred while saving.';


        if (err instanceof Error) {
            extractedError = err.message;
        } 

        else if (err && err.error) {
            if (typeof err.error === 'string') {
                extractedError = err.error;
            } else if (err.error.message) {
                extractedError = err.error.message;
            } else if (err.error.text) {
                extractedError = err.error.text;
            }
        } 

        else if (err && err.message) {
            extractedError = err.message;
        }

        this.modalError = extractedError;
        this.cdr.detectChanges();
    }

	private loadUserData(): void {
		forkJoin({
			user: this.userService.getUserProfile(),
			history: this.progress.getWatchHistory(),
			favorites: this.userService.getFavoriteMovies()
		}).subscribe({
			next: (results) => {

				this.user = {
					username: results.user.username,
					firstName: results.user.firstName,
					lastName: results.user.lastName,
					email: results.user.email,
					profilePictureUrl: results.user.avatar
				};

				const moviesFromBackend = results.history;
				const mappedMovies: Movie[] = moviesFromBackend.map((item: any) => ({
					id: item.id,
					title: item.title,
					release_date: item.year,
					poster_path: (item.posterPath && item.posterPath.startsWith('/')) 
						? `https://image.tmdb.org/t/p/w500${item.posterPath}` 
						: item.posterPath,
					vote_average: item.progress || 0,
					genre_ids: [],
					overview: '',
					backdrop_path: '', 
					adult: false,
					original_language: 'en'
				}));

				this.watchedMovies = mappedMovies.filter(m => 
					moviesFromBackend.find((b: any) => b.id === m.id)?.completed === true
				);
				this.historyMovies = mappedMovies.filter(m => 
					moviesFromBackend.find((b: any) => b.id === m.id)?.completed === false
				);


				this.favoriteMovies = results.favorites.map((dto: any) => {
					const fullPosterUrl = (dto.posterPath && dto.posterPath.startsWith('/')) 
						? `https://image.tmdb.org/t/p/w500${dto.posterPath}` 
						: dto.posterPath;

					return {
						id: dto.movieId,             
						title: dto.title,
						poster_path: fullPosterUrl,
						release_date: '',            
						vote_average: 0,
						genre_ids: [],
						overview: '',
						backdrop_path: '',
						adult: false,
						original_language: 'en'
					};
				});

				this.cdr.detectChanges();
			}
		});
	}


	// ── Helpers ──────────────────────────────────────────────────
	getInitials(): string {
		return this.user.username
			.split(' ')
			.map(n => n[0])
			.slice(0, 2)
			.join('')
			.toUpperCase();
	}

	// ── Tabs ─────────────────────────────────────────────────────
	setTab(tab: 'watched' | 'history' | 'favorites' | 'settings'): void {
		this.activeTab = tab;
	}

	// ── Edit ─────────────────────────────────────────────────────
	toggleEdit(): void {
		this.isEditing = !this.isEditing;
		if (this.isEditing) {
			this.editForm = {
				username: this.user.username,
				email: this.user.email,
				password: '',
			};
		}
	}

	saveProfile(): void {
		if (this.editForm.username.trim()) {
			this.user.username = this.editForm.username.trim();
		}
		if (this.editForm.email.trim()) {
			this.user.email = this.editForm.email.trim();
		}
		this.isEditing = false;
		this.cdr.detectChanges();
	}

	// ── Navigation ───────────────────────────────────────────────
	goToMovie(id: number): void {
		this.router.navigate(['/movie', id]);
	}

	goToProfile(): void {
		this.router.navigate(['/profile']);
	}

	// ── Auth ─────────────────────────────────────────────────────
	logout(): void {
		document.cookie = 'auth_token=; Max-Age=0; path=/';
		this.router.navigate(['/landing']);
	}
	
}