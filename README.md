# ÁGORA – Agregador de Notícias
**Projeto Integrador – Sistemas para Internet**
Aluno: Róger Couto 202211045

---

# 🚀 Implantação e Deploy com Docker - Projeto Ágora

## 📋 Pré-requisitos
* Docker Desktop instalado e em execução.
* Docker Compose instalado.

## ⚙️ Configurações Iniciais
As variáveis de ambiente para conexão do Spring Boot com o banco PostgreSQL já estão pré-configuradas no arquivo `docker-compose.yml`. O back-end aguarda a inicialização do banco de dados antes de iniciar suas atividades.

## 🛠️ Como Executar a Aplicação
1. Abra o terminal na raiz do projeto (onde está localizado o arquivo `docker-compose.yml`).
2. Execute o comando de build e inicialização:
   ```bash
   docker-compose up --build

---

## Stack utilizada
| Camada     | Tecnologia             |
|------------|------------------------|
| Backend    | Java 21 + Spring Boot 3.2 |
| Segurança  | Spring Security + JWT  |
| Banco      | PostgreSQL             |
| Frontend   | Angular 17 + SCSS      |
| News API   | NewsAPI.org            |

---

## Pré-requisitos

- Java 21+
- Maven 3.9+
- Node.js 20+ e npm
- PostgreSQL 15+
- Conta gratuita em [newsapi.org](https://newsapi.org)

---

## 1. Banco de Dados

```sql
-- No psql ou pgAdmin, crie o banco:
CREATE DATABASE agora_db;
```

---

## 2. Backend (Spring Boot)

```bash
cd backend

# Configure application.properties:
# - newsapi.key=SUA_CHAVE_DA_NEWSAPI
# - spring.datasource.password=SUA_SENHA_POSTGRES

# Executar:
./mvnw spring-boot:run
```

O Spring criará as tabelas automaticamente via `ddl-auto=update`.

**Endpoints disponíveis:**
```
POST   /api/auth/cadastrar      → criar conta
POST   /api/auth/login          → obter JWT

GET    /api/news/recentes        → top headlines Brasil
GET    /api/news/tag/{tag}       → filtrar por tópico
GET    /api/news/portal/{portal} → filtrar por portal
PATCH  /api/news/{id}/gostei     → curtir notícia
PATCH  /api/news/{id}/ler-depois → salvar para depois
```

---

## 3. Frontend (Angular)

```bash
cd frontend
npm install
ng serve
```

Acesse: **http://localhost:4200**

---

## 4. Temas disponíveis

| Tema       | Cor primária |
|------------|-------------|
| Oceano     | Azul        |
| Esmeralda  | Verde       |
| Ametista   | Roxo        |
| Solar      | Dourado     |
| Eclipse    | Cinza       |
| Volcanic   | Vermelho    |

Troque o tema pelo botão **TEMAS** no canto inferior da sidebar.

---

## Estrutura de pastas

```
agora-project/
├── backend/
│   ├── pom.xml
│   └── src/main/java/br/com/agora/api/
│       ├── AgoraApplication.java
│       ├── config/
│       │   ├── SecurityConfig.java
│       │   └── JwtService.java
│       ├── controller/
│       │   ├── NewsController.java
│       │   ├── UsuarioController.java
│       │   └── dto/
│       ├── domain/
│       │   ├── model/
│       │   ├── repository/
│       │   └── service/
│       └── resources/
│           └── application.properties
│
└── frontend/
    └── src/app/
        ├── components/
        │   ├── login/
        │   ├── feed/
        │   └── news-card/
        ├── services/
        │   ├── auth.service.ts
        │   ├── news.service.ts
        │   └── theme.service.ts
        ├── models/
        │   ├── noticia.model.ts
        │   └── tema.model.ts
        └── guards/
            └── auth.guard.ts
```

---
