import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ 
  name: 'tmdbImage', 
  standalone: true 
})
export class TmdbImagePipe implements PipeTransform {
  transform(path: string | null, size: string = 'w342'): string {
    if (!path) return 'assets/poster-placeholder.png';
    return `https://image.tmdb.org/t/p/${size}${path}`;
  }
}