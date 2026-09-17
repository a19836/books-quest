import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { AuthResponse, LoginRequest, RegisterRequest, User } from '../models/auth.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = environment.apiUrls.auth;
  private currentUserSubject: BehaviorSubject<User | null>;
  public currentUser$: Observable<User | null>;
  private tokenExpirationTimer: any;

  constructor(private http: HttpClient) {
    const storedUser = this.getStoredUser();
    this.currentUserSubject = new BehaviorSubject<User | null>(storedUser);
    this.currentUser$ = this.currentUserSubject.asObservable();
    
    if (storedUser) {
      this.setupTokenExpiration();
    }
  }

  public get currentUserValue(): User | null {
    return this.currentUserSubject.value;
  }

  register(email: string, username: string, password: string): Observable<AuthResponse> {
    const request: RegisterRequest = { email, username, password };
    return this.http.post<AuthResponse>(`${this.apiUrl}/register`, request)
      .pipe(
        map(response => {
          this.handleAuthResponse(response);
          return response;
        })
      );
  }

  login(email: string, password: string): Observable<AuthResponse> {
    const request: LoginRequest = { email, password };
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, request)
      .pipe(
        map(response => {
          this.handleAuthResponse(response);
          return response;
        })
      );
  }

  logout(): void {
    localStorage.removeItem('authToken');
    localStorage.removeItem('currentUser');
    this.currentUserSubject.next(null);
    if (this.tokenExpirationTimer) {
      clearTimeout(this.tokenExpirationTimer);
    }
  }

  getToken(): string | null {
    return localStorage.getItem('authToken');
  }

  isAuthenticated(): boolean {
    return this.getToken() !== null;
  }

  private handleAuthResponse(response: AuthResponse): void {
    localStorage.setItem('authToken', response.token);
    const user: User = {
      id: response.userId,
      email: response.email,
      username: response.username
    };
    localStorage.setItem('currentUser', JSON.stringify(user));
    this.currentUserSubject.next(user);
    this.setupTokenExpiration();
  }

  private getStoredUser(): User | null {
    const storedUser = localStorage.getItem('currentUser');
    if (storedUser) {
      try {
        return JSON.parse(storedUser);
      } catch {
        return null;
      }
    }
    return null;
  }

  private setupTokenExpiration(): void {
    if (this.tokenExpirationTimer) {
      clearTimeout(this.tokenExpirationTimer);
    }
    // Token expires in 1 hour (3600000 ms), logout after 1 hour
    this.tokenExpirationTimer = setTimeout(() => {
      this.logout();
    }, 3600000);
  }
}
