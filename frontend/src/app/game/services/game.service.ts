import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { GameSession } from '../models/game.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class GameService {
  private apiUrl = environment.apiUrls.game;

  constructor(private http: HttpClient) { }

  startGame(bookId: number): Observable<GameSession> {
    return this.http.post<GameSession>(`${this.apiUrl}/start/${bookId}`, {});
  }

  makeMove(gameSessionId: number, nextSectionId: number, bookId: number): Observable<GameSession> {
    return this.http.post<GameSession>(`${this.apiUrl}/${gameSessionId}/move?bookId=${bookId}`, { 
      nextSectionId: Number(nextSectionId)
    });
  }

  saveGame(gameSessionId: number): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/${gameSessionId}/save`, {});
  }
}
