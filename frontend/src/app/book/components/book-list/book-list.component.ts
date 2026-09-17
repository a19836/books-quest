import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { Book } from '../../models/book.model';
import { BookService } from '../../services/book.service';

@Component({
  selector: 'app-book-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './book-list.component.html',
  styleUrls: ['./book-list.component.css']
})
export class BookListComponent implements OnInit, OnDestroy {
  books: Book[] = [];
  filteredBooks: Book[] = [];
  isLoading = false;
  errorMessage = '';
  searchQuery = '';
  selectedDifficulty = '';
  selectedCategories: Set<string> = new Set();
  selectedTags: Set<string> = new Set();
  
  availableCategories: string[] = [];
  availableTags: string[] = [];
  availableDifficulties: string[] = ['EASY', 'MEDIUM', 'HARD'];
  
  private searchSubject = new Subject<string>();

  constructor(
    private bookService: BookService,
    private router: Router
  ) {
    this.searchSubject.pipe(
      debounceTime(500),
      distinctUntilChanged()
    ).subscribe(query => {
      this.searchQuery = query;
      this.applyFilter();
    });
  }

  ngOnInit() {
    this.loadBooks();
  }

  ngOnDestroy() {
    this.searchSubject.complete();
  }

  loadBooks() {
    // If filters are selected, apply them instead
    if (this.searchQuery || this.selectedDifficulty ||
        this.selectedCategories.size > 0 || this.selectedTags.size > 0) {
      this.applyFilter();
      return;
    }

    // No filters - load all books
    this.isLoading = true;
    this.errorMessage = '';

    this.bookService.getAllBooks().subscribe({
      next: (books) => {
        this.books = books;
        this.extractAvailableOptions();
        this.filteredBooks = books;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error loading books:', err);
        this.errorMessage = 'Failed to load books. Please try again later.';
        this.isLoading = false;
      }
    });
  }

  extractAvailableOptions() {
    const categories = new Set<string>();
    const tags = new Set<string>();
    
    this.books.forEach(book => {
      if (book.category) {
        categories.add(book.category);
      }
      if (book.tags && book.tags.length > 0) {
        book.tags.forEach(tag => tags.add(tag));
      }
    });
    
    this.availableCategories = Array.from(categories).sort();
    this.availableTags = Array.from(tags).sort();
  }

  applyFilter() {
    this.isLoading = true;
    this.errorMessage = '';
    
    const difficulty = this.selectedDifficulty || undefined;
    const category = this.selectedCategories.size > 0 ? Array.from(this.selectedCategories)[0] : undefined;
    const tags = this.selectedTags.size > 0 ? Array.from(this.selectedTags) : undefined;
    
    this.bookService.searchBooks(this.searchQuery, difficulty, category, tags).subscribe({
      next: (books) => {
        this.filteredBooks = books;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error searching books:', err);
        this.errorMessage = 'Failed to search books. Please try again later.';
        this.isLoading = false;
      }
    });
  }

  onSearchChange(event: any) {
    this.searchSubject.next(event.target.value);
  }

  toggleDifficulty(difficulty: string) {
    if (this.selectedDifficulty === difficulty) {
      this.selectedDifficulty = '';
    } else {
      this.selectedDifficulty = difficulty;
      this.selectedCategories.clear();
      this.selectedTags.clear();
    }
    this.applyFilter();
  }

  removeDifficulty(difficulty: string) {
    if (this.selectedDifficulty === difficulty) {
      this.selectedDifficulty = '';
      this.applyFilter();
    }
  }

  toggleCategory(category: string) {
    if (this.selectedCategories.has(category)) {
      this.selectedCategories.delete(category);
    } else {
      this.selectedDifficulty = '';
      this.selectedTags.clear();
      this.selectedCategories.clear();
      this.selectedCategories.add(category);
    }
    this.applyFilter();
  }

  removeCategory(category: string) {
    if (this.selectedCategories.has(category)) {
      this.selectedCategories.delete(category);
      this.applyFilter();
    }
  }

  toggleTag(tag: string) {
    if (this.selectedTags.has(tag)) {
      this.selectedTags.delete(tag);
    } else {
      this.selectedDifficulty = '';
      this.selectedCategories.clear();
      this.selectedTags.clear();
      this.selectedTags.add(tag);
    }
    this.applyFilter();
  }

  removeTag(tag: string) {
    if (this.selectedTags.has(tag)) {
      this.selectedTags.delete(tag);
      this.applyFilter();
    }
  }

  selectBook(book: Book) {
    this.router.navigate(['/book', book.id]);
  }
}
