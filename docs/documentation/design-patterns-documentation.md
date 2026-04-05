# 📂 Documentação de Design Patterns

## Visão Geral

Este documento explica os padrões de projeto (Design Patterns) utilizados no
projeto, incluindo Strategy Pattern e Factory Pattern para gestão de pagamentos.

---

## O que são Design Patterns?

**Design Patterns** são soluções recorrentes para problemas comuns em
desenvolvimento de software. Eles representam boas práticas desenvolvidas e
testadas ao longo do tempo.

---

## 1. Strategy Pattern (Padrão Estratégia)

### O que é?

O **Strategy Pattern** define uma família de algoritmos, encapsula cada um e os
torna intercambiáveis. Permite que o algoritmo varie independentemente dos
clientes que o utilizam.

### Quando usar?

- Quando você tem múltiplas formas de executar uma operação
- Quando precisa trocar algoritmos em tempo de execução
- Quando quer evitar condicionais complexas (if/else ou switch)

### Implementação no Projeto

#### Interface Strategy

**Localização**: `src/main/java/umc/exs/design/strategy/PagamentoStrategy.java`

```java
public interface PagamentoStrategy {
    
    boolean processar(double valor, CompraTokensRequestDTO dados);
    
    String getTipoPagamento();
}
```

#### Estratégias Concretas

**1. PagamentoPixStrategy**

**Localização**:
`src/main/java/umc/exs/design/strategy/impl/PagamentoPixStrategy.java`

```java
@Slf4j
@Component
public class PagamentoPixStrategy implements PagamentoStrategy {

    @Override
    public boolean processar(double valor, CompraTokensRequestDTO dados) {
        try {
            // Gera ID único para transação
            String idTransacao = "PX-" + System.currentTimeMillis();
            
            // Gera payload PIX fake
            String payloadPix = "00020126580014br.gov.bcb.pix0136" + idTransacao;
            
            // Gera QR Code
            String qrCodeUrl = "https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=" + payloadPix;

            // Preenche dados de resposta
            dados.setPixCopiaECola(payloadPix);
            dados.setQrCodeBase64(qrCodeUrl);
            dados.setPagamentoId(idTransacao);

            log.info("PIX Simulado gerado para o valor: R$ {}", valor);
            return true; 
        } catch (Exception e) {
            log.error("Erro ao gerar simulação de PIX: {}", e.getMessage());
            return false;
        } 
    }

    @Override
    public String getTipoPagamento() {
        return "PIX";
    }
}
```

**2. PagamentoCartaoStrategy**

**Localização**:
`src/main/java/umc/exs/design/strategy/impl/PagamentoCartaoStrategy.java`

```java
@Slf4j
@Component
public class PagamentoCartaoStrategy implements PagamentoStrategy {

    @Override
    public boolean processar(double valor, CompraTokensRequestDTO dados) {
        log.info("Processando pagamento de R$ {} via CARTÃO: {}", valor, dados.getNumeroCartao());
        // Simulação: cartão válido se não for vazio
        return dados.getNumeroCartao() != null && !dados.getNumeroCartao().isBlank();
    }

    @Override
    public String getTipoPagamento() {
        return "CARTAO";
    }
}
```

#### Uso no Controller

**Localização**: `src/main/java/umc/exs/controller/api/TokenController.java`

```java
@Slf4j
@RestController
@RequestMapping("/api/tokens")
@RequiredArgsConstructor
public class TokenController {

    private final ClienteService clienteService;
    private final PagamentoFactory pagamentoFactory;
    private final LogAuditoriaService logAuditoriaService;

    @PostMapping("/comprar")
    public ResponseEntity<?> comprar(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid CompraTokensRequestDTO request) {

        try {
            // 1. Buscar estratégia de pagamento (Strategy Pattern)
            PagamentoStrategy estrategia = pagamentoFactory.buscarEstrategia(request.getMetodoPagamento());

            // 2. Processar pagamento (qualquer estratégia serve)
            boolean sucesso = estrategia.processar(request.getValor(), request);

            if (!sucesso) {
                return ResponseEntity.badRequest().body("Pagamento recusado.");
            }

            // 3. Tratamento específico por método
            if ("PIX".equalsIgnoreCase(request.getMetodoPagamento())) {
                // Lógica específica para PIX (pendente)
                clienteService.registrarTransacaoPendente(...);
                return ResponseEntity.ok(request);
            } else {
                // Lógica específica para cartão (aprovação imediata)
                clienteService.adicionarTokens(...);
                return ResponseEntity.ok(clienteService.buscarPorId(cliente.getId()));
            }

        } catch (Exception e) {
            log.error("Erro crítico ao processar compra: ", e);
            return ResponseEntity.internalServerError().body("Erro interno.");
        }
    }
}
```

