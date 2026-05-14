import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { MovieComment } from '../models/comment.model';

@Injectable({ providedIn: 'root' })
export class CommentService {
    private http = inject(HttpClient);

    getComments(movieId: number): Observable<MovieComment[]> {
        return this.http.get<MovieComment[]>(`/api/movies/${movieId}/comments`);
    }

    postComment(movieId: number, text: string): Observable<MovieComment> {
    	return this.http.post<MovieComment>(`/api/movies/${movieId}/comments`, { text });
    }
 
}