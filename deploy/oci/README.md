# Implantação temporária no Oracle Cloud Always Free

Esta configuração mantém o coletor, o PostgreSQL e o serviço de embeddings no servidor. O Angular continua no computador local. O backend fica acessível somente em `127.0.0.1` na máquina remota e é usado pelo computador por um túnel SSH.

## 1. Criar a máquina

No Oracle Cloud, crie uma instância Ubuntu ARM Ampere A1 com recursos dentro da cota Always Free (até 2 OCPUs e 12 GB de memória, conforme a disponibilidade da região). Guarde o endereço IP público e o arquivo de chave SSH privada em local seguro. Não envie a chave privada por mensagem.

Na rede/firewall da instância, permita entrada SSH na porta 22. Não abra as portas 5432 (PostgreSQL) nem 8080 (API) para a internet.

## 2. Instalar Docker e baixar o projeto

Conecte-se por SSH como `ubuntu` e instale Docker Engine com o plugin Docker Compose seguindo a documentação oficial do Docker para Ubuntu. Depois, clone o repositório público do Ágora e entre na pasta do projeto:

```bash
git clone https://github.com/roger-couto/Agregador_Noticias_RogerCouto.git
cd Agregador_Noticias_RogerCouto
```

## 3. Configurar os segredos

Na raiz do projeto, crie o arquivo `.env` a partir do exemplo e preencha os três valores. Gere uma senha de banco e o segredo JWT no próprio servidor; não reutilize a senha do banco local. Como a chave da NewsAPI foi compartilhada anteriormente na conversa, gere uma chave nova antes de colocá-la no servidor.

```bash
cp deploy/oci/env.example .env
nano .env
```

O `.env` já é ignorado pelo Git. Nunca o adicione ao repositório.

## 4. Iniciar os serviços

Na raiz do projeto:

```bash
docker compose --env-file .env -f deploy/oci/compose.yml up -d --build
```

Na primeira inicialização, o modelo de embeddings será baixado para o volume persistente. Acompanhe a inicialização com:

```bash
docker compose --env-file .env -f deploy/oci/compose.yml logs -f backend embeddings
```

## 5. Usar a interface local com o backend remoto

No computador local, abra um PowerShell e mantenha este túnel SSH ativo (ajuste o caminho da chave e o IP):

```powershell
ssh -i "C:\caminho\para\chave-privada.key" -N -L 8080:127.0.0.1:8080 ubuntu@IP_PUBLICO_DA_INSTANCIA
```

Em outro terminal, rode o Angular normalmente em `http://localhost:4200`. Pare o backend local para liberar a porta 8080; as chamadas do front local passam pelo túnel para o servidor. O túnel também permite abrir `http://localhost:8080/api/news/recentes` para disparar a coleta inicial.

O backend tenta coletar até quatro consultas temáticas quando a coleta é necessária e também tem agendamento diário para as 8h de Brasília. A janela de busca atual cobre notícias de três dias atrás até ontem. A cota local compartilhada limita o projeto a 20 requisições por dia; resultados repetidos pela mesma URL não viram novos artigos no banco.

## 6. Conferir e fazer backup

Verifique os serviços e os logs:

```bash
docker compose --env-file .env -f deploy/oci/compose.yml ps
docker compose --env-file .env -f deploy/oci/compose.yml logs --tail=100 backend
```

Antes de encerrar a máquina ou remover volumes, faça uma cópia do PostgreSQL:

```bash
docker compose --env-file .env -f deploy/oci/compose.yml exec -T db pg_dump -U postgres -d agora > agora-backup.sql
```

O armazenamento persistente evita perder os dados em reinicializações comuns, mas não substitui um backup. A camada gratuita da Oracle pode ter indisponibilidade por falta de capacidade e prevê recuperação de máquinas Always Free consideradas ociosas. Portanto, acompanhe o servidor e mantenha o backup no computador.