### Benefícios do Strategy Pattern

| Benefício            | Descrição                                        |
| -------------------- | ------------------------------------------------ |
| **Extensibilidade**  | Adicionar novo método de pagamento = nova classe |
| **Simplicidade**     | Elimina condicionais complexas                   |
| **Testabilidade**    | Cada estratégia testada isoladamente             |
| **Manutenibilidade** | Mudanças em um método não afetam outros          |
| **SRP**              | Cada classe tem uma única responsabilidade       |

### Diagrama do Strategy Pattern

```
                    ┌─────────────────────┐
                    │ <<interface>>        │
                    │ PagamentoStrategy    │
                    ├─────────────────────┤
                    │ + processar()       │
                    │ + getTipoPagamento()│
                    └──────────┬──────────┘
                               │
        ┌──────────────────────┼──────────────────────┐
        │                      │                      │
        ▼                      ▼                      ▼
┌───────────────┐    ┌───────────────┐    ┌───────────────┐
│PagamentoPix   │    │PagamentoCartao│    │ FuturePayment │
│Strategy       │    │Strategy       │    │ Strategy       │
├───────────────┤    ├───────────────┤    ├───────────────┤
│+ processar() │    │+ processar()  │    │+ processar()  │
│+ getTipo...()│    │+ getTipo...() │    │+ getTipo...() │
└───────────────┘    └───────────────┘    └───────────────┘
```

---

## 2. Factory Pattern (Padrão Fábrica)

### O que é?

O **Factory Pattern** cria objetos sem especificar a classe exata do objeto que
será criado. Usa uma interface comum para criar objetos.

### Quando usar?

- Quando não quer conhecer a classe concreta para criar objetos
- Quando o sistema precisa ser independente de como seus produtos são criados
- Para gerenciar coleção de objetos similares

### Implementação no Projeto

#### Factory

**Localização**: `src/main/java/umc/exs/design/factory/PagamentoFactory.java`

```java
@Service
public class PagamentoFactory {

    private final Map<String, PagamentoStrategy> estrategias;

    // Construtor: recebe todas as estratégias via injeção de dependência
    public PagamentoFactory(List<PagamentoStrategy> listaEstrategias) {
        // Constrói mapa manualmente (evita streams conforme requisito)
        Map<String, PagamentoStrategy> mapa = new HashMap<>();
        for (PagamentoStrategy estrategia : listaEstrategias) {
            if (estrategia != null && estrategia.getTipoPagamento() != null) {
                mapa.put(estrategia.getTipoPagamento(), estrategia);
            }
        }
        estrategias = mapa;
    }

    public PagamentoStrategy buscarEstrategia(String metodo) {
        return Optional.ofNullable(estrategias.get(metodo.toUpperCase()))
                .orElseThrow(() -> new IllegalArgumentException("Método de pagamento '" + metodo + "' não suportado."));
    }
}
```

### Como o Spring Injeta as Estratégias

O Spring automaticamente detecta todas as classes que implementam
`PagamentoStrategy` e as injeta como uma `List<PagamentoStrategy>` no construtor
da Factory.

```java
// O Spring detecta:
// - PagamentoPixStrategy (@Component)
// - PagamentoCartaoStrategy (@Component)
// E injeta ambas na lista automaticamente!
public PagamentoFactory(List<PagamentoStrategy> listaEstrategias) { ... }
```

### Benefícios do Factory Pattern

| Benefício              | Descrição                                |
| ---------------------- | ---------------------------------------- |
| **Desacoplamento**     | Cliente não depende de classes concretas |
| **Centralização**      | Criação de objetos em um único lugar     |
| **Flexibilidade**      | Adicionar novas estratégias facilmente   |
| **Injeção Automática** | Spring gerencia dependências             |

### Diagrama do Factory Pattern

```
┌─────────────────────────────────────────────────────────────┐
│                    TokenController                          │
│                  (Cliente do Factory)                        │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          │ 1. buscaEstrategia("PIX")
                          ▼
            ┌───────────────────────────┐
            │    PagamentoFactory       │
            │  + buscarEstrategia()     │
            └─────────────┬─────────────┘
                          │
         ┌────────────────┼────────────────┐
         │                │                │
         ▼                ▼                ▼
┌─────────────┐  ┌─────────────┐  ┌─────────────┐
│ PagamentoPix│  │ Pagamento   │  │  ...        │
│ Strategy    │  │ Cartao      │  │             │
└─────────────┘  └─────────────┘  └─────────────┘
```

