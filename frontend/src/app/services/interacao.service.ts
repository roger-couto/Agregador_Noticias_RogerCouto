import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from './auth.service';
import { Noticia } from '../models/noticia.model';

export interface Interacao {
    id: number;
    usuarioId: number;
    newsId: number;
    curtido: boolean;
    salvo: boolean;
    verMais: boolean;
    verMenos: boolean;
    criadoEm: string;
    atualizadoEm: string;
}

export type FeedbackAcao = 'LIKE' | 'UNLIKE' | 'SAVE' | 'UNSAVE' | 'MORE' | 'LESS' | 'CLEAR_MORE' | 'CLEAR_LESS';

export interface Recomendacao {
    noticias: Noticia[];
    beta: number;
    sinaisConsiderados: number;
    sinaisPendentes: number;
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

    registrarFeedback(newsId: number, tipo: FeedbackAcao): Observable<Interacao> {
        return this.http.post<Interacao>(`${this.API}/${newsId}/feedback`, { tipo }, { headers: this.headers() });
    }

    registrarAbertura(newsId: number): Observable<Interacao> {
        return this.http.post<Interacao>(`${this.API}/${newsId}/abertura`, {}, { headers: this.headers() });
    }

    minhas(): Observable<Interacao[]> {
        return this.http.get<Interacao[]>(`${this.API}/minhas`, { headers: this.headers() });
    }

    paraVoce(): Observable<Recomendacao> {
        return this.http.get<Recomendacao>(`${this.API}/para-voce`, { headers: this.headers() });
    }
}
