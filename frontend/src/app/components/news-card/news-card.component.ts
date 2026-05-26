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
  @Input() noticia!: Noticia;
  @Output() onGostei = new EventEmitter<Noticia>();
  @Output() onSalvar = new EventEmitter<Noticia>();

  curtir(): void { this.onGostei.emit(this.noticia); }
  salvar(): void { this.onSalvar.emit(this.noticia); }

  abrirLink(): void {
    if (this.noticia.url && this.noticia.url !== '#') {
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