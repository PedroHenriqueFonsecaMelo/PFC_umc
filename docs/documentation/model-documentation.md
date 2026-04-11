# 📂 Documentação de Model

Esta seção documenta a camada de modelo do projeto, incluindo:

1. **Entidades** - Classes que mapeiam tabelas do banco de dados
2. **Repositories** - Interfaces para acesso a dados (DAO Pattern)
3. **DTOs** - Objetos para transferência de dados
4. **Mappers** - Conversores entre entidades e DTOs

---

## O que são Entidades?

**Entidades** são classes Java anotadas com `@Entity` que representam tabelas no
banco de dados. Cada instância de uma entidade corresponde a uma linha na
tabela.

### Características das Entidades

- Anotadas com `@Entity` do JPA/Hibernate
- Têm uma chave primária (`@Id`)
- Mapeiam colunas para campos (`@Column`)
- Representam relacionamentos entre tabelas (`@ManyToOne`, `@OneToMany`, etc.)

---

## Entidades do Projeto

### 1. Cliente (Entidade de Usuário)

**Localização**: `src/main/java/umc/exs/model/entidades/usuario/Cliente.java`

**Descrição**: Entidade principal que representa os usuários/clientes do
sistema.

**Tabela**: `users`

**Campos**:

| Campo       | Tipo          | Descrição                    |
| ----------- | ------------- | ---------------------------- |
| id          | Long          | Chave primária               |
| senha       | String        | Senha criptografada (BCrypt) |
| nome        | String        | Nome completo                |
| datanasc    | String        | Data de nascimento           |
| gen         | Genero        | Gênero (M, F, OUTRO)         |
| cpf         | String        | CPF único                    |
| email       | String        | Email único                  |
| tentativas  | int           | Tentativas de login falhadas |
| bloqueada   | boolean       | Status de bloqueio           |
| dataCriacao | LocalDateTime | Data de cadastro             |
| saldoTokens | Double        | Saldo de tokens              |

**Relacionamentos**:

| Tipo       | Entidade | Descrição            |
| ---------- | -------- | -------------------- |
| ManyToMany | Cartao   | Cartões do cliente   |
| ManyToMany | Endereco | Endereços do cliente |

**Métodos de Negócio**:

```java
public void registrarFalhaLogin() {
    this.tentativas++;
    if (this.tentativas >= 5) {
        this.bloqueada = true;
    }
}

public void resetarTentativas() {
    this.tentativas = 0;
    this.bloqueada = false;
}
```

---

### 2. Endereco

**Localização**: `src/main/java/umc/exs/model/entidades/usuario/Endereco.java`

**Descrição**: Entidade que representa endereços.

**Campos**:

| Campo          | Tipo   | Descrição                |
| -------------- | ------ | ------------------------ |
| id             | Long   | Chave primária           |
| pais           | String | País                     |
| cep            | String | CEP                      |
| estado         | String | Estado                   |
| cidade         | String | Cidade                   |
| rua            | String | Rua                      |
| bairro         | String | Bairro                   |
| numero         | String | Número                   |
| complemento    | String | Complemento              |
| tipoResidencia | String | Tipo (casa, apartamento) |

**Relacionamentos**:

| Tipo       | Entidade | Descrição                       |
| ---------- | -------- | ------------------------------- |
| ManyToMany | Cliente  | Clientes que usam este endereço |

---

### 3. Cartao

**Localização**: `src/main/java/umc/exs/model/entidades/usuario/Cartao.java`

**Descrição**: Entidade que representa cartões de crédito.

**Campos**:

| Campo       | Tipo   | Descrição                   |
| ----------- | ------ | --------------------------- |
| id          | Long   | Chave primária              |
| numero      | String | Número do cartão (único)    |
| bandeira    | String | Bandeira (Visa, Mastercard) |
| nomeTitular | String | Nome do titular             |
| validade    | String | Validade (MM/AA)            |
| cpfTitular  | String | CPF do titular              |

**Relacionamentos**:

| Tipo       | Entidade | Descrição                     |
| ---------- | -------- | ----------------------------- |
| ManyToMany | Cliente  | Clientes que usam este cartão |

---

### 4. LivroAnuncio

**Localização**:
`src/main/java/umc/exs/model/entidades/foundation/LivroAnuncio.java`

**Descrição**: Entidade que representa um anúncio de livro à venda.

**Campos**:

| Campo               | Tipo          | Descrição                  |
| ------------------- | ------------- | -------------------------- |
| id                  | Long          | Chave primária             |
| titulo              | String        | Título do livro            |
| autor               | String        | Autor                      |
| isbn                | String        | ISBN                       |
| fotoUrl             | String        | URL da foto                |
| vendedor            | Cliente       | Vendedor (ManyToOne)       |
| dataAnuncio         | LocalDateTime | Data do anúncio            |
| aprovado            | Boolean       | Status de aprovação        |
| precoAprovado       | Double        | Preço definido pelo admin  |
| estadoAprovado      | EstadoLivro   | Estado definido pelo admin |
| comentarioAprovacao | String        | Comentário do admin        |
| dataAprovacao       | LocalDateTime | Data de aprovação          |
| adminAprovadorId    | Long          | ID do admin aprovador      |