---

## 3. Outros Patterns Utilizados

### Builder Pattern

**O que é**: Padrão para construção de objetos complexos passo a passo.

**Exemplo no Projeto** (Lombok):

```java
// Usando @Builder do Lombok
LivroAnuncio anuncio = LivroAnuncio.builder()
    .titulo("Dom Quixote")
    .autor("Miguel de Cervantes")
    .fotoUrl("/uploads/livros/foto.jpg")
    .vendedor(vendedor)
    .dataAnuncio(LocalDateTime.now())
    .aprovado(false)
    .build();
```

**Benefícios**:

- Código mais legível
- Parâmetros nomeados
- Valores opcionais

---

### DAO Pattern (Repository)

**O que é**: Padrão que abstrai o acesso a dados.

**Exemplo no Projeto**:

```java
@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    Optional<Cliente> findByEmail(String email);
    boolean existsByEmail(String email);
}
```

---

### DTO Pattern

**O que é**: Objeto para transferência de dados entre camadas.

**Exemplo no Projeto**:

```java
// DTO para API
public class ClienteDTO {
    private Long id;
    private String nome;
    private String email;
    private Double saldoTokens;
    // Sem senha!
}
```

---

### Mapper Pattern

**O que é**: Converte entre objetos de diferentes camadas.

**Exemplo no Projeto** (MapStruct):

```java
@Mapper(componentModel = "spring")
public interface ClienteMapper {
    Cliente toEntity(SignupDTO dto);
    ClienteDTO toDTO(Cliente cliente);
}
```

---

### Singleton Pattern

**O que é**: Garante uma única instância.

**Exemplo no Projeto**:

```java
@Service  // Spring cria uma única instância
public class ClienteService {
    // Uma única instância compartilhada
}
```

---

### MVC Pattern

**O que é**: Separação em Model, View, Controller.

**Exemplo no Projeto**:

```
├── controller/     (Controllers - recebe requisições)
├── service/        (Services - lógica de negócio)
├── model/           (Entities, DTOs, Repositories)
└── resources/
    └── templates/   (Views - Thymeleaf)
```

---

## 4. Resumo dos Patterns

| Pattern       | Onde Usado               | Benefício                  |
| ------------- | ------------------------ | -------------------------- |
| **Strategy**  | Pagamentos (Pix, Cartão) | Algoritmos intercambiáveis |
| **Factory**   | PagamentoFactory         | Criação centralizada       |
| **Builder**   | Lombok @Builder          | Construção flexível        |
| **DAO**       | JpaRepository            | Abstração de dados         |
| **DTO**       | ClienteDTO, SignupDTO    | Transferência de dados     |
| **Mapper**    | MapStruct                | Conversão automática       |
| **Singleton** | Services Spring          | Uma única instância        |
| **MVC**       | Controller/Service/Model | Separação de preocupações  |

---

## 5. Adicionando Novo Método de Pagamento

Para adicionar um novo método de pagamento (ex: Boleto):

1. **Criar a Estratégia**:

```java
@Component
public class PagamentoBoletoStrategy implements PagamentoStrategy {

    @Override
    public boolean processar(double valor, CompraTokensRequestDTO dados) {
        // Lógica específica do boleto
        String codigoBarras = "...";
        dados.setCodigoBarras(codigoBarras);
        return true;
    }

    @Override
    public String getTipoPagamento() {
        return "BOLETO";
    }
}
```

2. **Pronto!** O Spring automaticamente:
   - Detecta a nova classe (`@Component`)
   - Injeta na lista de estratégias
   - A Factory disponibilizará o novo método

3. **Usar no Controller**:

```java
// Tudo continua igual!
PagamentoStrategy estrategia = pagamentoFactory.buscarEstrategia("BOLETO");
estrategia.processar(valor, dados);
```

---

## Conclusão

Os padrões **Strategy** e **Factory** são fundamentais para este projeto porque:

1. **Separam a lógica de pagamento** - Cada método tem sua própria classe
2. **Facilitam manutenção** - Mudanças em um método não afetam outros
3. **Permitem extensão** - Novos métodos são adicionados sem modificar código
   existente
4. **São testáveis** - Cada estratégia pode ser testada isoladamente
5. **Usam injeção de dependência** - Spring gerencia as dependências
   automaticamente

Estes padrões seguem os princípios **SOLID**, especialmente:

- **S**ingle Responsibility Principle (SRP)
- **O**pen/Closed Principle (OCP)
- **D**ependency Inversion Principle (DIP)
