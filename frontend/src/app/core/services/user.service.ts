import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Movie } from '../models/movie.model';
import {ProfileUserResponseDTO } from '../models/userDTO.model';

@Injectable({
	providedIn: 'root'
})
export class UserService {

	private apiUrl = '/api/users/me';

	private http = inject(HttpClient);


	updateUsername(username: string): Observable<void> {
        return this.http.patch<void>(`${this.apiUrl}/username`, {username});
    }

	updateEmail(email: string): Observable<void> {
        return this.http.patch<void>(`${this.apiUrl}/email`, {email});
    }

	updatePicture(picture: string): Observable<void> {
        return this.http.patch<void>(`${this.apiUrl}/picture`, { picture });
    }

	updatePassword(oldPassword: string, newPassword: string): Observable<void> {
		return this.http.patch<void>(`${this.apiUrl}/password`, { oldPassword, newPassword });
	}

	getUserProfile(): Observable<ProfileUserResponseDTO> {
		return this.http.get<ProfileUserResponseDTO>(this.apiUrl);
	}

	getFavoriteMovies(): Observable<any[]> {
		return this.http.get<any[]>(`${this.apiUrl}/favorites`);
	}
	
    addFavoriteMovie(movieId: number) : Observable<void> {
        return this.http.post<void>(`${this.apiUrl}/favorites/${movieId}`, {});
    }

    removeFavoriteMovie(movieId: number) : Observable<void> {
        return this.http.delete<void>(`${this.apiUrl}/favorites/${movieId}`);
    }

	getPublicProfile(username: string): Observable<any> {
		return this.http.get<any>(`/api/users/${username}/public`);
	}

	getPublicUserFavorites(username: string): Observable<any[]> {
        return this.http.get<any[]>(`/api/users/${username}/favorites`);
    }
}