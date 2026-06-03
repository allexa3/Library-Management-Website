import { Routes } from '@angular/router';
import { authGuard, guestGuard, adminGuard } from './guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'books',
    pathMatch: 'full'
  },
  {
    path: 'login',
    loadComponent: () => import('./features/login/login.component').then(m => m.LoginComponent),
    canActivate: [guestGuard]
  },
  {
    path: 'forgot-password',
    loadComponent: () => import('./features/forgot-password/forgot-password.component').then(m => m.ForgotPasswordComponent),
    canActivate: [guestGuard]
  },
  {
    path: 'people',
    loadComponent: () => import('./features/person-list/person-list-page.component').then(m => m.PersonListPageComponent),
    canActivate: [authGuard, adminGuard]
  },
  {
    path: 'books',
    loadComponent: () => import('./features/book-list/book-list-page.component').then(m => m.BookListPageComponent),
    canActivate: [authGuard]
  },
  {
    path: 'genres',
    loadComponent: () => import('./features/genre-list/genre-list-page.component').then(m => m.GenreListPageComponent),
    canActivate: [authGuard, adminGuard]
  },
  {
    path: '**',
    loadComponent: () => import('./features/not-found/not-found-page.component').then(m => m.NotFoundPageComponent)
  }
];