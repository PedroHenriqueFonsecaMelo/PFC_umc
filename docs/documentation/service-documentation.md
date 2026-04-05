# 📂 Documentação de Services

## O que é um Service?

**Services** são a camada de lógica de negócio em aplicações Spring. Enquanto os
**Controllers** lidam com requisições HTTP, os **Services** contêm a lógica de
negócio real, coordinando operações e regras da aplicação.

### Responsabilidades do Service

1. **Lógica de Negócio**: Implementar as regras e validações do domínio
2. **Transações**: Gerenciar operações de banco de dados (commit/rollback)
3. **Coordenação**: Coordenar múltiplas operações e entidades
4. **Abstração**: Isolar a camada de acesso a dados (Repositories) dos
   Controllers

### Anotações Comuns

- `@Service`: Marca a classe como componente de serviço
- `@Transactional`: Define que métodos devem executar em transação
- `@RequiredArgsConstructor` (Lombok): Injeção de dependências via construtor

---

## Services do Projeto

### 1. ClienteService

**Localização**: `src/main/java/umc/exs/service/ClienteService.java`

**O que faz**: Gerencia todas as operações relacionadas a clientes, incluindo:

#### Funcionalidades Principais

| Método                           | Descrição                                    |
| -------------------------------- | -------------------------------------------- |
| `salvarCliente()`                | Cadastra novo cliente básico                 |
| `salvarClienteCompleto()`        | Cadastra cliente com endereço e cartão       |
| `atualizarClienteEAssociacoes()` | Atualiza dados do cliente e suas associações |
| `deletarClientePorId()`          | Remove conta do cliente                      |
| `adicionarTokens()`              | Adiciona tokens à carteira                   |
| `listarHistoricoTransacoes()`    | Lista histórico de compras                   |
| `autenticarCliente()`            | Valida credenciais                           |
| `buscarClientePorEmail()`        | Busca cliente por e-mail                     |
| `iniciarRecuperacaoSenha()`      | Gera token para recuperação                  |
| `validarTokenRecuperacao()`      | Valida token de recuperação                  |
| `alterarSenhaComToken()`         | Altera senha com token                       |
| `registrarTransacaoPendente()`   | Registra transação PIX pendente              |
| `verificarSeFoiPago()`           | Verifica status do pagamento                 |
| `aprovarPagamento()`             | Aprova pagamento PIX                         |

#### Dependências

- `ClienteRepository` - Acesso a dados de clientes
- `CartaoRepository` - Acesso a dados de cartões
- `EnderecoRepository` - Acesso a dados de endereços
- `TransacaoRepository` - Acesso a transações
- `RecuperacaoSenhaRepository` - Tokens de recuperação
- `EmailService` - Envio de e-mails
- `PasswordEncoder` - Criptografia de senhas
- `EnderecoService` - Lógica de endereços
- `CartaoService` - Lógica de cartões
- `ClienteMapper` - Mapeamento DTO/Entidade

---

### 2. LivroService

**Localização**: `src/main/java/umc/exs/service/LivroService.java`

**O que faz**: Gerencia todas as operações com livros/anúncios.

#### Funcionalidades Principais

| Método                    | Descrição                             |
| ------------------------- | ------------------------------------- |
| `cadastrarVenda()`        | Cria novo anúncio de livro            |
| `listarLivrosAprovados()` | Lista livros para vitrine             |
| `listarLivrosPendentes()` | Lista livros aguardando aprovação     |
| `listarTodosLivros()`     | Lista todos os livros (admin)         |
| `aprovarLivro()`          | Aprova livro definindo preço e estado |
| `rejeitarLivro()`         | Rejeita e remove livro                |
| `realizarCompra()`        | Executa compra de livro               |

#### Características Especiais

- **Recompensa em Tokens**: Ao cadastrar um livro, o usuário ganha 10 tokens
- **Aprovação Admin**: Livros são criados com `aprovado = false`
- **Preço Admin**: O admin define o preço na aprovação
- **Validação de Preço**: Impede preços abusivos para livros desgasteados
- **Transferência de Tokens**: Compra transfere tokens de comprador para
  vendedor

#### Dependências

- `LivroRepository` - Acesso a dados de livros
- `ClienteRepository` - Acesso a dados de clientes
- `LogAuditoriaService` - Auditoria de ações

---

### 3. AuthHelper

**Localização**: `src/main/java/umc/exs/service/AuthHelper.java`

**O que faz**: Auxilia na autenticação de usuários após operações como cadastro
ou login.

#### Funcionalidades

| Método                       | Descrição                             |
| ---------------------------- | ------------------------------------- |
| `authenticateAndSetCookie()` | Autentica usuário e define cookie JWT |

#### Características

- Gera token JWT
- Define cookie HTTP-only
- Configura autenticação no Spring Security Context
- Registra auditoria da ação

#### Dependências

- `JwtUserDetailsService` - Carregamento de usuários
- `JwtUtil` - Geração de JWT
- `LogAuditoriaService` - Auditoria

---

### 4. AvaliacaoLivroService

**Localização**: `src/main/java/umc/exs/service/AvaliacaoLivroService.java`

