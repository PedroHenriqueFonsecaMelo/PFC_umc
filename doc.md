
# Documentação das Entidades e Serviços do Sistema UMC

Este documento reúne todas as principais **entidades, classes e serviços** do sistema UMC, incluindo suas estruturas, relacionamentos, endpoints e métodos.

---

## 🧩 Entidades Principais

### Cliente

#### Visão Geral

A classe `Cliente` representa um usuário do sistema responsável por realizar compras, gerenciar dados pessoais, cartões e endereços.

#### Estrutura da Entidade

| Campo        | Tipo           | Restrições                                   | Descrição                                   |
|--------------|----------------|---------------------------------------------|---------------------------------------------|
| `id`         | `Long`         | `@Id`, `@GeneratedValue`                    | Identificador único do cliente.             |
| `senha`      | `String`       | `@Column(nullable = false)`                 | Senha de acesso.                            |
| `nome`       | `String`       | `@Column(nullable = false)`                 | Nome completo.                              |
| `datanasc`   | `String`       | `@Column(nullable = false)`                 | Data de nascimento.                         |
| `gen`        | `String`       | `@Column(nullable = false)`                 | Gênero do cliente.                          |
| `email`      | `String`       | `@Column(nullable = false, unique = true)`  | E-mail único usado para login.              |
| `tentativas` | `int`          | `@Column(nullable = false)`                 | Contador de tentativas de login.            |
| `bloqueada`  | `boolean`      | `@Column(nullable = false)`                 | Indica se a conta está bloqueada.           |
| `cartoes`    | `Set<Cartao>`  | `@ManyToMany`                               | Cartões cadastrados pelo cliente.           |
| `enderecos`  | `Set<Endereco>`| `@ManyToMany`                               | Endereços cadastrados pelo cliente.         |

#### Métodos Importantes

