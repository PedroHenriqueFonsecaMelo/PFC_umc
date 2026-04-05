# 📂 Documentação de Controllers

## O que é um Controller?

**Controllers** são componentes fundamentais em aplicações web que seguem o
padrão **MVC (Model-View-Controller)**. No contexto do Spring Framework,
controllers são responsáveis por **receber requisições HTTP**, **processá-las**
(geralmente delegando para Services) e **retornar uma resposta** ao cliente.

### Responsabilidades do Controller

1. **Receber Requisições**: Interceptar chamadas HTTP (GET, POST, PUT, DELETE)
2. **Validar Entrada**: Verificar se os dados recebidos estão corretos
3. **Delegar Processamento**: Encaminhar para a camada de serviço (Service)
4. **Retornar Resposta**: Enviar o resultado de volta ao cliente (HTML para
   Thymeleaf ou JSON para APIs REST)

### Tipos de Controllers no Projeto

#### 1. View Controllers (Thymeleaf)

Retornam **Views (HTML)** usando templates Thymeleaf. Usam a anotação
`@Controller`.

#### 2. REST Controllers (API)

Retornam **dados (JSON/XML)** diretamente no corpo da resposta. Usam a anotação
`@RestController`.

---

## Controllers do Projeto

### 1. ClientController

**Localização**: `src/main/java/umc/exs/controller/ClientController.java`

**Tipo**: View Controller (`@Controller`)

**O que é**: Responsável por toda a gestão de clientes/usuários, incluindo
cadastro, login, perfil, carteira e recuperação de senha.

**Anotações principais**:

- `@Controller` - Define como controller de views
- `@RequestMapping("/clientes")` - Prefixo de todas as rotas

**Endpoints**:

| Método | Rota                          | Descrição                      |
| ------ | ----------------------------- | ------------------------------ |
| GET    | `/clientes/novo-cadastro`     | Exibe formulário de cadastro   |
| POST   | `/clientes/novo-cadastro`     | Processa cadastro básico       |
| POST   | `/clientes/cadastro-completo` | Cadastro com endereço e cartão |
| GET    | `/clientes/login`             | Exibe página de login          |
| POST   | `/clientes/login`             | Processa autenticação          |
| GET    | `/clientes/sair`              | Realiza logout                 |
| GET    | `/clientes/meu-perfil`        | Exibe perfil do usuário        |
| POST   | `/clientes/atualizar`         | Atualiza dados do perfil       |
| POST   | `/clientes/deletar`           | Deleta conta do usuário        |
| GET    | `/clientes/carteira`          | Exibe carteira de tokens       |
| POST   | `/clientes/comprar-tokens`    | Compra tokens                  |
| GET    | `/clientes/recuperar-senha`   | Página de recuperação          |
| POST   | `/clientes/recuperar-senha`   | Inicia processo de recuperação |
| GET    | `/clientes/reset-senha`       | Formulário de nova senha       |
| POST   | `/clientes/alterar-senha`     | Altera senha com token         |

**Dependências injetadas**:

- `ClienteService` - Lógica de negócio de clientes
- `LogAuditoriaService` - Auditoria de ações
- `AuthHelper` - Auxiliar de autenticação
- `JwtUtil` - Utilitário de JWT

---

### 2. AuthController

**Localização**: `src/main/java/umc/exs/controller/AuthController.java`

**Tipo**: REST Controller (`@RestController`)

**O que é**: API REST para autenticação e registro de usuários. Retorna JSON em
vez de views.

**Anotações principais**:

- `@RestController` - Define como API REST
- `@RequestMapping("/auth")` - Prefixo das rotas de autenticação

**Endpoints**:

| Método | Rota             | Descrição                       |
| ------ | ---------------- | ------------------------------- |
| POST   | `/auth/login`    | Autentica usuário e retorna JWT |
| POST   | `/auth/register` | Registra novo cliente           |

**Funcionalidades**:

- Geração de token JWT
- Criptografia de senha com BCrypt
- Armazenamento de token em cookie HTTP-only
- Validação de credenciais

**Dependências injetadas**:

- `JwtUtil` - Geração/validação de JWT
- `JwtUserDetailsService` - Carregamento de usuários
- `ClienteService` - Persistência de clientes
- `PasswordEncoder` - Criptografia de senhas

---

### 3. AdminController

**Localização**: `src/main/java/umc/exs/controller/AdminController.java`

**Tipo**: REST Controller (`@RestController`)

**O que é**: API REST para gerenciamento administrativo de livros (aprovação e
rejeição).

**Anotações principais**:

- `@RestController` - API REST
- `@RequestMapping("/api/admin")` - Prefixo da API

**Endpoints**:

| Método | Rota                              | Descrição                         |
| ------ | --------------------------------- | --------------------------------- |
| GET    | `/api/admin/livros/pendentes`     | Lista livros aguardando aprovação |
| POST   | `/api/admin/livros/{id}/aprovar`  | Aprova um livro                   |
| POST   | `/api/admin/livros/{id}/rejeitar` | Rejeita um livro                  |

**Funcionalidades**:

- Listar livros pendentes de aprovação
- Aprovar livros definindo preço e estado
- Rejeitar livros com comentário
- Validação de preço por estado (evitar preços abusivos)
- Auditoria de todas as ações administrativas

**Dependências injetadas**:

- `LivroService` - Lógica de negócio de livros
- `AdminRepository` - Acesso a dados de administradores

---

### 4. AdminViewController

**Localização**: `src/main/java/umc/exs/controller/AdminViewController.java`

**Tipo**: View Controller (`@Controller`)

**O que é**: Páginas HTML para o painel administrativo.

**Endpoints**:

| Método | Rota            | Descrição             |
| ------ | --------------- | --------------------- |
| GET    | `/admin/painel` | Painel administrativo |
| GET    | `/admin/sair`   | Logout do admin       |

