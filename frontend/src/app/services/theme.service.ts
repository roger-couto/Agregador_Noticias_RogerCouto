import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { TEMAS, Tema } from '../models/tema.model';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly STORAGE_KEY = 'agora_tema';
  private temaAtual$ = new BehaviorSubject<Tema>(this.carregarTema());

  tema$ = this.temaAtual$.asObservable();

  constructor() {
    this.aplicarTema(this.temaAtual$.value);
  }

  private carregarTema(): Tema {
    const salvo = localStorage.getItem(this.STORAGE_KEY);
    if (salvo) {
      const encontrado = TEMAS.find(t => t.classe === salvo);
      if (encontrado) return encontrado;
    }
    return TEMAS[3]; // Oceano como padrão (igual ao screenshot azul)
  }

  definirTema(tema: Tema): void {
    localStorage.setItem(this.STORAGE_KEY, tema.classe);
    this.temaAtual$.next(tema);
    this.aplicarTema(tema);
  }

  private aplicarTema(tema: Tema): void {
    const body = document.body;
    TEMAS.forEach(t => body.classList.remove(t.classe));
    body.classList.add(tema.classe);
  }

  getTemaAtual(): Tema {
    return this.temaAtual$.value;
  }

  getTemas(): Tema[] {
    return TEMAS;
  }
}
