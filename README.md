# 📚 Bibliotroca

> Plataforma web de troca e venda de livros usados com sistema de tokens, gamificação e comunidade integrada.

---

## Sobre o Projeto

O **Bibliotroca** é uma aplicação web desenvolvida como Projeto Final de Curso (PFC) da Universidade de Mogi das Cruzes (UMC). A plataforma conecta leitores que desejam dar uma nova vida aos livros que já leram, permitindo a venda de exemplares usados por meio de uma moeda virtual chamada **Tokens Bibliotroca (T$)**.

O problema central que o projeto resolve é a dificuldade de circulação de livros usados entre leitores: obras que ficam paradas nas prateleiras poderiam estar sendo lidas por outras pessoas. A Bibliotroca cria um ecossistema onde o leitor recebe tokens ao vender livros e usa esses mesmos tokens para adquirir outros títulos, incentivando a rotatividade e o acesso à leitura.

O público-alvo são leitores de qualquer faixa etária que desejam expandir sua biblioteca de forma econômica e sustentável, bem como aqueles que querem monetizar (em tokens) livros que não leem mais. A plataforma inclui ainda uma comunidade com fórum de discussão e blog editorial.

---

## Funcionalidades Principais

### Visitante (não autenticado)
- Navegar na vitrine de livros disponíveis para compra
- Visualizar detalhes de livros e avaliações
- Ler posts do blog e tópicos do fórum
- Visualizar o ranking público de gamificação
- Acessar página de cadastro e login

### Cliente (autenticado)
- **Conta e perfil:** cadastro com CPF e e-mail validados, edição de perfil, gerenciamento de endereços de entrega e cartões, política de privacidade e termos de uso
- **Compra de livros:** vitrine com busca por ISBN (via Google Books API), carrinho de compras (estante), checkout com reserva temporária de 5 minutos, limite de 5 livros por checkout, confirmação com animação de fogos de artifício
- **Venda de livros:** envio de livro individual com foto de capa, envio em lote, acompanhamento do status de aprovação, histórico de vendas
- **Pedidos:** acompanhamento de status de envio (Aguardando → Em Trânsito → Entregue), código de rastreio, solicitação de cancelamento com motivo
- **Carteira:** recarga de tokens via PIX com QR Code (Mercado Pago), histórico de transações
- **Lista de desejos:** adicionar livros, receber notificação quando entram em estoque
- **Gamificação:** pontuação XP por ações, progressão por níveis (Iniciante → Bronze → Prata → Ouro), ranking público, conquista automática de cupons de desconto ao atingir 500 XP
- **Fórum:** criar tópicos por categoria, responder, curtir respostas, marcar melhor resposta, excluir própria resposta
- **Blog:** ler posts, curtir, comentar e excluir comentário próprio
- **Notificações:** sininho com notificações em tempo real via WebSocket, histórico persistido no dashboard
- **Recuperação de senha:** solicitação via e-mail com token de uso único

### Administrador
- **Dashboard:** KPIs de clientes, livros, pedidos, tokens e visitas
- **Painel de livros:** aprovação e rejeição de livros submetidos (com e-mail ao vendedor), gestão de lotes, estoque
- **Painel de pedidos:** visualização completa, atualização de status, inserção de código de rastreio, cancelamento com e-mail ao comprador, geração de etiqueta de envio em PDF
- **Cupons:** criar, ativar, desativar e listar cupons de desconto
- **Clientes:** visualizar perfil completo, bloquear conta
- **Cancelamentos:** analisar e processar solicitações de cancelamento
- **Blog e fórum:** moderação — excluir posts, tópicos e respostas
- **Auditoria:** visualizar e filtrar logs por data, usuário e tipo de ação, exportar relatório PDF
- **Notificações:** envio de broadcast de e-mails para segmentos de usuários

---

## Stack Tecnológica

| Camada | Tecnologia | Versão |
|--------|-----------|--------|
| Linguagem | Java | 21 LTS |
| Framework principal | Spring Boot | 3.5.14 |
| MVC e REST | Spring Web MVC | (via Boot) |
| Segurança | Spring Security | (via Boot) |
| Persistência | Spring Data JPA / Hibernate | (via Boot) |
| Template engine | Thymeleaf | (via Boot) |
| Banco (local/dev) | SQLite | 3.46.1.3 |
| Banco (produção) | PostgreSQL | 42.7.3 |
| JWT | JJWT | 0.12.6 |
| Hash de senha | jBCrypt | 0.4 |
| Mapeamento DTO | MapStruct | 1.5.5.Final |
| Redução de boilerplate | Lombok | 1.18.38 |
| Geração de PDF | OpenPDF | 1.3.30 |
| WebSocket | Spring WebSocket + Spring Messaging | (via Boot) |
| Documentação API | SpringDoc OpenAPI (Swagger UI) | 2.8.5 |
| E-mail | Spring Mail + JavaMail | (via Boot) |
| Variáveis de ambiente | dotenv-java | 3.0.0 |
| Análise estática | SpotBugs Maven Plugin | 4.8.6.0 |
| Testes | JUnit 5 + Mockito + Spring Security Test | (via Boot) |