**O que faz**: Gerencia avaliações de livros pelos usuários.

#### Funcionalidades Principais

| Método                        | Descrição                    |
| ----------------------------- | ---------------------------- |
| `criarAvaliacao()`            | Cria nova avaliação de livro |
| `buscarAvaliacoesPorIsbn()`   | Lista avaliações por ISBN    |
| `calcularMediaPorIsbn()`      | Calcula média de notas       |
| `buscarLivrosComAvaliacoes()` | Lista livros avaliados       |

#### Validações

- Nota entre 1 e 5
- ISBN obrigatório
- Usuário não pode avaliar o mesmo livro duas vezes
- Título do livro obrigatório

#### Dependências

- `AvaliacaoLivroRepository` - Acesso a dados de avaliações
- `ClienteRepository` - Acesso a dados de clientes
- `LogAuditoriaService` - Auditoria

---

### 5. CartaoService

**Localização**: `src/main/java/umc/exs/service/CartaoService.java`

**O que faz**: Gerencia cartões de crédito dos clientes.

#### Funcionalidades

| Método                | Descrição                                |
| --------------------- | ---------------------------------------- |
| `saveOrReuseCartao()` | Salva novo cartão ou reutiliza existente |

#### Lógica

- Verifica se cartão já existe pelo número
- Se existir, reutiliza; se não, cria novo
- Associa cliente ao cartão

---

### 6. EnderecoService

**Localização**: `src/main/java/umc/exs/service/EnderecoService.java`

**O que faz**: Gerencia endereços dos clientes.

#### Funcionalidades

| Método                  | Descrição                                  |
| ----------------------- | ------------------------------------------ |
| `saveOrReuseEndereco()` | Salva novo endereço ou reutiliza existente |

#### Lógica

- Verifica se endereço já existe pelos campos únicos
- Se existir, reutiliza; se não, cria novo
- Associa cliente ao endereço

---

### 7. PagamentoMockService

**Localização**: `src/main/java/umc/exs/service/PagamentoMockService.java`

**O que faz**: Simula operações de pagamento (para desenvolvimento/testes).

---

### 8. LogAuditoriaService

**Localização**: `src/main/java/umc/exs/log/LogAuditoriaService.java`

**O que faz**: Registra ações de usuários para auditoria.

#### Funcionalidades

| Método                  | Descrição                |
| ----------------------- | ------------------------ |
| `registrarLog()`        | Registra ação do usuário |
| `buscarLogsDoCliente()` | Lista logs de um cliente |

#### Ações Registradas

- `LOGIN_SUCESSO`, `LOGIN_FALHA`
- `LOGOUT_SUCESSO`
- `CADASTRO_SUCESSO`
- `LIVRO_CADASTRADO_RECOMPENSA`
- `LIVRO_APROVADO`, `LIVRO_REJEITADO`
- `COMPRA_LIVRO_SUCESSO`
- `AVALIACAO_CRIADA`
- `COMPRA_SUCESSO`, `COMPRA_FALHA`

---

## Services de E-mail

### 9. EmailService

**Localização**: `src/main/java/umc/exs/service/email/EmailService.java`

**O que faz**: Interface para envio de e-mails.

### 10. SmtpEmailSender

**Localização**: `src/main/java/umc/exs/service/email/SmtpEmailSender.java`

**O que faz**: Implementação real de envio via SMTP.

### 11. NoopEmailSender

**Localização**: `src/main/java/umc/exs/service/email/NoopEmailSender.java`

**O que faz**: Implementação vazia (para desenvolvimento).

---

## Resumo dos Services

| Service               | Responsabilidade                                               |
| --------------------- | -------------------------------------------------------------- |
| ClienteService        | Gestão de clientes, autenticação, tokens, recuperação de senha |
| LivroService          | Cadastro, aprovação, compra de livros                          |
| AuthHelper            | Auxiliar de autenticação com JWT                               |
| AvaliacaoLivroService | Avaliações e notas de livros                                   |
| CartaoService         | Gestão de cartões                                              |
| EnderecoService       | Gestão de endereços                                            |
| LogAuditoriaService   | Auditoria de ações                                             |
| PagamentoMockService  | Simulação de pagamentos                                        |
| EmailService          | Envio de e-mails                                               |

---

## Padrão de Serviço

O padrão típico de um método de serviço:

```java
@Service
@RequiredArgsConstructor
public class ExemploService {

    private final ExemploRepository repository;
    private final OutroService outroService;

    @Transactional
    public EntityDTO salvar(EntityDTO dto) {
        // 1. Validar entrada
        validar(dto);

        // 2. Converter DTO para Entidade
        Entity entity = mapper.toEntity(dto);

        // 3. Executar lógica de negócio
        entity.setDataCriacao(LocalDateTime.now());

        // 4. Salvar no banco
        Entity salvo = repository.save(entity);

        // 5. Retornar DTO
        return mapper.toDTO(salvo);
    }

    @Transactional(readOnly = true)
    public List<EntityDTO> listar() {
        return repository.findAll().stream()
            .map(mapper::toDTO)
            .collect(Collectors.toList());
    }
}
```