**Relacionamentos**:

| Tipo      | Entidade | Descrição         |
| --------- | -------- | ----------------- |
| ManyToOne | Cliente  | Vendedor do livro |

---

### 5. Administrador

**Localização**:
`src/main/java/umc/exs/model/entidades/foundation/Administrador.java`

**Descrição**: Entidade que representa administradores do sistema.

**Tabela**: `admins`

**Campos**:

| Campo    | Tipo   | Descrição      |
| -------- | ------ | -------------- |
| id       | Long   | Chave primária |
| nome     | String | Nome           |
| email    | String | Email          |
| password | String | Senha          |

---

### 6. Transacao

**Localização**:
`src/main/java/umc/exs/model/entidades/foundation/Transacao.java`

**Descrição**: Entidade que representa transações de tokens.

**Campos**:

| Campo           | Tipo          | Descrição                    |
| --------------- | ------------- | ---------------------------- |
| id              | Long          | Chave primária               |
| cliente         | Cliente       | Cliente (ManyToOne)          |
| valor           | Double        | Valor em tokens              |
| dataHora        | LocalDateTime | Data/hora                    |
| metodoPagamento | String        | Método (PIX, CARTAO)         |
| finalCartao     | String        | 4 últimos dígitos            |
| pagamentoId     | String        | ID do pagamento PIX          |
| status          | String        | Status (PENDENTE, CONCLUIDO) |

---

### 7. AvaliacaoLivro

**Localização**:
`src/main/java/umc/exs/model/entidades/foundation/AvaliacaoLivro.java`

**Descrição**: Entidade que representa avaliações de livros.

**Campos**:

| Campo         | Tipo          | Descrição           |
| ------------- | ------------- | ------------------- |
| id            | Long          | Chave primária      |
| isbn          | String        | ISBN do livro       |
| tituloLivro   | String        | Título do livro     |
| nota          | Integer       | Nota (1-5)          |
| comentario    | String        | Comentário          |
| dataAvaliacao | LocalDateTime | Data da avaliação   |
| avaliador     | Cliente       | Cliente que avaliou |

---

### 8. LogAuditoria

**Localização**:
`src/main/java/umc/exs/model/entidades/foundation/LogAuditoria.java`

**Descrição**: Entidade para registro de auditoria de ações.

**Campos**:

| Campo        | Tipo          | Descrição        |
| ------------ | ------------- | ---------------- |
| id           | Long          | Chave primária   |
| idUsuario    | Long          | ID do usuário    |
| emailUsuario | String        | Email do usuário |
| acao         | String        | Ação realizada   |
| detalhes     | String        | Detalhes         |
| dataHora     | LocalDateTime | Data/hora        |

---

### 9. RecuperacaoSenha

**Localização**:
`src/main/java/umc/exs/model/entidades/foundation/RecuperacaoSenha.java`

**Descrição**: Entidade para tokens de recuperação de senha.

**Campos**:

| Campo         | Tipo          | Descrição           |
| ------------- | ------------- | ------------------- |
| id            | Long          | Chave primária      |
| token         | String        | Token único         |
| email         | String        | Email               |
| cliente       | Cliente       | Cliente (ManyToOne) |
| dataExpiracao | LocalDateTime | Data de expiração   |

---

## Enums

### Genero

**Localização**:
`src/main/java/umc/exs/model/entidades/foundation/enums/Genero.java`

```java
public enum Genero {
    M,  // Masculino
    F,  // Feminino
    OUTRO // Outro / Não-binário
}
```

### EstadoLivro

**Localização**:
`src/main/java/umc/exs/model/entidades/foundation/enums/EstadoLivro.java`

```java
public enum EstadoLivro {
    NOVO,     // Livro novo
    OTIMO,    // Excelente condição
    BOM,      // Boa condição
    DESGASTADO // Desgastado (preço máximo de 50 tokens)
}
```

---

## Repositories

**O que são**: Interfaces que estendem `JpaRepository` para operações de banco
de dados.

### 1. ClienteRepository

**Localização**:
`src/main/java/umc/exs/model/daos/repository/ClienteRepository.java`

**Métodos principais**:

