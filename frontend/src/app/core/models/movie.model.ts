// frontend/src/app/core/models/movie.model.ts

export interface Movie {
	id: number;
	title: string;
	overview: string;
	poster_path: string | null;
	backdrop_path?: string | null;
	vote_average?: number;
	release_date?: string;
	genre_ids?: number[];
	adult?: boolean;
	original_language?: string;
	original_title?: string;
	popularity?: number;
	video?: boolean;
	vote_count?: number;
}


export interface MovieDetail extends Movie {
  genres: { id: number; name: string }[];
  runtime: number;
  tagline: string;
  status: string;
  budget: number;
  revenue: number;
  production_companies: { id: number; name: string; logo_path: string | null }[];
  spoken_languages: { english_name: string }[];
}