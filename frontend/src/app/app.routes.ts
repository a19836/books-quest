import { Routes } from '@angular/router';
import { HomeComponent } from './shared/components/home/home.component';
import { LoginComponent } from './auth/components/login/login.component';
import { AuthGuard } from './auth/guards/auth.guard';
import { BookListComponent } from './book/components/book-list/book-list.component';
import { BookDetailComponent } from './book/components/book-detail/book-detail.component';
import { GamePlayerComponent } from './game/components/game-player/game-player.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'books', component: BookListComponent },
  { path: 'book/:id', component: BookDetailComponent },
  { path: '', component: HomeComponent },
  { path: 'game/:bookId', component: GamePlayerComponent, canActivate: [AuthGuard] },
  { path: '**', redirectTo: '' }
];
