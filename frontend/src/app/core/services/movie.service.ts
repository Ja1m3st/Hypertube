import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, catchError, map, of } from 'rxjs';
import { Movie, MovieDetail } from '../models/movie.model';
import { LanguageService } from './languaje.service';

interface TmdbResponse {
  page: number;
  results: Movie[];
  total_pages: number;
  total_results: number;
}

@Injectable({
  providedIn: 'root'
})
export class MovieService {
  private http = inject(HttpClient);
  private langService = inject(LanguageService);
  private readonly API = '/api/movies';

  private fixPaths(movie: any): any {
    return {
      ...movie,
      poster_path: movie.poster_path
        ? `https://image.tmdb.org/t/p/w500${movie.poster_path}`
        : 'assets/placeholder.jpg',
      backdrop_path: movie.backdrop_path
        ? `https://image.tmdb.org/t/p/w1280${movie.backdrop_path}`
        : null,
    };
  }

  getPopularMovies(page: string): Observable<Movie[]> {
    return this.http.get<TmdbResponse>(`${this.API}/popular`, {
      params: { page, language: this.langService.tmdbLang }
    }).pipe(
      map(response => response.results.map(m => this.fixPaths(m))),
      catchError(err => {
        return of([]);
      })
    );
  }

  getMovieById(id: number): Observable<MovieDetail> {
    return this.http.get<MovieDetail>(`${this.API}/${id}`, {
      params: { language: this.langService.tmdbLang }
    }).pipe(
      map(movie => this.fixPaths(movie)),
      catchError(err => {
        throw err;
      })
    );
  }

  searchMovies(query: string): Observable<Movie[]> {
    return this.http.get<TmdbResponse>(`${this.API}/search`, {
      params: {
        query: query,
        language: this.langService.tmdbLang
      }
    }).pipe(
      map(response => (response.results ?? []).map(m => this.fixPaths(m))),
      catchError(err => {
        return of([]);
      })
    );
  }

  getMoviesSorted(page: string, sortBy: string): Observable<Movie[]> {
    return this.http.get<TmdbResponse>(`${this.API}/discover`, {
      params: { page, sortBy, language: this.langService.tmdbLang }
    }).pipe(
      map(response => response.results.map(m => this.fixPaths(m))),
      catchError(err => {
        return of([]);
      })
    );
  }
  
  watchMovie(id: number, title: string, year: string): Observable<{ status: string; magnet: string; message: string }> {
    return this.http.post<any>(`${this.API}/${id}/watch`, { title, year }).pipe(
      catchError(err => {
        throw err;
      })
    );
  }

  getSimilarMovies(id: number): Observable<Movie[]> {
    return this.http.get<TmdbResponse>(`${this.API}/${id}/similar`, {
      params: { language: this.langService.tmdbLang }
    }).pipe(
      map(response => (response.results ?? []).map(m => this.fixPaths(m))),
      catchError(err => {
        return of([]);
      })
    );
  }

  isMovieReady(movieId: number): Observable<{ ready: boolean; size: number }> {
    return this.http.get<{ ready: boolean; size: number }>(`${this.API}/${movieId}/ready`);
  }

  getDownloadProgress(movieId: number): Observable<number> {
    return new Observable(observer => {
      let eventSource: EventSource | null = null;
      let retryTimeout: any;
      let closed = false;

      const connect = () => {
        if (closed) return;
        eventSource = new EventSource(`/api/movies/${movieId}/progress`);

        eventSource.onmessage = (event) => {
          const percent = parseInt(event.data, 10);
          observer.next(percent);
          if (percent >= 100) {
            observer.complete();
            eventSource?.close();
          }
        };

        eventSource.onerror = () => {
          eventSource?.close();
          if (!closed) {
            retryTimeout = setTimeout(connect, 3000);
          }
        };
      };

      connect();

      return () => {
        closed = true;
        clearTimeout(retryTimeout);
        eventSource?.close();
      };
    });
  }
}