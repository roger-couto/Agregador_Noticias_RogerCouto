import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Noticia } from '../../models/noticia.model';

@Component({
  selector: 'app-news-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './news-card.component.html',
  styleUrls: ['./news-card.component.scss']
})
export class NewsCardComponent {
  // O cartão só emite intenções; o componente Feed decide como chamar o backend.
  @Input() noticia!: Noticia;
  @Output() onGostei = new EventEmitter<Noticia>();
  @Output() onSalvar = new EventEmitter<Noticia>();
  @Output() onVerMais = new EventEmitter<Noticia>();
  @Output() onVerMenos = new EventEmitter<Noticia>();
  @Output() onAbrirNoticia = new EventEmitter<Noticia>();

  // Cada botão envia a notícia para o pai, sem conhecer URL ou autenticação da API.
  curtir(): void { this.onGostei.emit(this.noticia); }
  salvar(): void { this.onSalvar.emit(this.noticia); }
  verMais(): void { this.onVerMais.emit(this.noticia); }
  verMenos(): void { this.onVerMenos.emit(this.noticia); }

  registrarCliqueNoLink(event: Event): void {
    // Impede que o clique no link também acione o clique geral do cartão.
    event.stopPropagation();
    this.onAbrirNoticia.emit(this.noticia);
  }

  abrirLink(): void {
    if (this.noticia.url && this.noticia.url !== '#') {
      this.onAbrirNoticia.emit(this.noticia);
      window.open(this.noticia.url, '_blank', 'noopener');
    }
  }

  onImgError(event: Event): void {
    const el = event.target as HTMLElement;
    if (el) el.style.display = 'none';
  }

  formatarTempo(iso: string): string {
    if (!iso) return '';
    const diff = Math.floor((Date.now() - new Date(iso).getTime()) / 60000);
    if (diff < 1) return 'agora';
    if (diff < 60) return `HÁ ${diff} MIN`;
    if (diff < 1440) return `HÁ ${Math.floor(diff / 60)}H`;
    return `HÁ ${Math.floor(diff / 1440)}D`;
  }
}
