import { Component, OnInit, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NewsService } from '../../services/news.service';
import { AuthService } from '../../services/auth.service';
import { ThemeService } from '../../services/theme.service';
import { InteracaoService } from '../../services/interacao.service';
import { Noticia } from '../../models/noticia.model';
import { Tema, TEMAS } from '../../models/tema.model';
import { NewsCardComponent } from '../news-card/news-card.component';

@Component({
  selector: 'app-feed',
  standalone: true,
  imports: [CommonModule, FormsModule, NewsCardComponent],
  templateUrl: './feed.component.html',
  styleUrls: ['./feed.component.scss']
})
export class FeedComponent implements OnInit {
  noticias: Noticia[] = [];
  todasNoticias: Noticia[] = [];
  carregando = false;
  erro = '';
  filtroAtivo = 'recentes';
  termoBusca = '';
  mostrarTemas = false;
  mostrarDropdown = false;
  temaAtual: Tema;
  temas = TEMAS;
  usuario: { nome: string; email: string; id: number } | null = null;

  private mapaInteracoes = new Map<number, { curtido: boolean; salvo: boolean }>();

  // Portais divididos em dois blocos organizados em ordem alfabética
  portaisBr = ['ESTADÃO', 'EXAME', 'GLOBO / G1', 'IGN BRASIL', 'METRÓPOLES', 'UOL'];
  portaisGringos = ['BBC NEWS', 'BLOOMBERG', 'CNN', 'REUTERS', 'TECHCRUNCH', 'THE NEW YORK TIMES'];

  tags = ['#TECNOLOGIA', '#LAZER', '#ECONOMIA', '#ESPORTE', '#POLÍTICA', '#SAÚDE', '#CIÊNCIA', '#CULTURA'];

  constructor(
      private newsService: NewsService,
      private auth: AuthService,
      private themeService: ThemeService,
      private interacaoService: InteracaoService
  ) {
    this.temaAtual = this.themeService.getTemaAtual();
    this.themeService.tema$.subscribe(t => this.temaAtual = t);
  }

