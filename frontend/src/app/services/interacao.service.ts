import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from './auth.service';

export interface Interacao {
    id: number;
    usuarioId: number;
    newsId: number;
    curtido: boolean;
    salvo: boolean;
    criadoEm: string;
}

@Injectable({ providedIn: 'root' })
export class InteracaoService {
    private readonly API = 'http://localhost:8080/api/interacoes';

    constructor(private http: HttpClient, private auth: AuthService) {}

    private headers(): HttpHeaders {
        return new HttpHeaders({ Authorization: `Bearer ${this.auth.getToken()}` });
    }

    curtir(newsId: number): Observable<Interacao> {
        return this.http.post<Interacao>(`${this.API}/${newsId}/curtir`, {}, { headers: this.headers() });
    }

    salvar(newsId: number): Observable<Interacao> {
        return this.http.post<Interacao>(`${this.API}/${newsId}/salvar`, {}, { headers: this.headers() });
    }

    minhas(): Observable<Interacao[]> {
        return this.http.get<Interacao[]>(`${this.API}/minhas`, { headers: this.headers() });
    }
}