### APIs Externas Integradas

| API | Finalidade |
|-----|-----------|
| Google Books API | Busca de metadados de livros por ISBN |
| OpenLibrary | Fallback para metadados quando Google Books não retorna |
| ViaCEP | Auto-completar endereço ao digitar CEP |
| Mercado Pago SDK | Geração de QR Code PIX e processamento de pagamentos |
| Gmail SMTP | Envio de e-mails transacionais |

---

## Arquitetura

O projeto segue o padrão de camadas clássico do Spring Boot:

```
Controller (HTTP / Thymeleaf)
    └── Service (regras de negócio, @Transactional)
            └── Repository (Spring Data JPA)
                    └── Banco de dados (SQLite local / PostgreSQL produção)
```

### Estrutura de Pacotes

```
src/
├── main/
│   ├── java/umc/exs/
│   │   ├── config/          # SecurityConfig, WebSocketConfig, etc.
│   │   ├── controller/
│   │   │   ├── api/         # Endpoints REST (/api/**)
│   │   │   └── web/         # Controllers MVC (páginas Thymeleaf)
│   │   ├── dto/
│   │   │   ├── request/     # DTOs de entrada
│   │   │   └── response/    # DTOs de saída
│   │   ├── handler/         # GlobalExceptionHandler (@ControllerAdvice)
│   │   ├── model/
│   │   │   ├── entidades/   # Entidades JPA por domínio
│   │   │   │   ├── foundation/  # Pedido, Cupom, Lote, Transacao, ...
│   │   │   │   ├── livro/       # Livro, Obra, AvaliacaoLivro
│   │   │   │   ├── logic/       # Administrador, LogAuditoria, RecuperacaoSenha
│   │   │   │   ├── social/      # TopicoForum, RespostaForum, PostBlog, PontuacaoUsuario
│   │   │   │   └── usuario/     # Cliente, Endereco, Cartao
│   │   │   ├── enums/       # NivelUsuario, StatusEnvio, CategoriaForum, ...
│   │   │   └── mapper/      # MapStruct mappers
│   │   ├── repository/      # Interfaces Spring Data JPA
│   │   ├── security/        # JwtUtil, JwtRequestFilter, RateLimitFilter, ...
│   │   └── service/         # Lógica de negócio por domínio
│   └── resources/
│       ├── application.properties          # Configuração base + perfil ativo
│       ├── application-local.yml           # Configuração local (SQLite, SMTP, dev)
│       ├── application-render.yml          # Configuração produção (variáveis de ambiente)
│       ├── templates/                      # Templates Thymeleaf (43 arquivos HTML)
│       │   ├── admin/                      # Painel administrativo
│       │   ├── cliente/                    # Área do cliente
│       │   ├── produto/                    # Vitrine, checkout, pedido, venda
│       │   ├── forum/                      # Fórum da comunidade
│       │   ├── blog/                       # Blog editorial
│       │   ├── fragments/                  # Navbar, sidebar, loader (reutilizáveis)
│       │   └── error/                      # Páginas 404 e 500 customizadas
│       ├── static/
│       │   ├── css/                        # Estilos por módulo
│       │   └── js/                         # Scripts por módulo (41 arquivos)
│       └── certs/                          # Keystore HTTPS (não versionado)
└── test/
    └── java/umc/exs/                       # 20 classes de teste
```

### Perfis de Execução

| Perfil | Banco | SSL | Uso |
|--------|-------|-----|-----|
| `local` | SQLite (`user.db`) | Habilitado (porta 8443) | Desenvolvimento |
| `render` | PostgreSQL (variáveis de ambiente) | Gerenciado pelo Render | Produção |

O perfil ativo é controlado pela variável `PROFILE` em `application.properties`:
```properties
spring.profiles.active=${PROFILE:local}
```

---

## Requisitos

- **Java 21 LTS** (recomendado: Amazon Corretto, Eclipse Temurin ou Oracle JDK 21)
- **Maven 3.8+**
- **Git**
- Para execução local: nenhum banco externo necessário (SQLite é embutido)
- Para envio de e-mails: conta Gmail com senha de aplicativo configurada

---

## Como Executar Localmente

### 1. Clonar o repositório

