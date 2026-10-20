# Coleta diária com Neon e GitHub Actions

Esta configuração mantém o banco PostgreSQL hospedado na Neon e executa o coletor uma vez por dia em um executor temporário do GitHub Actions. O back-end não fica ligado 24 horas e o front-end continua sendo executado no computador.

## O que precisa estar pronto

- Uma conta gratuita na Neon e um projeto PostgreSQL criado.
- O repositório do Ágora no GitHub como público, para usar os executores padrão do Actions sem custo.
- Uma chave válida da NewsAPI. Cadastre apenas uma chave autorizada para o uso de desenvolvimento/teste previsto pelo plano.

## 1. Criar o PostgreSQL remoto

1. Crie um projeto na Neon e escolha uma região próxima, se disponível.
2. Na tela de conexão, selecione Java/JDBC e copie as informações de conexão.
3. Separe o endereço JDBC, o nome de usuário e a senha. A URL precisa usar TLS, normalmente com `sslmode=require`.
4. Não coloque esses valores em arquivos do projeto nem os envie nesta conversa.

## 2. Migrar o banco que já existe

O banco da Neon começa vazio. Para manter os usuários, as notícias, os feedbacks e os centroides atuais, faça uma cópia do PostgreSQL local para ele antes de ativar a rotina diária.

Uma forma segura é usar `pg_dump` para exportar o banco local e `psql` para importar o arquivo na conexão da Neon. O comando de exportação deve apontar para o banco que o Ágora usa hoje; confira no seu ambiente se ele se chama `agora_db` (execução direta) ou `agora` (Docker Compose). Não compartilhe o arquivo exportado: ele contém dados pessoais e de autenticação.

Se preferir, posso acompanhar essa migração quando você tiver criado o projeto Neon e souber qual dos dois bancos locais está usando. Primeiro faremos uma cópia, sem apagar o banco local.

## 3. Guardar as chaves no GitHub

No repositório, abra **Settings → Secrets and variables → Actions → New repository secret** e crie estes cinco segredos:

| Nome do segredo | Valor |
| --- | --- |
| `NEON_JDBC_URL` | URL JDBC completa da Neon, com `sslmode=require` |
| `NEON_DB_USERNAME` | Usuário do banco Neon |
| `NEON_DB_PASSWORD` | Senha do banco Neon |
| `NEWSAPI_KEY` | Chave atual da NewsAPI |
| `JWT_SECRET` | Segredo aleatório de pelo menos 32 caracteres |

Os segredos são usados somente pelo workflow de coleta. Não habilite outros workflows desconhecidos no repositório enquanto esses segredos estiverem cadastrados.

## 4. Executar uma coleta manual

Depois de salvar os segredos, abra **Actions → Coleta diária de notícias → Run workflow**. O workflow monta e inicia o back-end, conecta ao PostgreSQL da Neon, chama o endpoint existente de notícias recentes e encerra o processo. O agendamento automático está configurado para aproximadamente 08:20 no horário de Brasília; o GitHub pode atrasá-lo alguns minutos.

O coletor existente mantém o limite de 20 chamadas por dia UTC e grava o estado da coleta no PostgreSQL compartilhado, evitando repetir a mesma busca a cada execução. A tarefa usa as quatro consultas temáticas já configuradas. O serviço Python de embeddings não roda no executor diário; isso não impede salvar as notícias, e o processamento de embeddings continua disponível quando o back-end local é iniciado com o serviço Python.

## 5. Usar o catálogo no computador

Para o front-end local mostrar as notícias coletadas na nuvem, o back-end iniciado no IntelliJ precisa apontar para a mesma URL, usuário e senha da Neon. Configure `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` e `SPRING_DATASOURCE_PASSWORD` na configuração de execução do IntelliJ. Mantenha o serviço Python de embeddings local como antes.

O arquivo `docker-compose.yml` da execução local continua usando seu PostgreSQL local. Para apontar o Compose à Neon, é necessária uma configuração Compose separada; não altere o Compose local de forma que ele troque de banco sem querer.

## Limites desta solução

- Isto agenda apenas a coleta; não publica o site nem deixa a API do Ágora disponível na internet.
- A primeira migração de dados é manual e deve ser concluída antes da rotina diária.
- O plano gratuito da Neon tem limites de armazenamento e computação; acompanhe o uso durante o estudo.
- O cron do GitHub é aproximado, não um horário garantido.
