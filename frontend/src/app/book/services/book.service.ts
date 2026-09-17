import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Book } from '../models/book.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class BookService {
  private apiUrl = environment.apiUrls.book;

  constructor(private http: HttpClient) { }

  getAllBooks(): Observable<Book[]> {
    return this.http.get<Book[]>(this.apiUrl);
  }

  getBookById(id: number): Observable<Book> {
    return this.http.get<Book>(`${this.apiUrl}/${id}`);
  }

  searchBooks(query: string, difficulty?: string, category?: string, tags?: string[]): Observable<Book[]> {
    let params = new URLSearchParams();
    
    if (query) {
      params.set('search', query);
    }
    if (difficulty) {
      params.set('difficulty', difficulty);
    }
    if (category) {
      params.set('category', category);
    }
    if (tags && tags.length > 0) {
      params.set('tags', tags.join(','));
    }
    
    const queryString = params.toString();
    const url = queryString ? `${this.apiUrl}/search?${queryString}` : this.apiUrl;
    
    return this.http.get<Book[]>(url);
  }
}