```bash
git clone <url-do-repositorio>
cd PFC_umc-main
```

### 2. Configurar `application-local.yml`

Crie ou edite o arquivo `src/main/resources/application-local.yml` com os valores do seu ambiente. O projeto já inclui um arquivo de exemplo — substitua os valores marcados:

```yaml
spring:
  mail:
    username: "seu-email@gmail.com"
    password: "sua-senha-de-aplicativo-gmail"  # Gerar em myaccount.google.com/apppasswords

jwt:
  secret: "uma-chave-secreta-de-pelo-menos-32-caracteres"

mercadopago:
  access-token: "APP_USR-seu-token-mercadopago"

google:
  books:
    api-key: "sua-chave-google-books-api"
```

> **Atenção:** nunca comite este arquivo com credenciais reais. Ele deve estar no `.gitignore`.

### 3. Compilar e executar

```bash
# Compilar e rodar com o perfil local
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Alternativamente, gerar o JAR e executar
mvn clean package -DskipTests
java -jar target/exs-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

### 4. Acessar a aplicação

| URL | Descrição |
|-----|-----------|
| `https://localhost:8443` | Aplicação principal (HTTPS) |
| `https://localhost:8443/entrar` | Página de login do cliente |
| `https://localhost:8443/admin/login` | Login do administrador |
| `https://localhost:8443/swagger-ui.html` | Documentação interativa da API REST |

> O certificado autoassinado usado no perfil local pode gerar aviso no navegador — clique em "Avançado → Continuar assim mesmo".

### 5. Executar apenas os testes

```bash
mvn test
```

---

## Variáveis de Ambiente

Usadas no perfil `render` (produção). Em desenvolvimento local, os valores equivalentes ficam em `application-local.yml`.

| Variável | Descrição | Exemplo |
|----------|-----------|---------|
| `DATABASE_URL_JDBC` | URL JDBC do banco PostgreSQL | `jdbc:postgresql://host/db` |
| `DATABASE_USERNAME` | Usuário do banco | `postgres` |
| `DATABASE_PASSWORD` | Senha do banco | `senha-forte` |
| `JWT_SECRET` | Chave HMAC para assinar tokens JWT (mín. 32 chars) | `chave-secreta-aleatoria-longa` |
| `JWT_EXPIRATION` | Duração do token em milissegundos | `86400000` (24h) |
| `MAIL_HOST` | Servidor SMTP | `smtp.gmail.com` |
| `MAIL_PORT` | Porta SMTP | `587` |
| `MAIL_USERNAME` | E-mail remetente | `noreply@exemplo.com` |
| `MAIL_PASSWORD` | Senha de aplicativo do e-mail | `xxxx xxxx xxxx xxxx` |
| `APP_BASE_URL` | URL base da aplicação em produção | `https://bibliotroca.onrender.com` |
| `PORT` | Porta HTTP (definida automaticamente pelo Render) | `8080` |
| `MERCADOPAGO_ACCESS_TOKEN` | Token de acesso do Mercado Pago | `APP_USR-...` |
| `GOOGLE_BOOKS_API_KEY` | Chave da Google Books API | `AIzaSy...` |

---

## Segurança

### Mecanismos implementados

- **Autenticação:** JWT (JJWT 0.12.6) armazenado em cookie `HttpOnly + Secure + SameSite=Strict`, sem exposição em `localStorage`
- **Senhas:** hash com BCrypt; nunca armazenadas em texto puro
- **CPF:** criptografado em repouso com AES-GCM via `AttributeConverter` JPA
- **Rate limiting:** `RateLimitFilter` limita tentativas de login por IP; conta é bloqueada após 5 tentativas consecutivas com falha
- **Upload de imagens:** validação em 3 camadas — tamanho máximo (10 MB), MIME type (allowlist: JPEG, PNG, WebP) e magic bytes reais
- **Concorrência em compras:** `@Lock(PESSIMISTIC_WRITE)` no repositório de livros previne compra simultânea do mesmo exemplar
- **CORS:** origens permitidas configuradas explicitamente; sem wildcard `*`
- **HTTPS:** keystore PKCS12 com certificado autoassinado no perfil local; TLS gerenciado pelo Render em produção
- **Tratamento de erros:** `GlobalExceptionHandler` nunca expõe stack trace ao usuário; mensagens genéricas em produção (`server.error.include-stacktrace: never`)
- **Auditoria:** `LogAuditoriaService` registra ações críticas sem persistir dados sensíveis (senha, CPF, cartão)
- **Dados de cartão:** não armazenados — processamento delegado inteiramente ao Mercado Pago SDK

### LGPD

