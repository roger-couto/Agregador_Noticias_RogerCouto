import { Routes } from '@angular/router';
import { LoginComponent } from './components/login/login.component';
import { FeedComponent } from './components/feed/feed.component';
import { authGuard } from './guards/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: '/feed', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'feed', component: FeedComponent, canActivate: [authGuard] },
  { path: '**', redirectTo: '/feed' }
];
