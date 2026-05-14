import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { UserService } from '../../../core/services/user.service';
import { LanguageService, Lang } from '../../../core/services/languaje.service';

@Component({
  selector: 'app-public-profile',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './public-profile.component.html',
  styleUrl: './public-profile.component.css'
})
export class PublicProfileComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private userService = inject(UserService);
  private cdr = inject(ChangeDetectorRef);
  public langService = inject(LanguageService);

  user: any = null;
  username: string = '';
  userProfile: any = null;
  isLoading = true;
  errorMsg = '';
  publicFavorites: any[] = [];

  get availableLangs() { return this.langService.available; }
  get currentLang() { return this.langService.lang; }

  setLang(lang: Lang): void {
    this.langService.setLang(lang);
    this.cdr.detectChanges();
  }

  ngOnInit(): void {
    this.username = this.route.snapshot.paramMap.get('username') || '';
    this.loadUserProfile();
    this.loadProfile();
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

  loadProfile(): void {
    this.userService.getPublicProfile(this.username).subscribe({
      next: (data) => {
        this.userProfile = data;
        this.isLoading = false;
		
        this.cdr.detectChanges();
      },
      error: () => {
        this.errorMsg = this.langService.t('profile_not_found');
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });

    this.userService.getPublicUserFavorites(this.username).subscribe({
      next: (dtos) => {
        this.publicFavorites = dtos.map(dto => ({
          id: dto.movieId,
          title: dto.title,
          poster_path: dto.posterPath?.startsWith('/')
            ? `https://image.tmdb.org/t/p/w500${dto.posterPath}`
            : dto.posterPath
        }));
        this.cdr.detectChanges();
      }
    });
  }

  goToProfile(): void { this.router.navigate(['/profile']); }
  goToMovie(id: number): void { this.router.navigate(['/movie', id]); }
  goBack(): void { window.history.back(); }
  logout(): void {
    document.cookie = 'auth_token=; Max-Age=0; path=/';
    this.router.navigate(['/landing']);
  }
}