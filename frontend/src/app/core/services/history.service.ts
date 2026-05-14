import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, Observable } from 'rxjs';

@Injectable({
	providedIn: 'root'
})
export class HistoryService {
  
	constructor(private http: HttpClient) {}


	saveProgress(movieId: number, title: string, year: string, posterPath: string ,percentage: number) {
		const body = {
			title: title,
			year: year,
			posterPath: `https://image.tmdb.org/t/p/w500${posterPath}`,
			percentage: Math.round(percentage)
		};

		return this.http.post(`/api/history/${movieId}/progress`, body);
	}

	getWatchHistory(): Observable<any>  {
		return this.http.get(`/api/history/me`);
	}

	getMovieProgress(movieId: number): Observable<number> {
		return this.getWatchHistory().pipe(
			map((history: any[]) => {
				const entry = history.find((h: any) => h.id === movieId);
				return entry ? (entry.progress ?? 0) : 0;
			})
		);
	}
}