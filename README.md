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
| Personalização | Sentence-BERT multilíngue + K-means |

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

POST   /api/interacoes/{newsId}/feedback → registrar LIKE, UNLIKE, SAVE, UNSAVE, MORE, LESS, CLEAR_MORE ou CLEAR_LESS
POST   /api/interacoes/{newsId}/abertura → registrar abertura da matéria (sinal implícito fraco)
GET    /api/interacoes/minhas            → consultar estados de feedback do usuário autenticado
GET    /api/interacoes/para-voce         → notícias ordenadas pelo beta dinâmico do usuário
```

As ações autenticadas devem enviar `Authorization: Bearer <token>`. Os estados atuais ficam em `tb_interacoes` e cada ação é acrescentada a `tb_feedback_eventos`; o Hibernate cria/atualiza essas estruturas ao iniciar o backend com a configuração atual de `ddl-auto=update`.

### Personalização de notícias (etapa experimental do TCC)

- O serviço `embedding-service` usa `paraphrase-multilingual-MiniLM-L12-v2` para representar **título + resumo** em vetores semânticos. O conteúdo completo da matéria não é obtido.
- O serviço é local ao Docker Compose e não faz chamadas à NewsAPI. Os vetores são gerados apenas para notícias que ainda não têm embedding e ficam em `tb_news` (JSONB), junto do nome/revisão do modelo.
- O primeiro uso baixa os arquivos do modelo do Hugging Face. O volume `embedding_model_cache` preserva esse download entre reinicializações. Esse download e a geração local podem exigir memória e CPU, mas não consomem a cota de requisições da NewsAPI.
- Com pelo menos duas notícias representadas, o backend ajusta K-means determinístico, com no máximo cinco grupos. Os centroides ficam em `tb_cluster_centroides`; notícias futuras são atribuídas ao centroide mais próximo, mantendo os grupos estáveis durante a coleta.
- Cada notícia recebe um `clusterId`, e o evento de feedback registra o grupo conhecido no momento do clique. Com poucos dados, os grupos podem ser frágeis ou pouco interpretáveis; essa limitação faz parte da avaliação acadêmica.
- A seção **Para você** calcula as preferências por cluster com decaimento temporal. `beta` controla o peso dessas preferências na ordenação: começa em 0,15, cresce com os sinais válidos até o limite de 0,85 e é recalculado a cada consulta. Curtir e “ver menos” têm peso maior; abrir notícia tem peso baixo. O endpoint devolve o valor de beta e a quantidade de sinais considerados ou ainda pendentes. Ao abrir a seção, o backend também tenta processar embeddings que ficaram pendentes, sem buscar novos artigos na NewsAPI; eventos antigos sem cluster usam o cluster atual da notícia quando disponível.
- No `docker-compose up --build`, o serviço de embeddings sobe junto do backend e frontend. Se estiver rodando o backend fora do Compose, configure `embedding.service.url` (padrão `http://localhost:8001`) e inicie o serviço Python em `embedding-service`.

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
