# 📚 Documentação Completa do Projeto

Bem-vindo à documentação completa do projeto. Este arquivo serve como índice
para todos os documentos de documentação.

---

## 📋 Índice de Documentação

### 1. Dicionário e Conceitos

| Arquivo                                                      | Descrição                                                                 |
| ------------------------------------------------------------ | ------------------------------------------------------------------------- |
| [dictionary/annotations.md](dictionary/annotations.md)       | Dicionário completo de todas as anotações (annotations) usadas no projeto |
| [dictionary/complex-topics.md](dictionary/complex-topics.md) | Tópicos avançados: Lambda, Streams, Optional, Design Patterns, JWT, etc.  |

### 2. Arquitetura da Aplicação

| Arquivo                                                              | Descrição                                         |
| -------------------------------------------------------------------- | ------------------------------------------------- |
| [controller-documentation.md](controller-documentation.md)           | Documentação de todos os Controllers (MVC e REST) |
| [service-documentation.md](service-documentation.md)                 | Documentação de todos os Services                 |
| [model-documentation.md](model-documentation.md)                     | Entidades, Repositories, DTOs e Mappers           |
| [security-documentation.md](security-documentation.md)               | Sistema de segurança, JWT e autenticação          |
| [design-patterns-documentation.md](design-patterns-documentation.md) | Padrões de projeto utilizados                     |

---

## 🚀 Visão Geral do Projeto

Este é um sistema de ** marketplace de livros** com as seguintes funcionalidades
principais:

### Funcionalidades

- **Cadastro e Autenticação**: Usuários podem se cadastrar e fazer login
- **Carteira de Tokens**: Sistema de tokens virtual para compras
- **Venda de Livros**: Usuários podem vender livros (sujeitos a aprovação)
- **Compra de Livros**: Sistema de compra com transferência de tokens
- **Avaliações**: Sistema de reviews e notas para livros
- **Administração**: Painel admin para aprovação de livros

### Arquitetura (ASCII Art)

```
Frontend (Thymeleaf)       Controllers API    Services        Model
+----------------+         +-------------+   +--------+     +----------+
| ClientCtrl     | ----->  | AuthCtrl    |-->| Client |---->| Repo     |
| IndexCtrl      |         | LivroCtrlApi|   | Service|     | DB (MySQL)
| AdminViewCtrl  |         | TokenCtrl   |   +--------+
+----------------+         +-------------+
```

**Atualizado**: Diagrama ASCII arquitetura textual.

---

## 📁 Estrutura de Diretórios

```
src/main/java/umc/exs/
├── config/              # Configurações (Security, Web, WebSocket)
├── controller/          # Controllers Thymeleaf (Views)
│   ├── ClientController.java
│   ├── AdminController.java
│   ├── AuthController.java
│   └── ...
├── controller/api/     # Controllers REST (JSON)
│   ├── LivroControllerApi.java
│   ├── AvaliacaoLivroController.java
│   └── TokenController.java
├── design/             # Design Patterns
│   ├── factory/        # Factory Pattern
│   └── strategy/       # Strategy Pattern
├── handler/            # Exception Handlers
├── log/                # Auditoria
├── model/
│   ├── entidades/      # Entidades JPA
│   │   ├── foundation/ # Entidades principais
│   │   └── usuario/    # Entidades de usuário
│   ├── daos/
│   │   ├── mappers/    # MapStruct
│   │   └── repository/ # JpaRepository
│   └── dtos/           # Data Transfer Objects
├── security/           # Componentes de segurança JWT
├── service/            # Camada de serviços
├── service/email/      # Serviços de e-mail
└── utils/              # Utilitários
```

---

## 🔄 Fluxo Principal

### 1. Cadastro de Usuário

```
Usuário → ClientController → ClienteService → ClienteRepository → Banco
```

### 2. Login

```
Usuário → AuthController → JwtUserDetailsService → PasswordEncoder
                                         ↓
                                   JwtUtil.generateToken()
                                         ↓
                                   Cookie HTTP-only
```

### 3. Compra de Tokens

```
Usuário → TokenController → PagamentoFactory → Strategy (Pix/Cartao)
                                              ↓
                                         ClienteService
                                              ↓
                                         TransacaoRepository
```

### 4. Venda de Livro

```
Usuário → LivroControllerApi → LivroService → LivroRepository
                                          ↓
                               AdminController (aprovação)
```

### 5. Compra de Livro

```
Usuário → LivroControllerApi → LivroService
                                   ↓
                            Valida saldo
                                   ↓
                            Transfere tokens
                                   ↓
                            Remove anúncio
```

---

## 🛠️ Tecnologias Utilizadas

| Tecnologia          | Uso                      |
| ------------------- | ------------------------ |
| **Java 17**         | Linguagem principal      |
| **Spring Boot**     | Framework web            |
| **Spring Security** | Segurança e autenticação |
| **Spring Data JPA** | Persistência de dados    |
| **Hibernate**       | ORM                      |
| **MySQL**           | Banco de dados           |
| **Thymeleaf**       | Templates HTML           |
| **Lombok**          | Redução de boilerplate   |
| **MapStruct**       | Mapeamento DTO/Entidade  |
| **JWT**             | Tokens de autenticação   |
| **BCrypt**          | Criptografia de senhas   |

---

## 📖 Glossário Rápido

| Termo          | Significado                                 |
| -------------- | ------------------------------------------- |
| **Controller** | Recebe requisições HTTP e retorna respostas |
| **Service**    | Contém lógica de negócio                    |
| **Repository** | Acessa o banco de dados                     |
| **DTO**        | Objeto para transferência de dados          |
| **Entity**     | Representa uma tabela no banco              |
| **JWT**        | Token para autenticação                     |
| **Strategy**   | Padrão para algoritmos intercambiáveis      |
| **Factory**    | Padrão para criação de objetos              |

---

## 📌 Comece por Aqui

### Novo no Projeto?

1. Leia **[dictionary/annotations.md](dictionary/annotations.md)** para entender
   as anotações
2. Veja **[controller-documentation.md](controller-documentation.md)** para
   entender as rotas
3. Leia **[security-documentation.md](security-documentation.md)** para entender
   a autenticação

### Quer Entender os Padrões de Código?

1. Leia **[design-patterns-documentation.md](design-patterns-documentation.md)**
2. Veja **[dictionary/complex-topics.md](dictionary/complex-topics.md)** para
   tópicos avançados

### Precisa Modificar algo?

- **Controllers**: [controller-documentation.md](controller-documentation.md)
- **Services**: [service-documentation.md](service-documentation.md)
- **Dados/Model**: [model-documentation.md](model-documentation.md)

---

## 📞 Links Úteis

- [Spring Boot Docs](https://docs.spring.io/spring-boot/docs/current/reference/)
- [Spring Security Docs](https://docs.spring.io/spring-security/reference/)
- [JWT.io](https://jwt.io/)
- [Lombok](https://projectlombok.org/)
- [MapStruct](https://mapstruct.org/)

---

_Documentação gerada automaticamente para o projeto._
