# 📚 Documentação da API REST - Controllers e Serviços

---

## **AdminController**

**Base URL:** `/admin`

### Endpoints Admin CRUD

| Método | URL            | Descrição                       | Request Body | Response                  |
| ------ | -------------- | ------------------------------- | ------------ | ------------------------- |
| POST   | `/admins`      | Criar novo administrador        | `AdminDTO`   | Admin criado (`AdminDTO`) |
| GET    | `/admins`      | Listar todos os administradores | -            | Lista de `AdminDTO`       |
| GET    | `/admins/{id}` | Buscar administrador por ID     | -            | `AdminDTO` ou 404         |
| PUT    | `/admins/{id}` | Atualizar administrador         | `AdminDTO`   | Admin atualizado ou 404   |
| DELETE | `/admins/{id}` | Deletar administrador           | -            | 204 No Content ou 404     |

---

### Endpoints Produtos

| Método | URL              | Descrição              | Request Body | Response              |
| ------ | ---------------- | ---------------------- | ------------ | --------------------- |
| POST   | `/products`      | Criar produto          | `ProdutoDTO` | Produto criado        |
| DELETE | `/products/{id}` | Deletar produto por ID | -            | 204 No Content ou 404 |

---

### Endpoints Trocas (Exchanges)

| Método | URL                       | Descrição      | Request Body / Params            | Response                      |
| ------ | ------------------------- | -------------- | -------------------------------- | ----------------------------- |
| POST   | `/exchanges/{id}/approve` | Aprovar troca  | -                                | JSON com troca e cupom        |
| POST   | `/exchanges/{id}/reject`  | Rejeitar troca | `motivo` (query param, opcional) | Troca atualizada (`TrocaDTO`) |

---

### Estatísticas de Vendas

| Método | URL            | Descrição              | Query Params                        | Response              |
| ------ | -------------- | ---------------------- | ----------------------------------- | --------------------- |
| GET    | `/sales/stats` | Estatísticas de vendas | `since` e `until` (datas opcionais) | Mapa com estatísticas |

---

## **CartController**

**Base URL:** `/cart`

| Método | URL          | Descrição                     | Request Body / Params     | Response                    |
| ------ | ------------ | ----------------------------- | ------------------------- | --------------------------- |
| POST   | `/add`       | Adicionar produto ao carrinho | `produtoId`, `quantidade` | Objeto `Carrinho`           |
| POST   | `/remove`    | Remover produto do carrinho   | `produtoId`               | Objeto `Carrinho`           |
| POST   | `/cupom`     | Aplicar cupom ao carrinho     | `codigo`                  | Objeto `Carrinho`           |
| POST   | `/endereco`  | Definir endereço de entrega   | `enderecoId`              | Objeto `Carrinho`           |
| POST   | `/pagamento` | Definir pagamentos            | Lista de `PagamentoDTO`   | Objeto `Carrinho` ou erro   |
| POST   | `/finalizar` | Finalizar compra              | `clienteId`               | Mensagem de sucesso ou erro |
| GET    | `/`          | Consultar carrinho atual      | -                         | Objeto `Carrinho`           |

---

## **ClientController**

**Base URL:** `/clientes`

### CRUD Cliente

| Método | URL              | Descrição             | Request Body | Response                  |
| ------ | ---------------- | --------------------- | ------------ | ------------------------- |
| POST   | `/clientes`      | Criar cliente         | `Cliente`    | Cliente criado (201)      |
| GET    | `/clientes/{id}` | Buscar cliente por ID | -            | Cliente ou 404            |
| PUT    | `/clientes/{id}` | Atualizar cliente     | `Cliente`    | Cliente atualizado ou 404 |
| DELETE | `/clientes/{id}` | Deletar cliente       | -            | 204 No Content ou 404     |

---

### Endereços

| Método | URL                               | Descrição          | Request Body | Response                  |
| ------ | --------------------------------- | ------------------ | ------------ | ------------------------- |
| POST   | `/clientes/{clienteId}/endereco`  | Adicionar endereço | `Endereco`   | Endereço criado (201)     |
| GET    | `/clientes/{clienteId}/enderecos` | Listar endereços   | -            | Lista de endereços ou 404 |

---

### Cartões

| Método | URL                             | Descrição        | Request Body | Response                |
| ------ | ------------------------------- | ---------------- | ------------ | ----------------------- |
| POST   | `/clientes/{clienteId}/cartao`  | Adicionar cartão | `Cartao`     | Cartão criado (201)     |
| GET    | `/clientes/{clienteId}/cartoes` | Listar cartões   | -            | Lista de cartões ou 404 |

---

## **AuthService**

Serviço para cadastro e login de usuários (Clientes).

### Métodos

* **cadastro(nome, senha) : boolean**
  Registra usuário novo. Retorna false se nome já existe. Senha salva com hash BCrypt.

* **login(nome, senha) : String**
  Autentica usuário, verifica bloqueios e falhas. Retorna mensagens informativas sobre o status da tentativa.

---

## **CarrinhoService**

Gerencia o carrinho de compras, incluindo produtos, cupom, endereço, pagamentos e finalização.

### Funcionalidades

* `addProduto(produtoId, quantidade) : Carrinho`
* `removeProduto(produtoId) : Carrinho`
* `aplicarCupom(codigo) : Carrinho`
* `setEndereco(enderecoId) : Carrinho`
* `setPagamentos(List<PagamentoDTO>) : Carrinho`
* `finalizarCompra(Cliente) : boolean`
* `getCarrinho() : Carrinho`

---

## **ClientService**

Gerencia CRUD de Cliente, Endereço e Cartão.

### Cliente

* `salvarCliente(Cliente) : Cliente`
* `buscarClientePorId(Long) : Optional<Cliente>`
* `deletarCliente(Long) : void`

### Endereço

* `salvarEndereco(clienteId, Endereco) : Endereco`
* `buscarEnderecosPorCliente(clienteId) : List<Endereco>`

### Cartão

* `salvarCartao(clienteId, Cartao) : Cartao`

---

## **SalesService**

Calcula estatísticas de vendas por período.

### Método Principal

* `computeSalesStats(String since, String until) : Map<String,Object>`

  * Recebe datas no formato ISO_LOCAL_DATE_TIME.
  * Busca pedidos no período ou todos se não informado.
  * Calcula:

    * Total de pedidos (`totalOrders`).
    * Receita total (`totalRevenue`): maior entre soma dos pedidos ou soma dos itens.
    * Produtos vendidos (`productsSold`) com ID, título, quantidade e receita.

---

## Dependências dos Serviços

* `PedidoRepository`, `PedidoItemRepository`
* `ClienteRepository`, `EnderecoRepository`, `CartaoRepository`
* `ProdutoRepository`, `CupomRepository`
* `SecurityLogger`, `BCrypt`
