import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { Book } from '../../../book/models/book.model';
import { GameSession, Section } from '../../models/game.model';
import { BookService } from '../../../book/services/book.service';
import { GameService } from '../../services/game.service';

@Component({
  selector: 'app-game-player',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './game-player.component.html',
  styleUrls: ['./game-player.component.css']
})
export class GamePlayerComponent implements OnInit {
  gameSession: GameSession | null = null;
  book: Book | null = null;
  currentSection: Section | null = null;
  isLoading = false;
  isMoving = false;
  errorMessage = '';
  sectionHistory: number[] = [];
  isSaving = false;
  saveMessage = '';
  bookId = 0;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private gameService: GameService,
    private bookService: BookService
  ) {}

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      this.bookId = Number(params['bookId']);
      this.initializeGame();
    });
  }

  initializeGame(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.book = null;
    this.currentSection = null;
    this.sectionHistory = [];

    this.gameService.startGame(this.bookId).subscribe({
      next: (gameSession) => {
        this.gameSession = gameSession;
        this.sectionHistory = gameSession.moveHistory || [];
        this.loadBook(gameSession.bookId);
      },
      error: (err) => {
        console.error('Error starting game:', err);
        this.errorMessage = this.getErrorMessage(err, 'Failed to load game session');
        this.isLoading = false;
      }
    });
  }

  loadBook(bookId: number): void {
    this.bookService.getBookById(bookId).subscribe({
      next: (book) => {
        this.book = book;
        this.loadCurrentSection();
      },
      error: (err) => {
        console.error('Error loading book:', err);
        this.errorMessage = this.getErrorMessage(err, 'Failed to load book');
        this.isLoading = false;
      }
    });
  }

  loadCurrentSection(): void {
    if (!this.book || !this.gameSession) return;

    const sections = this.book.content?.sections || [];
    const section = sections.find((s: Section) => String(s.id) === String(this.gameSession?.activeSectionId));

    if (section) {
      this.currentSection = section;
    } else {
      this.errorMessage = 'Section not found';
    }

    this.isLoading = false;
  }

  selectOption(gotoId: any): void {
    if (!this.gameSession || !this.book) return;

    this.isMoving = true;
    const nextSectionId = typeof gotoId === 'string' ? parseInt(gotoId, 10) : gotoId;
    this.sectionHistory.push(this.gameSession.activeSectionId);

    this.gameService.makeMove(this.gameSession.id, nextSectionId, this.book.id).subscribe({
      next: (updatedSession) => {
        this.gameSession = updatedSession;

        if (updatedSession.status === 'COMPLETED') {
          setTimeout(() => this.showGameOver(), 500);
        } else {
          this.loadCurrentSection();
        }

        this.isMoving = false;
      },
      error: (err) => {
        console.error('Error making move:', err);
        this.errorMessage = this.getErrorMessage(err, 'Failed to make move');
        this.isMoving = false;
        this.sectionHistory.pop();
      }
    });
  }

  goToPreviousSection(): void {
    if (this.sectionHistory.length === 0) return;

    this.isMoving = true;
    const previousSectionId = this.sectionHistory.pop()!;

    if (!this.gameSession || !this.book) return;

    this.gameService.makeMove(this.gameSession.id, previousSectionId, this.book.id).subscribe({
      next: (updatedSession) => {
        this.gameSession = updatedSession;
        this.loadCurrentSection();
        this.isMoving = false;
      },
      error: (err) => {
        console.error('Error going to previous section:', err);
        this.errorMessage = this.getErrorMessage(err, 'Failed to go to previous section');
        this.isMoving = false;
        this.sectionHistory.push(previousSectionId);
      }
    });
  }

  saveCurrentGame(): void {
    if (!this.gameSession) return;

    this.isSaving = true;
    this.saveMessage = '';

    this.gameService.saveGame(this.gameSession.id).subscribe({
      next: () => {
        this.isSaving = false;
        this.saveMessage = '✓ Game saved successfully!';
        setTimeout(() => {
          this.saveMessage = '';
        }, 3000);
      },
      error: (err) => {
        console.error('Error saving game:', err);
        this.isSaving = false;
        this.saveMessage = '✗ ' + this.getErrorMessage(err, 'Failed to save game');
        setTimeout(() => {
          this.saveMessage = '';
        }, 3000);
      }
    });
  }

  private getErrorMessage(err: any, defaultMessage: string): string {
    return err?.error?.error || defaultMessage;
  }

  getSectionType(type: string): string {
    switch (type) {
      case 'BEGIN':
        return 'Beginning';
      case 'END':
        return 'Ending';
      case 'NODE':
        return 'Story';
      default:
        return type;
    }
  }

  goBack(): void {
    this.router.navigate(['/books']);
  }

  showGameOver(): void {
    const message = (this.gameSession?.activeHealth ?? 0) <= 0
      ? 'Game Over! You ran out of health.'
      : 'Congratulations! You reached the end!';

    alert(message);
    this.router.navigate(['/books']);
  }
}
