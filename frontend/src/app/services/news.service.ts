import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Noticia } from '../models/noticia.model';

@Injectable({ providedIn: 'root' })
export class NewsService {
  private readonly API = 'http://localhost:8080/api/news';

  constructor(private http: HttpClient) {}

  getRecentes(): Observable<Noticia[]> {
    return this.http.get<Noticia[]>(`${this.API}/recentes`);
  }

  getPorTag(tag: string): Observable<Noticia[]> {
    return this.http.get<Noticia[]>(`${this.API}/tag/${encodeURIComponent(tag)}`);
  }

  getPorPortal(portal: string): Observable<Noticia[]> {
    return this.http.get<Noticia[]>(`${this.API}/portal/${encodeURIComponent(portal)}`);
  }

  curtir(id: number): Observable<Noticia> {
    return this.http.patch<Noticia>(`${this.API}/${id}/gostei`, {});
  }

  salvarParaDepois(id: number): Observable<Noticia> {
    return this.http.patch<Noticia>(`${this.API}/${id}/ler-depois`, {});
  }

  getPorIds(ids: number[]): Observable<Noticia[]> {
    return this.http.post<Noticia[]>(`${this.API}/porIds`, ids);
  }
}