```java
public void setFalhas() {
    if (this.tentativas >= 5) {
        this.bloqueada = true;
    } else {
        this.tentativas++;
    }
}
````

---

### Cartao

Representa um cartão de crédito ou débito associado a um cliente.

#### Estrutura

| Campo          | Tipo           | Descrição                         |
| -------------- | -------------- | --------------------------------- |
| `id`           | `Long`         | Identificador único.              |
| `numero`       | `String`       | Número do cartão.                 |
| `bandeira`     | `String`       | Bandeira (ex: VISA, MasterCard).  |
| `nomeTitular`  | `String`       | Nome impresso no cartão.          |
| `validade`     | `String`       | Validade (MM/AA).                 |
| `cvv`          | `String`       | Código de segurança.              |
| `preferencial` | `boolean`      | Indica se é o cartão padrão.      |
| `clientes`     | `Set<Cliente>` | Clientes associados (ManyToMany). |

---

### Administrador

| Campo      | Tipo     | Descrição              |
| ---------- | -------- | ---------------------- |
| `id`       | `Long`   | Identificador único.   |
| `nome`     | `String` | Nome do administrador. |
| `email`    | `String` | E-mail de login.       |
| `password` | `String` | Senha (criptografada). |

---

### Produto

| Campo          | Tipo     | Descrição                  |
| -------------- | -------- | -------------------------- |
| `id`           | `Long`   | Identificador único.       |
| `titulo`       | `String` | Nome ou título do produto. |
| `precificacao` | `float`  | Valor do produto.          |

---

### Carrinho

Classe que representa o estado atual da compra antes da finalização.

| Campo        | Tipo                 | Descrição               |
| ------------ | -------------------- | ----------------------- |
| `itens`      | `List<ItemCarrinho>` | Produtos e quantidades. |
| `pagamentos` | `List<PagamentoDTO>` | Métodos de pagamento.   |
| `cupom`      | `Cupom`              | Cupom de desconto.      |
| `endereco`   | `Endereco`           | Endereço de entrega.    |
| `frete`      | `float`              | Valor do frete.         |
| `total`      | `float`              | Valor total da compra.  |

---

### Cupom

| Campo       | Tipo            | Descrição                 |
| ----------- | --------------- | ------------------------- |
| `id`        | `Long`          | Identificador único.      |
| `codigo`    | `String`        | Código único do cupom.    |
| `valor`     | `Float`         | Valor do desconto.        |
| `clienteId` | `Long`          | Cliente associado.        |
| `expiracao` | `LocalDateTime` | Data e hora de expiração. |
| `usado`     | `boolean`       | Indica se já foi usado.   |

---

### Pedido

| Campo        | Tipo               | Descrição              |
| ------------ | ------------------ | ---------------------- |
| `id`         | `Long`             | Identificador único.   |
| `clienteId`  | `Long`             | ID do cliente.         |
| `total`      | `Double`           | Valor total.           |
| `data`       | `LocalDateTime`    | Data e hora do pedido. |
| `status`     | `String`           | Status do pedido.      |
| `enderecoId` | `Long`             | Endereço de entrega.   |
| `itens`      | `List<PedidoItem>` | Itens do pedido.       |

---

### PedidoItem

| Campo           | Tipo      | Descrição            |
| --------------- | --------- | -------------------- |
| `id`            | `Long`    | Identificador único. |
| `produtoId`     | `Long`    | ID do produto.       |
| `produtoTitulo` | `String`  | Nome do produto.     |
| `quantidade`    | `Integer` | Quantidade.          |
| `precoUnitario` | `Double`  | Preço unitário.      |
| `pedido`        | `Pedido`  | Pedido pai.          |

---

### Troca

| Campo            | Tipo            | Descrição                          |
| ---------------- | --------------- | ---------------------------------- |
| `id`             | `Long`          | Identificador único.               |
| `pedidoId`       | `Long`          | ID do pedido.                      |
| `clienteId`      | `Long`          | ID do cliente.                     |
| `valor`          | `Double`        | Valor associado.                   |
| `status`         | `String`        | `PENDING`, `APPROVED`, `REJECTED`. |
| `motivoRejeicao` | `String`        | Motivo da rejeição.                |
| `decisaoPor`     | `String`        | Quem decidiu.                      |
| `decisionAt`     | `LocalDateTime` | Data da decisão.                   |
| `createdAt`      | `LocalDateTime` | Data de criação.                   |

---

## ⚙️ Serviços

### AuthService

Serviço responsável por autenticação e gerenciamento de login.

#### Métodos

* **cadastro(String nome, String senha)**
  Cria um novo cliente, valida duplicidade e retorna `true` se bem-sucedido.

* **login(String nome, String senha)**
  Realiza login, valida tentativas e bloqueio.

#### Dependências

* `ClienteRepository`
* `SecurityLogger`
* `BCryptPasswordEncoder`

---

### CarrinhoService

Gerencia operações do carrinho de compras.

#### Principais Métodos

| Método              | Descrição                           |
| ------------------- | ----------------------------------- |
| `addProduto()`      | Adiciona produto ao carrinho.       |
| `removeProduto()`   | Remove produto.                     |
| `aplicarCupom()`    | Aplica cupom de desconto.           |
| `setEndereco()`     | Define endereço de entrega.         |
| `setPagamentos()`   | Define os pagamentos.               |
| `finalizarCompra()` | Finaliza o pedido e salva no banco. |
| `getCarrinho()`     | Retorna o estado atual do carrinho. |

* `ProdutoRepository`
* `CupomRepository`
* `EnderecoRepository`
* `PedidoRepository`

---

### ClientService

Gerencia CRUD de Cliente, Endereço e Cartão.

* `salvarCliente()`
* `buscarClientePorId()`
* `deletarCliente()`

#### Endereço

* `salvarEndereco()`
* `buscarEnderecosPorCliente()`

#### Cartão

* `salvarCartao()`

* `ClienteRepository`
* `EnderecoRepository`
* `CartaoRepository`

---

### SalesService

Serviço responsável por calcular estatísticas de vendas.

#### Método Principal

**computeSalesStats(String since, String until)**
Calcula estatísticas no período especificado.

Retorna:

```json
{
  "totalOrders": 45,
  "totalRevenue": 9834.50,
  "productsSold": [
    {
      "productId": 1,
      "titulo": "Mouse Gamer RGB",
      "quantity": 12,
      "revenue": 2398.8
    }
  ]
}
```

* `PedidoRepository`
* `PedidoItemRepository`

---

## 📦 Controllers

### AdminController

Base URL: `/admin`

| Método | URL            | Descrição                 |
| ------ | -------------- | ------------------------- |
| POST   | `/admins`      | Criar novo administrador. |
| GET    | `/admins`      | Listar administradores.   |
| PUT    | `/admins/{id}` | Atualizar administrador.  |
| DELETE | `/admins/{id}` | Deletar administrador.    |
| GET    | `/sales/stats` | Estatísticas de vendas.   |

---

### CartController

Base URL: `/cart`

| Método | URL          | Descrição                 |
| ------ | ------------ | ------------------------- |
| POST   | `/add`       | Adicionar produto.        |
| POST   | `/remove`    | Remover produto.          |
| POST   | `/cupom`     | Aplicar cupom.            |
| POST   | `/endereco`  | Definir endereço.         |
| POST   | `/pagamento` | Definir pagamento.        |
| POST   | `/finalizar` | Finalizar compra.         |
| GET    | `/`          | Consultar carrinho atual. |

---

### ClientController

Base URL: `/clientes`

#### Endpoints

| Método | URL              | Descrição          |
| ------ | ---------------- | ------------------ |
| POST   | `/clientes`      | Criar cliente.     |
| GET    | `/clientes/{id}` | Buscar por ID.     |
| PUT    | `/clientes/{id}` | Atualizar cliente. |
| DELETE | `/clientes/{id}` | Deletar cliente.   |

#### Endereços

| Método | URL                        | Descrição           |
| ------ | -------------------------- | ------------------- |
| POST   | `/clientes/{id}/endereco`  | Adicionar endereço. |
| GET    | `/clientes/{id}/enderecos` | Listar endereços.   |

#### Cartões

| Método | URL                      | Descrição         |
| ------ | ------------------------ | ----------------- |
| POST   | `/clientes/{id}/cartao`  | Adicionar cartão. |
| GET    | `/clientes/{id}/cartoes` | Listar cartões.   |

---

✅ **Arquivo compatível com MarkdownLint**

* Apenas **um H1**
* Sem duplicações de cabeçalhos
* Estrutura hierárquica clara (`##`, `###`, `####`)
* Code blocks com linguagem especificada
* Pode ser salvo diretamente como:
  `documentacao-umc.md`
