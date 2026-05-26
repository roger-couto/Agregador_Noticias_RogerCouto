import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent {
  modo: 'login' | 'cadastro' = 'login';
  email = '';
  senha = '';
  nome = '';
  erro = '';
  carregando = false;

  constructor(private auth: AuthService, private router: Router) {}

  entrar(): void {
    if (!this.email || !this.senha) { this.erro = 'Preencha todos os campos.'; return; }
    this.carregando = true;
    this.erro = '';
    this.auth.login(this.email, this.senha).subscribe({
      next: () => this.router.navigate(['/feed']),
      error: () => { this.erro = 'E-mail ou senha incorretos.'; this.carregando = false; }
    });
  }

  cadastrar(): void {
    if (!this.nome || !this.email || !this.senha) { this.erro = 'Preencha todos os campos.'; return; }
    this.carregando = true;
    this.erro = '';
    this.auth.cadastrar(this.nome, this.email, this.senha).subscribe({
      next: () => this.router.navigate(['/feed']),
      error: (e: any) => {
        this.erro = e.status === 409 ? 'E-mail já cadastrado.' : 'Erro ao cadastrar.';
        this.carregando = false;
      }
    });
  }
}