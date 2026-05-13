import { Routes } from '@angular/router';
import { LandingComponent } from './features/landing/landing.component';
import { HomeComponent } from './features/home/home.component';
import { LoginComponent } from './features/auth/login/login.component';
import { RegisterComponent } from './features/auth/register/register.component';
import { MovieDetailComponent } from './features/movie-detail/movie-detail.component';
import { ProfileComponent } from './features/auth/profile/profile.component';
import { PublicProfileComponent } from './features/auth/publicProfile/public-profile.component';
import { ForgotPasswordComponent } from './features/auth/forgot/forgot-password.component';
import { ResetPasswordComponent } from './features/auth/reset/reset-password.component';
import { Oauth2RedirectComponent } from './features/auth/oauth/oauth2-redirect.component';
import { ChooseUsernameComponent } from './features/auth/choose/choose-username.component';
import { authGuard } from './core/interceptors/auth.interceptor';


export const routes: Routes = [
  { path: '', component: LandingComponent },

  { path: 'login', component: LoginComponent },

  { path: 'register', component: RegisterComponent },

  { 
    path: 'home', 
    component: HomeComponent, 
    canActivate: [authGuard]
  },

  { path: 'movie/:id', component: MovieDetailComponent, 
    canActivate: [authGuard] }, 

  { path: 'profile', component: ProfileComponent, 
    canActivate: [authGuard] },

  { path: 'forgot-password', component: ForgotPasswordComponent },

  { path: 'reset-password', component: ResetPasswordComponent },

  { path: 'user/:username', component: PublicProfileComponent, 
    canActivate: [authGuard] },

  { path: 'auth-callback', component: Oauth2RedirectComponent },

  { path: 'choose-username', component: ChooseUsernameComponent, 
    canActivate: [authGuard] },

  { path: '**', redirectTo: '' }

];