| Método                                    | Descrição                         |
| ----------------------------------------- | --------------------------------- |
| `findByEmail(String email)`               | Busca por email (com EntityGraph) |
| `findByCpf(String cpf)`                   | Busca por CPF                     |
| `findByEmailAndId(String email, Long id)` | Busca por email e ID              |
| `findByIdWithCartoes(Long id)`            | Busca com cartões                 |
| `findByIdWithEnderecos(Long id)`          | Busca com endereços               |
| `existsByEmail(String email)`             | Verifica existência por email     |

---

### 2. LivroRepository

**Localização**:
`src/main/java/umc/exs/model/daos/repository/LivroRepository.java`

**Métodos principais**:

| Método                             | Descrição                  |
| ---------------------------------- | -------------------------- |
| `findByAprovadoTrue()`             | Livros aprovados (vitrine) |
| `findByAprovadoFalse()`            | Livros pendentes (admin)   |
| `findByIdAndAprovadoTrue(Long id)` | Livro específico aprovado  |

---

### 3. TransacaoRepository

**Localização**:
`src/main/java/umc/exs/model/daos/repository/TransacaoRepository.java`

**Métodos principais**:

| Método                                        | Descrição                     |
| --------------------------------------------- | ----------------------------- |
| `findByClienteIdOrderByDataHoraDesc(Long id)` | Histórico do cliente          |
| `findByPagamentoId(String pagamentoId)`       | Transação por ID de pagamento |

---

### 4. AvaliacaoLivroRepository

**Localização**:
`src/main/java/umc/exs/model/daos/repository/AvaliacaoLivroRepository.java`

**Métodos principais**:

| Método                                             | Descrição           |
| -------------------------------------------------- | ------------------- |
| `findAll()`                                        | Todas as avaliações |
| `existsByIsbnAndAvaliadorId(String isbn, Long id)` | Verifica se avaliou |

---

### 5. LogAuditoriaRepository

**Localização**:
`src/main/java/umc/exs/model/daos/repository/LogAuditoriaRepository.java`

**Métodos principais**:

| Método                                        | Descrição       |
| --------------------------------------------- | --------------- |
| `findByIdUsuarioOrderByDataHoraDesc(Long id)` | Logs do usuário |

---

### 6. EnderecoRepository

**Localização**:
`src/main/java/umc/exs/model/daos/repository/EnderecoRepository.java`

**Métodos**: Herda métodos padrão do JpaRepository.

---

### 7. CartaoRepository

**Localização**:
`src/main/java/umc/exs/model/daos/repository/CartaoRepository.java`

**Métodos principais**:

| Método                        | Descrição                  |
| ----------------------------- | -------------------------- |
| `findByNumero(String numero)` | Busca por número de cartão |

---

## DTOs

**O que são**: Data Transfer Objects - objetos para transferência de dados entre
camadas.

### 1. SignupDTO

**Localização**: `src/main/java/umc/exs/model/dtos/auth/SignupDTO.java`

**Uso**: Cadastro de novos clientes.

**Campos**:

| Campo           | Validação                           |
| --------------- | ----------------------------------- |
| cpf             | Obrigatório, formato 000.000.000-00 |
| email           | Obrigatório, email válido           |
| senha           | Obrigatório, mínimo 8 caracteres    |
| nome            | Obrigatório                         |
| datanasc        | Obrigatório                         |
| gen             | Obrigatório                         |
| termsAccepted   | @AssertTrue                         |
| privacyAccepted | @AssertTrue                         |

---

### 2. LoginDTO

**Localização**: `src/main/java/umc/exs/model/dtos/auth/LoginDTO.java`

**Uso**: Autenticação de clientes.

**Campos**: `email`, `senha`

---

### 3. ClienteDTO

**Localização**: `src/main/java/umc/exs/model/dtos/user/ClienteDTO.java`

**Uso**: Resposta de dados do cliente.

**Campos**: `id`, `nome`, `email`, `datanasc`, `gen`, `senha`, `cpf`,
`saldoTokens`, `enderecos`, `cartoes`

---

### 4. EnderecoDTO

**Localização**: `src/main/java/umc/exs/model/dtos/user/EnderecoDTO.java`

**Uso**: Dados de endereço.

---

### 5. CartaoDTO

**Localização**: `src/main/java/umc/exs/model/dtos/user/CartaoDTO.java`

**Uso**: Dados de cartão.

---

### 6. LivroRequestDTO

**Localização**: `src/main/java/umc/exs/model/dtos/LivroRequestDTO.java`

**Uso**: Dados para criar anúncio de livro.

**Campos**: `titulo`, `autor`, `isbn`

---

### 7. CompraTokensRequestDTO

**Localização**:
`src/main/java/umc/exs/model/dtos/compra/CompraTokensRequestDTO.java`

**Uso**: Compra de tokens.

**Campos**:

| Campo           | Descrição                   |
| --------------- | --------------------------- |
| valor           | Valor da compra             |
| metodoPagamento | PIX ou CARTAO               |
| numeroCartao    | Número do cartão            |
| pixCopiaECola   | PIX Copia e Cola (resposta) |
| qrCodeBase64    | QR Code PIX (resposta)      |
| pagamentoId     | ID do pagamento PIX         |

