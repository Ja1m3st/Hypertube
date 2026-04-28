// frontend/src/app/core/models/user.model.ts

export interface User {
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  profilePictureUrl?: string;
  language?: 'en' | 'es' | 'fr';
  password?: string;
  provider?: AuthProvider;
  isEmailVerified?: boolean;
  watchHistory?: WatchedVideo[]; 
  createdAt?: string;
  updatedAt?: string;
}

export type AuthProvider = 'local' | '42' | 'github' | 'google';

export interface WatchedVideo {
  torrentId: string;
  title: string;
  watchedAt: string;
  progress: number;
}