  ngOnInit(): void {
    this.usuario = this.auth.getUser();
    this.carregarInteracoesEDepoisNoticias();
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(e: Event): void {
    const target = e.target as HTMLElement;
    if (!target.closest('.usuario-bloco')) {
      this.mostrarDropdown = false;
    }
  }

  toggleDropdown(e: Event): void {
    e.stopPropagation();
    this.mostrarDropdown = !this.mostrarDropdown;
  }

  private carregarInteracoesEDepoisNoticias(): void {
    if (!this.auth.isLoggedIn()) {
      this.carregarRecentes();
      return;
    }
    this.interacaoService.minhas().subscribe({
      next: (interacoes) => {
        this.mapaInteracoes.clear();
        interacoes.forEach(i => this.mapaInteracoes.set(i.newsId, { curtido: i.curtido, salvo: i.salvo }));
        this.carregarRecentes();
      },
      error: () => this.carregarRecentes()
    });
  }

  private aplicarInteracoesNaLista(lista: Noticia[]): Noticia[] {
    return lista.map(n => {
      const estado = n.id != null ? this.mapaInteracoes.get(n.id) : undefined;
      return { ...n, likedByUser: estado?.curtido ?? false, savedByUser: estado?.salvo ?? false };
    });
  }

  carregarRecentes(): void {
    this.filtroAtivo = 'recentes';
    this.carregando = true;
    this.erro = '';
    this.newsService.getRecentes().subscribe({
      next: (data) => {
        const base = data.length > 0 ? data : this.noticiasMock();
        this.todasNoticias = this.aplicarInteracoesNaLista(base);
        this.noticias = this.todasNoticias;
        this.carregando = false;
      },
      error: () => {
        this.todasNoticias = this.aplicarInteracoesNaLista(this.noticiasMock());
        this.noticias = this.todasNoticias;
        this.erro = 'Não foi possível carregar as notícias. Verifique se o backend está rodando.';
        this.carregando = false;
      }
    });
  }

  verCurtidos(): void {
    this.filtroAtivo = 'curtidos';
    this.noticias = this.todasNoticias.filter(n => n.likedByUser);
  }

  verLerMaisTarde(): void {
    this.filtroAtivo = 'ler-mais-tarde';
    this.noticias = this.todasNoticias.filter(n => n.savedByUser);
  }

  selecionarTag(tag: string): void {
    const tagLimpa = tag.replace('#', '');
    this.filtroAtivo = tagLimpa;
    this.newsService.getPorTag(tagLimpa).subscribe(this.handlerFiltro());
  }

  selecionarPortal(portal: string): void {
    this.filtroAtivo = portal;
    this.newsService.getPorPortal(portal).subscribe(this.handlerFiltro());
  }

  private handlerFiltro() {
    this.carregando = true;
    const mapaExistentes = new Map<number, Noticia>();
    this.todasNoticias.forEach(n => { if (n.id != null) mapaExistentes.set(n.id, n); });

    return {
      next: (data: Noticia[]) => {
        // 1. Converte e aplica o estado de curtido/salvo nas novas notícias vindo da API
        const novasNoticiasTratadas = this.aplicarInteracoesNaLista(data.map(n => mapaExistentes.get(n.id!) ?? n));

        // 2. Alimenta a lista que está aparecendo na tela agora
        this.noticias = novasNoticiasTratadas;

        // 3. Alimenta o repositório global para que as abas "Curtidos" e "Ler mais tarde" encontrem elas
        novasNoticiasTratadas.forEach(nova => {
          if (nova.id && !this.todasNoticias.some(t => t.id === nova.id)) {
            this.todasNoticias.push(nova);
          }
        });

        this.carregando = false;
      },
      error: () => { this.noticias = []; this.carregando = false; }
    };
  }

  get noticiasFiltradas(): Noticia[] {
    if (!this.termoBusca.trim()) return this.noticias;
    const t = this.termoBusca.toLowerCase();
    return this.noticias.filter(n =>
        n.titulo?.toLowerCase().includes(t) || n.descricao?.toLowerCase().includes(t)
    );
  }

  onGostei(noticia: Noticia): void {
    if (!noticia.id) return;
    this.interacaoService.curtir(noticia.id).subscribe({
      next: (i) => {
        const atual = this.mapaInteracoes.get(noticia.id!) ?? { curtido: false, salvo: false };
        this.mapaInteracoes.set(noticia.id!, { ...atual, curtido: i.curtido });

        noticia.likedByUser = i.curtido;

        const original = this.todasNoticias.find(n => n.id === noticia.id);
        if (original && original !== noticia) {
          original.likedByUser = i.curtido;
        }

        if (this.filtroAtivo === 'curtidos') {
          this.noticias = this.todasNoticias.filter(n => n.likedByUser);
        }
      },
      error: () => {}
    });
  }

  onSalvar(noticia: Noticia): void {
    if (!noticia.id) return;
    this.interacaoService.salvar(noticia.id).subscribe({
      next: (i) => {
        const atual = this.mapaInteracoes.get(noticia.id!) ?? { curtido: false, salvo: false };
        this.mapaInteracoes.set(noticia.id!, { ...atual, salvo: i.salvo });

        noticia.savedByUser = i.salvo;

        const original = this.todasNoticias.find(n => n.id === noticia.id);
        if (original && original !== noticia) {
          original.savedByUser = i.salvo;
        }

        if (this.filtroAtivo === 'ler-mais-tarde') {
          this.noticias = this.todasNoticias.filter(n => n.savedByUser);
        }
      },
      error: () => {}
    });
  }

  definirTema(tema: Tema): void {
    this.themeService.definirTema(tema);
    this.mostrarTemas = false;
  }

  irParaPrivacidade(): void { this.mostrarDropdown = false; }
  irParaConfiguracoes(): void { this.mostrarDropdown = false; }

  sair(): void {
    this.mostrarDropdown = false;
    this.auth.logout();
  }

  get inicialUsuario(): string {
    return this.usuario?.nome?.charAt(0).toUpperCase() ?? 'U';
  }

  private noticiasMock(): Noticia[] {
    return [
      { id: 1, titulo: 'Backend Java conectado com sucesso ao Ágora', descricao: 'O sistema Spring Boot está integrado e pronto para receber requisições do frontend Angular.', url: '#', portal: 'G1', publicadoEm: new Date(Date.now() - 120000).toISOString(), gostei: 0, lerDepois: 0, likedByUser: false, savedByUser: false },
      { id: 2, titulo: 'Arquitetura minimalista do Smart Journal foca na experiência do usuário', descricao: 'O projeto integra React, Angular e Spring Boot com PostgreSQL para entrega de notícias personalizadas.', url: '#', portal: 'BBC NEWS', publicadoEm: new Date(Date.now() - 900000).toISOString(), gostei: 0, lerDepois: 0, likedByUser: false, savedByUser: false },
      { id: 3, titulo: 'Tecnologia de personalização de feeds avança com modelos de linguagem modernos', descricao: 'Pesquisadores desenvolvem algoritmos de recomendação que consideram o histórico de leitura.', url: '#', portal: 'CNN', publicadoEm: new Date(Date.now() - 3600000).toISOString(), gostei: 0, lerDepois: 0, likedByUser: false, savedByUser: false },
    ];
  }
}