| Requisito | Implementação |
|-----------|--------------|
| Direito ao esquecimento | Soft-delete: `Cliente.deletedAt` + `Cliente.ativo = false` |
| Retenção de dados | `Pedido.dataRetencaoExpira` = 5 anos após criação (Art. 16 LGPD) |
| Minimização de dados | Ranking público exibe apenas primeiro nome e inicial do sobrenome |
| Proteção de dados sensíveis | CPF criptografado (AES-GCM); CVV nunca persistido |
| Transparência | Páginas de Política de Privacidade e Termos de Uso disponíveis |

---

## Testes

### Executar

```bash
# Todos os testes
mvn test

# Apenas testes unitários (por convenção de nome)
mvn test -Dtest="*UnitTest"

# Apenas testes de integração
mvn test -Dtest="*IntegrationTest"
```

### Cobertura atual (20 classes de teste)

| Pacote | Tipo | Classes |
|--------|------|---------|
| `controller_api` | Unitário + Integração | `LivroControllerApiUnitTest`, `LivroControllerApiIntegrationTest` |
| `controller_web` | Unitário + Integração | Admin, Audit, Client, Forum, Index, LivroStory, LivroView (7 pares) |
| `handler` | Unitário | `GlobalExceptionHandlerTest` — testa BusinessException, erros genéricos, 404, 500 |
| `integration` | Integração | `ConcorrenciaCompraIntegrationTest` — testa race condition em compra simultânea |
| `security` | Unitário | `JwtUtilTest` — 8 casos: geração, extração de claims, validação, tokens inválidos |
| `service` | Unitário | `LivroCompraServiceTest` — saldo insuficiente, auto-compra, fluxo de carrinho |

> O banco H2 em memória é utilizado nos testes de integração, evitando dependência do SQLite local.

---

## APIs Externas

### Google Books API
- **Uso:** busca automática de metadados (título, autor, capa, descrição) ao informar ISBN no formulário de venda
- **Configuração:** obter chave em [console.cloud.google.com](https://console.cloud.google.com) → APIs → Books API → Credenciais
- **Configurar em:** `application-local.yml` → `google.books.api-key`

### OpenLibrary
- **Uso:** fallback quando Google Books não retorna resultado para o ISBN informado
- **Configuração:** nenhuma — API pública sem necessidade de chave

### ViaCEP
- **Uso:** auto-completar endereço ao digitar CEP no formulário de cadastro e edição de endereço
- **Configuração:** nenhuma — API pública sem necessidade de chave

### Mercado Pago
- **Uso:** geração de QR Code PIX para recarga de tokens; processamento de pagamentos via webhook
- **Configuração:** obter access token em [mercadopago.com.br/developers](https://www.mercadopago.com.br/developers)
- **Configurar em:** `application-local.yml` → `mercadopago.access-token`

### Gmail SMTP
- **Uso:** envio de todos os e-mails transacionais (confirmação de compra, recuperação de senha, aprovação de livro, atualização de pedido, cancelamento)
- **Configuração:** habilitar verificação em duas etapas na conta Google → gerar senha de aplicativo em [myaccount.google.com/apppasswords](https://myaccount.google.com/apppasswords)
- **Configurar em:** `application-local.yml` → `spring.mail.username` e `spring.mail.password`

---

## Estrutura do Projeto

```
PFC_umc-main/
├── pom.xml                          # Dependências e build Maven
├── README.md                        # Este arquivo
├── .gitignore
├── src/
│   ├── main/
│   │   ├── java/umc/exs/            # Código-fonte principal
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── application-local.yml   # NÃO versionado com credenciais reais
│   │       ├── application-render.yml  # Produção via variáveis de ambiente
│   │       ├── certs/               # Keystore HTTPS (não versionado)
│   │       ├── templates/           # 43 templates Thymeleaf
│   │       └── static/
│   │           ├── css/             # Estilos por módulo
│   │           └── js/              # 41 arquivos JavaScript por módulo
│   └── test/
│       └── java/umc/exs/            # 20 classes de teste (unitários + integração)
└── logs/                            # Logs de execução (não versionado)
```

---

## Equipe

| Nome | Função |
|------|--------|
| Guilherme Fernando Pires dos Santos | Desenvolvimento |
| João da Cruz Gallo Junior | Desenvolvimento |
| Pedro Henrique Fonseca Melo | Desenvolvimento |
| Thiago Henrique Yaginuma Okabayashi | Desenvolvimento |

**Instituição:** Universidade de Mogi das Cruzes (UMC)  
**Curso:** Engenharia de Software  
**Ano:** 2026

---

## Licença

Este projeto foi desenvolvido para fins acadêmicos (PFC — Projeto Final de Curso).  
Todos os direitos reservados aos autores © 2026.
