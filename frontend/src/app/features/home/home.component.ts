import { Component, OnInit, inject, ChangeDetectorRef, HostListener } from '@angular/core';
import { CommonModule, DecimalPipe, DatePipe, SlicePipe } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { debounceTime, distinctUntilChanged, Subject } from 'rxjs';
import { MovieService } from '../../core/services/movie.service';
import { Movie } from '../../core/models/movie.model';
import { forkJoin } from 'rxjs';
import { HistoryService } from '../../core/services/history.service';
import { UserService } from '../../core/services/user.service';
import { LanguageService, Lang } from '../../core/services/languaje.service';

@Component({
	selector: 'app-home',
	standalone: true,
	imports: [CommonModule, RouterLink, FormsModule, DecimalPipe, DatePipe, SlicePipe],
	templateUrl: './home.component.html',
	styleUrl: './home.component.css',
})
export class HomeComponent implements OnInit {
	private movieService = inject(MovieService);
	private historyService = inject(HistoryService);
	private router = inject(Router);
	private cdr = inject(ChangeDetectorRef);
	private userService = inject(UserService);
	public langService = inject(LanguageService);

	user: any = null;
	movies: Movie[] = [];
	filteredMovies: Movie[] = [];
	continueWatching: any[] = [];
	featuredMovie: Movie | null = null;
	isLoading = true;
	searchQuery = '';
	currentSort: string = 'popular';
	skeletons = Array(12).fill(0);
	private searchSubject = new Subject<string>();


	currentPage = 2;
	isLoadingMore = false;


	get t() { return (key: string) => this.langService.t(key); }
	get availableLangs() { return this.langService.available; }
	get currentLang() { return this.langService.lang; }

	ngOnInit(): void {
		this.loadMovies(true);
		this.loadPopular();
		this.loadHistory();
		this.loadUserProfile();
		this.searchSubject.pipe(
			debounceTime(300),
			distinctUntilChanged(),
		).subscribe(query => this.filterMovies(query));
	}

	setLang(lang: Lang): void {
		this.langService.setLang(lang);
		this.cdr.detectChanges();
	}

	loadUserProfile(): void {
		this.userService.getUserProfile().subscribe({
			next: (data) => {
				this.user = { ...data, profilePictureUrl: data.avatar };
				if (data.language) {
					this.langService.setLang(data.language as Lang);
				}
				this.cdr.detectChanges();
			}
		});
	}

	sortMovies(): void {
		this.loadMovies(true);
	}

	loadMovies(reset: boolean = false): void {
        if (reset) {
            this.isLoading = true;
            this.currentPage = 1;
            this.movies = [];
            this.filteredMovies = [];
            this.cdr.detectChanges();
            
            forkJoin([
                this.movieService.getMoviesSorted('1', this.currentSort),
                this.movieService.getMoviesSorted('2', this.currentSort),
                this.movieService.getMoviesSorted('3', this.currentSort),
            ]).subscribe({
                next: ([page1, page2, page3]) => {
                    const all = [...page1, ...page2, ...page3];
                    this.movies = all;
                    this.filteredMovies = all;
                    this.featuredMovie = all[0] ?? null;
                    this.currentPage = 3;
                    this.isLoading = false;
                    this.cdr.detectChanges();
                },
                error: () => { this.isLoading = false; this.cdr.detectChanges(); }
            });
        } 
    }

	@HostListener('window:scroll', ['$event'])
	onScroll(event: Event): void {
		if (this.isLoading || this.isLoadingMore || this.searchQuery) return;
		const pos = (document.documentElement.scrollTop || document.body.scrollTop) + document.documentElement.offsetHeight;
		const max = document.documentElement.scrollHeight;
		if (pos > max - 300) { this.loadMore(); }
	}

	logout(): void {
		document.cookie = 'auth_token=; Max-Age=0; path=/';
		this.router.navigate(['/landing']);
	}

	loadHistory(): void {
		this.historyService.getWatchHistory().subscribe({
			next: (history: any[]) => {
				const unfinishedMovies = history.filter(h => h.progress > 0 && !h.completed);
				this.continueWatching = unfinishedMovies.map(h => ({
					...h,
					posterPath: (h.posterPath && h.posterPath.startsWith('/'))
						? `https://image.tmdb.org/t/p/w500${h.posterPath}`
						: h.posterPath
				}));
				this.cdr.detectChanges();
			}
		});
	}

	goToProfile(): void { this.router.navigate(['/profile']); }

	loadPopular(): void {
		this.isLoading = true;
		forkJoin([
			this.movieService.getPopularMovies('1'),
			this.movieService.getPopularMovies('2'),
			this.movieService.getPopularMovies('3')
		]).subscribe({
			next: ([page1, page2, page3]) => {
				const all = [...page1, ...page2, ...page3];
				this.movies = all;
				this.filteredMovies = all;
				this.featuredMovie = all[0] ?? null;
				this.isLoading = false;
				this.cdr.detectChanges();
			},
			error: () => { this.isLoading = false; this.cdr.detectChanges(); }
		});
	}

	loadMore(): void {
        if (this.isLoadingMore || this.isLoading) return;
        
        this.isLoadingMore = true;
        this.currentPage++;
        
        this.movieService.getMoviesSorted(this.currentPage.toString(), this.currentSort).subscribe({
            next: (newMovies) => {
                const uniqueNewMovies = newMovies.filter(newMovie => 
                    !this.movies.some(existingMovie => existingMovie.id === newMovie.id)
                );

                this.movies = [...this.movies, ...uniqueNewMovies];
                
                if (!this.searchQuery) {
                    this.filteredMovies = this.movies;
                }
                
                this.isLoadingMore = false;
                this.cdr.detectChanges();
            },
            error: () => {
                this.isLoadingMore = false;
                this.currentPage--;
                this.cdr.detectChanges();
            }
        });
    }

	onSearch(): void { this.searchSubject.next(this.searchQuery.trim()); }

	clearSearch(): void {
		this.searchQuery = '';
		this.filteredMovies = this.movies;
	}

	private filterMovies(query: string): void {
		if (!query) { this.filteredMovies = this.movies; return; }
		this.isLoading = true;
		this.cdr.detectChanges();
		this.movieService.searchMovies(query).subscribe({
			next: (results) => { this.filteredMovies = results; this.isLoading = false; this.cdr.detectChanges(); },
			error: () => { this.filteredMovies = []; this.isLoading = false; this.cdr.detectChanges(); }
		});
	}

	goToMovie(id: number): void { this.router.navigate(['/movie', id]); }

	onImageError(event: Event): void {
		(event.target as HTMLImageElement).src = 'https://via.placeholder.com/300x450/1a1a1a/ffffff?text=No+Poster';
	}
}