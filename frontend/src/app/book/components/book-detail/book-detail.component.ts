import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { Book } from '../../models/book.model';
import { BookService } from '../../services/book.service';
import { GameService } from '../../../game/services/game.service';

@Component({
  selector: 'app-book-detail',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './book-detail.component.html',
  styleUrls: ['./book-detail.component.css']
})
export class BookDetailComponent implements OnInit {
  book: Book | null = null;
  isLoading = false;
  isStartingGame = false;
  errorMessage = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private bookService: BookService,
    private gameService: GameService
  ) { }

  ngOnInit() {
    this.route.params.subscribe(params => {
      const bookId = params['id'];
      this.loadBook(bookId);
    });
  }

  private getErrorMessage(err: any, defaultMessage: string): string {
    return err?.error?.error || defaultMessage;
  }

  loadBook(bookId: number) {
    this.isLoading = true;
    this.errorMessage = '';
    this.bookService.getBookById(bookId).subscribe({
      next: (book) => {
        this.book = book;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error loading book:', err);
        this.errorMessage = this.getErrorMessage(err, 'Failed to load book details');
        this.isLoading = false;
      }
    });
  }

  startGame() {
    if (!this.book) return;

    this.isStartingGame = true;
    this.gameService.startGame(this.book.id).subscribe({
      next: () => {
        this.router.navigate(['/game', this.book!.id]);
      },
      error: (err) => {
        console.error('Error starting game:', err);
        alert(this.getErrorMessage(err, 'Failed to start game'));
        this.isStartingGame = false;
      }
    });
  }

  goBack() {
    this.router.navigate(['/books']);
  }
}