---

### 8. AvaliacaoLivroDTO

**Localização**: `src/main/java/umc/exs/model/dtos/AvaliacaoLivroDTO.java`

**Uso**: Criar/listar avaliações.

**Campos**: `isbn`, `tituloLivro`, `nota`, `comentario`

---

### 9. AdminAprovacaoDTO

**Localização**: `src/main/java/umc/exs/model/dtos/AdminAprovacaoDTO.java`

**Uso**: Aprovação de livros pelo admin.

**Campos**: `precoAprovado`, `estadoAprovado`, `comentario`

---

### 10. ResponseDTO

**Localização**: `src/main/java/umc/exs/model/dtos/auth/ResponseDTO.java`

**Uso**: Respostas genéricas da API.

---

## Mappers (MapStruct)

**O que são**: Interfaces que geram código para conversão entre Entidades e
DTOs.

### 1. ClienteMapper

**Localização**: `src/main/java/umc/exs/model/daos/mappers/ClienteMapper.java`

**Métodos**:

| Método                                                    | Descrição                        |
| --------------------------------------------------------- | -------------------------------- |
| `toEntity(SignupDTO)`                                     | Converte SignupDTO para Cliente  |
| `toDTO(Cliente)`                                          | Converte Cliente para ClienteDTO |
| `updateEntityFromDto(ClienteDTO, @MappingTarget Cliente)` | Atualiza entidade existente      |

---

### 2. EnderecoMapper

**Localização**: `src/main/java/umc/exs/model/daos/mappers/EnderecoMapper.java`

**Métodos**: `toEntity()`, `toDTO()`, `updateEntityFromDto()`

---

### 3. CartaoMapper

**Localização**: `src/main/java/umc/exs/model/daos/mappers/CartaoMapper.java`

**Métodos**: `toEntity()`, `toDTO()`

---

### 4. LivroMapper

**Localização**: `src/main/java/umc/exs/model/daos/mappers/LivroMapper.java`

**Métodos**: Conversão entre LivroAnuncio e DTOs

---

## Diagrama de Relacionamentos

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           CLIENTE                                        │
│  (id, nome, email, senha, cpf, datanasc, gen, saldoTokens)            │
└─────────────────────────────────────────────────────────────────────────┘
                    │                              │
                    │ 1:N                          │ 1:N
                    ▼                              ▼
┌──────────────────────────┐     ┌──────────────────────────────────────┐
│        ENDERECO          │     │              CARTAO                  │
│ (id, cep, rua, bairro,   │     │ (id, numero, bandeira, nomeTitular,  │
│  cidade, estado, etc.)   │     │  validade, cpfTitular)               │
└──────────────────────────┘     └──────────────────────────────────────┘


┌─────────────────────────────────────────────────────────────────────────┐
│                          LIVROANUNCIO                                   │
│  (id, titulo, autor, isbn, fotoUrl, precoAprovado, estadoAprovado,   │
│   aprovado, dataAnuncio, dataAprovacao)                                │
└─────────────────────────────────────────────────────────────────────────┘
                    │
                    │ N:1
                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                            CLIENTE                                      │
│                         (Vendedor)                                      │
└─────────────────────────────────────────────────────────────────────────┘


┌─────────────────────────────────────────────────────────────────────────┐
│                          TRANSACAO                                     │
│  (id, valor, dataHora, metodoPagamento, status, pagamentoId)         │
└─────────────────────────────────────────────────────────────────────────┘
                    │
                    │ N:1
                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                            CLIENTE                                      │
└─────────────────────────────────────────────────────────────────────────┘


┌─────────────────────────────────────────────────────────────────────────┐
│                        AVALIACAOLIVRO                                   │
│  (id, isbn, tituloLivro, nota, comentario, dataAvaliacao)            │
└─────────────────────────────────────────────────────────────────────────┘
                    │
                    │ N:1
                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                            CLIENTE                                      │
│                         (Avaliador)                                     │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Padrões Utilizados

### 1. DAO Pattern (Repository)

Repositórios abstraem o acesso ao banco de dados.

### 2. DTO Pattern

DTOs separam a representação interna (entidades) da representação externa (API).

### 3. Mapper Pattern

Mapeadores convertem entre entidades e DTOs automaticamente.

### 4. Cascade Types

- `CascadeType.PERSIST`: Salva automaticamente entidades relacionadas
- `CascadeType.MERGE`: Atualiza automaticamente entidades relacionadas

### 5. Fetch Types

- `FetchType.EAGER`: Carrega dados imediatamente
- `FetchType.LAZY`: Carrega dados sob demanda (preferível para performance)
