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


export const routes: Routes = [
  { path: '', component: LandingComponent },

  { path: 'login', component: LoginComponent },

  { path: 'register', component: RegisterComponent },

  { path: 'home', component: HomeComponent },

  { path: 'movie/:id', component: MovieDetailComponent }, 

  { path: 'profile', component: ProfileComponent },

  { path: 'forgot-password', component: ForgotPasswordComponent },

  { path: 'reset-password', component: ResetPasswordComponent },

  { path: 'user/:username', component: PublicProfileComponent },

  { path: 'auth-callback', component: Oauth2RedirectComponent },

  { path: 'choose-username', component: ChooseUsernameComponent },

  { path: '**', redirectTo: '' }

];