import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';

export interface AuthResponse {
  token: string;
  nome: string;
  email: string;
  usuarioId: number;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly API = 'http://localhost:8080/api/auth';
  private readonly TOKEN_KEY = 'agora_token';
  private readonly USER_KEY  = 'agora_user';

  constructor(private http: HttpClient, private router: Router) {}

  login(email: string, senha: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.API}/login`, { email, senha }).pipe(
      tap(res => {
        localStorage.setItem(this.TOKEN_KEY, res.token);
        localStorage.setItem(this.USER_KEY, JSON.stringify({ nome: res.nome, email: res.email, id: res.usuarioId }));
      })
    );
  }

  cadastrar(nome: string, email: string, senha: string, regiao?: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.API}/cadastrar`, { nome, email, senha, regiao }).pipe(
      tap(res => {
        localStorage.setItem(this.TOKEN_KEY, res.token);
        localStorage.setItem(this.USER_KEY, JSON.stringify({ nome: res.nome, email: res.email, id: res.usuarioId }));
      })
    );
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
    this.router.navigate(['/login']);
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  getUser(): { nome: string; email: string; id: number } | null {
    const u = localStorage.getItem(this.USER_KEY);
    return u ? JSON.parse(u) : null;
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }
}