---

### 5. LivroViewController

**Localização**: `src/main/java/umc/exs/controller/LivroViewController.java`

**Tipo**: View Controller (`@Controller`)

**O que é**: Páginas de visualização e venda de livros.

**Endpoints**:

| Método | Rota              | Descrição                      |
| ------ | ----------------- | ------------------------------ |
| GET    | `/livros/vender`  | Formulário de venda de livro   |
| GET    | `/livros/vitrine` | Vitrine com livros disponíveis |

---

### 6. LivroStoryController

**Localização**: `src/main/java/umc/exs/controller/LivroStoryController.java`

**Tipo**: View Controller (`@Controller`)

**O que é**: Página de história do livro (histórico, avaliações e notas).

**Endpoints**:

| Método | Rota                      | Descrição                     |
| ------ | ------------------------- | ----------------------------- |
| GET    | `/livros/{isbn}/historia` | Página de avaliações do livro |

---

### 7. IndexController

**Localização**: `src/main/java/umc/exs/controller/IndexController.java`

**Tipo**: View Controller (`@Controller`)

**O que é**: Controlador da página inicial.

---

### 8. AuditController

**Localização**: `src/main/java/umc/exs/controller/AuditController.java`

**Tipo**: View Controller (`@Controller`)

**O que é**: Página de auditoria/extrato de ações do cliente.

**Endpoints**:

| Método | Rota                      | Descrição           |
| ------ | ------------------------- | ------------------- |
| GET    | `/historico/cliente`      | Página de auditoria |
| GET    | `/historico/cliente/json` | API de logs em JSON |

**Funcionalidades**:

- Listar todas as ações do usuário (login, compras, cadastros)
- Exibir em formato HTML ou JSON

---

### 9. SecurityDebugController

**Localização**: `src/main/java/umc/exs/controller/SecurityDebugController.java`

**Tipo**: REST Controller

**O que é**: Controller de debug para testes de segurança (em ambiente de
desenvolvimento).

---

## API Controllers (REST)

### 10. LivroControllerApi

**Localização**: `src/main/java/umc/exs/controller/api/LivroControllerApi.java`

**Tipo**: REST Controller

**O que é**: API para operações com livros (venda, listagem, compra).

**Endpoints**:

| Método | Rota                       | Descrição               |
| ------ | -------------------------- | ----------------------- |
| POST   | `/api/livros/vender`       | Criar anúncio de venda  |
| GET    | `/api/livros/todos`        | Listar livros aprovados |
| POST   | `/api/livros/{id}/comprar` | Comprar livro           |

**Características especiais**:

- `consumes = MediaType.MULTIPART_FORM_DATA_VALUE` - Aceita upload de arquivo
  (foto)
- `@RequestPart` - Para receber multipart data

---

### 11. AvaliacaoLivroController

**Localização**:
`src/main/java/umc/exs/controller/api/AvaliacaoLivroController.java`

**Tipo**: REST Controller

**O que é**: API para gerenciar avaliações de livros.

**Endpoints**:

| Método | Rota                                 | Descrição                  |
| ------ | ------------------------------------ | -------------------------- |
| POST   | `/api/avaliacoes`                    | Criar avaliação            |
| GET    | `/api/avaliacoes/livro/{isbn}`       | Listar avaliações por ISBN |
| GET    | `/api/avaliacoes/livro/{isbn}/media` | Média de notas             |

---

### 12. TokenController

**Localização**: `src/main/java/umc/exs/controller/api/TokenController.java`

**Tipo**: REST Controller

**O que é**: API para compra de tokens e verificação de pagamentos.

**Endpoints**:

| Método | Rota                                            | Descrição                    |
| ------ | ----------------------------------------------- | ---------------------------- |
| POST   | `/api/tokens/comprar`                           | Comprar tokens               |
| GET    | `/api/tokens/historico`                         | Histórico de transações      |
| GET    | `/api/tokens/verificar-pagamento/{pagamentoId}` | Verificar status PIX         |
| GET    | `/api/tokens/simular-webhook/{pagamentoId}`     | Simular webhook de pagamento |

**Funcionalidades**:

- Integração com Factory de pagamento (Strategy Pattern)
- Suporte a PIX e Cartão
- Simulação de webhook para testes

---

## Resumo dos Controllers

| Controller               | Tipo | Prefixo           | Função Principal                  |
| ------------------------ | ---- | ----------------- | --------------------------------- |
| ClientController         | View | `/clientes`       | Cadastro, login, perfil, carteira |
| AuthController           | REST | `/auth`           | API de autenticação               |
| AdminController          | REST | `/api/admin`      | API de aprovação de livros        |
| AdminViewController      | View | `/admin`          | Páginas admin                     |
| LivroViewController      | View | `/livros`         | Vender e Vitrine                  |
| LivroStoryController     | View | `/livros`         | Avaliações                        |
| AuditController          | View | `/historico`      | Auditoria                         |
| LivroControllerApi       | REST | `/api/livros`     | API de livros                     |
| AvaliacaoLivroController | REST | `/api/avaliacoes` | API de avaliações                 |
| TokenController          | REST | `/api/tokens`     | API de tokens/pagamento           |

---

## Fluxo de Requisição Típico

```
1. Usuário envia requisição
       ↓
2. Security Filter valida JWT
       ↓
3. Controller recebe requisição
       ↓
4. Valida dados de entrada (@Valid)
       ↓
5. Chama Service (lógica de negócio)
       ↓
6. Service acessa Repository (banco)
       ↓
7. Repository retorna dados
       ↓
8. Service processa e retorna
       ↓
9. Controller retorna resposta (View ou JSON)
       ↓
10. Usuário recebe resposta
```
