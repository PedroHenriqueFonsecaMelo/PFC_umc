# 📖 Tópicos Avançados

Este documento explica conceitos avançados e complexos utilizados no projeto,
como Expressões Lambda, Streams, Programação Funcional e outros padrões
importantes.

---

## 1. Expressões Lambda

### O que são?

Expressões Lambda são funções anônimas que podem ser passadas como argumento
para métodos ou armazenadas em variáveis. Introduzidas no Java 8, elas permitem
escrever código mais conciso e expressivo.

### Sintaxe Básica

```java
// Sintaxe: (parâmetros) -> {corpo}

// Exemplo 1: Sem parâmetros
() -> System.out.println("Olá!")

// Exemplo 2: Com um parâmetro
nome -> System.out.println("Olá, " + nome)

// Exemplo 3: Com múltiplos parâmetros
(a, b) -> a + b

// Exemplo 4: Com corpo de múltiplas linhas
(a, b) -> {
    int soma = a + b;
    return soma * 2;
}
```

### Exemplos no Projeto

#### Exemplo 1: Comparator com Lambda

```java
// Tradicional (sem lambda)
Collections.sort(lista, new Comparator<Livro>() {
    @Override
    public int compare(Livro l1, Livro l2) {
        return l1.getTitulo().compareTo(l2.getTitulo());
    }
});

// Com Lambda
lista.sort((l1, l2) -> l1.getTitulo().compareTo(l2.getTitulo()));

// Com Method Reference (mais conciso)
lista.sort(Comparator.comparing(Livro::getTitulo));
```

#### Exemplo 2: Runnable com Lambda

```java
// Tradicional
Thread t = new Thread(new Runnable() {
    @Override
    public void run() {
        System.out.println("Executando");
    }
});

// Com Lambda
Thread t = () -> System.out.println("Executando");
```

#### Exemplo 3: Predicate (condições)

```java
// Predicate para filtrar clientes maiores de idade
Predicate<Cliente> maiorDeIdade = cliente -> {
    LocalDate dataNasc = LocalDate.parse(cliente.getDatanasc());
    return Period.between(dataNasc, LocalDate.now()).getYears() >= 18;
};

// Uso
if (maiorDeIdade.test(cliente)) {
    // processar
}
```

---

## 2. Streams API

### O que são?

Streams são sequências de elementos que suportam operações agregadas. Permitem
processar coleções de forma funcional e paralela.

### Operações Principais

#### Pipeline de Stream

```
Fonte (Collection/Array) → Intermediate Operations → Terminal Operation → Resultado
```

#### Intermediate Operations (retornam Stream)

- `filter(Predicate)` - Filtra elementos
- `map(Function)` - Transforma elementos
- `flatMap(Function)` - Achata estruturas
- `sorted(Comparator)` - Ordena
- `distinct()` - Remove duplicados
- `limit(n)` - Limita quantidade
- `skip(n)` - Pula elementos

#### Terminal Operations (retornam resultado)

- `collect()` - Coleta para Collection
- `forEach(Consumer)` - Iera sobre elementos
- `count()` - Conta elementos
- `anyMatch(Predicate)` - Verifica se algum corresponde
- `allMatch(Predicate)` - Verifica se todos correspondem
- `noneMatch(Predicate)` - Verifica se nenhum corresponde
- `findFirst()` / `findAny()` - Encontra elemento
- `reduce()` - Reduz a um valor único

### Exemplos no Projeto

#### Exemplo 1: Filtrar e Coletar

```java
// Encontrar livros aprovados
List<LivroAnuncio> aprovados = livros.stream()
    .filter(livro -> Boolean.TRUE.equals(livro.getAprovado()))
    .collect(Collectors.toList());

// equivalent in traditional loop:
List<LivroAnuncio> aprovados = new ArrayList<>();
for (LivroAnuncio livro : livros) {
    if (Boolean.TRUE.equals(livro.getAprovado())) {
        aprovados.add(livro);
    }
}
```

#### Exemplo 2: Mapear e Transformar

```java
// Obter lista de nomes de clientes
List<String> nomes = clientes.stream()
    .map(Cliente::getNome)
    .collect(Collectors.toList());

// equivalent in traditional loop:
List<String> nomes = new ArrayList<>();
for (Cliente cliente : clientes) {
    nomes.add(cliente.getNome());
}
```

#### Exemplo 3: Encontrar Primeiro Elemento

```java
// Encontrar cliente por email
Optional<Cliente> encontrado = clientes.stream()
    .filter(c -> c.getEmail().equals(email))
    .findFirst();

// equivalent in traditional loop:
Optional<Cliente> encontrado = Optional.empty();
for (Cliente c : clientes) {
    if (c.getEmail().equals(email)) {
        encontrado = Optional.of(c);
        break;
    }
}
```

#### Exemplo 4: Agrupar por Categoria

```java
// Agrupar livros por gênero
Map<String, List<LivroAnuncio>> porGenero = livros.stream()
    .collect(Collectors.groupingBy(LivroAnuncio::getGenero));
```

#### Exemplo 5: Estatísticas

```java
// Calcular média de notas
Double mediaNotas = avaliacoes.stream()
    .mapToInt(AvaliacaoLivro::getNota)
    .average()
    .orElse(0.0);

// equivalent in traditional loop:
int soma = 0;
int quantidade = 0;
for (AvaliacaoLivro avaliacao : avaliacoes) {
    if (avaliacao.getNota() != null) {
        soma += avaliacao.getNota();
        quantidade++;
    }
}
double mediaNotas = quantidade > 0 ? (double) soma / quantidade : 0.0;
```

---

## 3. Method Reference (Referência de Método)

### O que são?

Method References são formas simplificadas de expressões Lambda que chamam um
método diretamente.

### Sintaxe

```java
// Tipos de Method Reference

// 1. Referência a método estático
// Tipo: ClassName::staticMethod
String::valueOf
Math::max

// 2. Referência a método de instância de objeto específico
// Tipo: instance::instanceMethod
str::toUpperCase
System.out::println

// 3. Referência a método de instância de objeto arbitrário
// Tipo: ClassName::instanceMethod
String::compareToIgnoreCase
Cliente::getNome

// 4. Referência a construtor
// Tipo: ClassName::new
ArrayList::new
BigDecimal::new
```

### Exemplos no Projeto

```java
// Em vez de:
clientes.stream()
    .map(cliente -> cliente.getNome())
    .collect(Collectors.toList());

// Você pode usar:
clientes.stream()
    .map(Cliente::getNome)
    .collect(Collectors.toList());

// Em vez de:
lista.sort((a, b) -> a.getNome().compareTo(b.getNome()));

// Você pode usar:
lista.sort(Comparator.comparing(Cliente::getNome));
```

---

## 4. Optional

### O que é?

Optional é um container que pode conter ou não um valor não-nulo. Ajuda a evitar
NullPointerException e escrever código mais limpo.

### Criação

```java
// Criar Optional vazio
Optional<String> vazio = Optional.empty();

// Criar Optional com valor
Optional<String> presente = Optional.of("valor");

// Criar Optional que pode ser nulo
Optional<String> opcional = Optional.ofNullable(valorPossivelmenteNulo);
```

### Operações

```java
// isPresent() - verificar se tem valor
if (opt.isPresent()) {
    String valor = opt.get();
}

// ifPresent() - executar ação se presente
opt.ifPresent(valor -> System.out.println(valor));

// orElse() - valor padrão se ausente
String resultado = opt.orElse("padrão");

// orElseGet() - supplier para valor padrão
String resultado = opt.orElseGet(() -> "calculado");

// orElseThrow() - exceção se ausente
String resultado = opt.orElseThrow(() -> new RuntimeException("Ausente"));

// map() - transformar se presente
Optional<Integer> tamanho = opt.map(String::length);

// filter() - filtrar se presente
Optional<String> filtrado = opt.filter(s -> s.length() > 5);
```

### Exemplos no Projeto

```java
// Encontrar cliente por email
Optional<Cliente> opt = clienteRepository.findByEmail(email);
if (opt.isPresent()) {
    Cliente cliente = opt.get();
    // processar
}

// Forma mais elegante com orElseThrow
Cliente cliente = clienteRepository.findByEmail(email)
    .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

// Encadeamento com map
String nome = clienteRepository.findByEmail(email)
    .map(Cliente::getNome)
    .orElse("Anônimo");
```

---

## 5. Functional Interfaces

### O que são?

Interfaces com apenas um método abstrato. A base para expressões lambda.

### Principais Interfaces java.util.function

| Interface           | Método                | Descrição         |
| ------------------- | --------------------- | ----------------- |
| `Function<T,R>`     | `R apply(T t)`        | Transforma T em R |
| `Predicate<T>`      | `boolean test(T t)`   | Retorna boolean   |
| `Consumer<T>`       | `void accept(T t)`    | Executa ação      |
| `Supplier<T>`       | `T get()`             | Fornece valor     |
| `UnaryOperator<T>`  | `T apply(T t)`        | Operador unário   |
| `BinaryOperator<T>` | `T apply(T t1, T t2)` | Operador binário  |

### Exemplos no Projeto

```java
// Function: Converter DTO para Entidade
Function<SignupDTO, Cliente> toEntity = dto -> {
    Cliente c = new Cliente();
    c.setNome(dto.getNome());
    c.setEmail(dto.getEmail());
    return c;
};

// Predicate: Verificar se está aprovado
Predicate<LivroAnuncio> isAprovado = livro -> Boolean.TRUE.equals(livro.getAprovado());

// Consumer: Imprimir cliente
Consumer<Cliente> printCliente = c -> System.out.println(c.getNome());

// Supplier: Fornecer valor padrão
Supplier<LocalDateTime> agora = () -> LocalDateTime.now();
```

---

## 6. Construtor Privado com Builder (Pattern Builder)

### O que é?

O padrão Builder permite construir objetos complexos passo a passo. Lombok
simplifica com `@Builder`.

### Exemplo no Projeto

```java
// Entidade com Builder
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LivroAnuncio {
    private String titulo;
    private String autor;
    private Boolean aprovado;
    // ...
}

// Criação do objeto
LivroAnuncio anuncio = LivroAnuncio.builder()
    .titulo("Dom Quixote")
    .autor("Miguel de Cervantes")
    .aprovado(false)
    .build();
```

---

## 7. Programação Orientada a Aspectos com AOP

### Conceito

AOP permite separar cross-cutting concerns (loggin, segurança, transações) do
código de negócio.

### Exemplo: @Transactional

```java
@Transactional
public ClienteDTO salvarCliente(SignupDTO signupDTO) {
    // Antes: Início de transação
    validarDadosSignup(signupDTO);
    Cliente cliente = clienteMapper.toEntity(signupDTO);
    cliente.setSenha(passwordEncoder.encode(signupDTO.getSenha()));
    Cliente salvo = clienteRepository.save(cliente);
    // Depois: Commit ou Rollback automático
    return clienteMapper.toDTO(salvo);
}
```

---

## 8. Injeção de Dependência (DI)

### O que é?

Padrão onde objetos recebem suas dependências de fontes externas em vez de
criá-las internamente.

### Exemplo no Projeto

```java
@Service
@RequiredArgsConstructor
public class ClienteService {
    // Dependencies injected via constructor
    private final ClienteRepository clienteRepository;
    private final CartaoRepository cartaoRepository;
    private final EmailService emailService;
    // Lombok gera o construtor automaticamente
}

// Uso em Controller
@Controller
@RequiredArgsConstructor
public class ClientController {
    private final ClienteService clienteService;
    private final LogAuditoriaService logAuditoriaService;
}
```

---

## 9. Singleton Pattern

### Conceito

Garante que uma classe tenha apenas uma instância e fornece um ponto global de
acesso a ela.

### Exemplos no Projeto

**Spring Beans são Singletons por padrão:**

```java
@Service
public class ClienteService {
    // Uma única instância criada pelo Spring
    // e compartilhada entre todas as requisições
}
```

---

## 10. DAO (Data Access Object) Pattern

### Conceito

Padrão que abstrai o acesso a dados, isolando a lógica de persistência.

### Exemplo no Projeto

```java
@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    Optional<Cliente> findByEmail(String email);
    boolean existsByEmail(String email);
}
```

---

## 11. DTO (Data Transfer Object)

### Conceito

Objeto para transferência de dados entre camadas, especialmente entre API e
banco.

### Exemplo no Projeto

```java
// DTO para resposta
public class ClienteDTO {
    private Long id;
    private String nome;
    private String email;
    private Double saldoTokens;
    // Sem dados sensíveis como senha
}

// Conversão Entity ↔ DTO via Mapper
ClienteDTO dto = clienteMapper.toDTO(cliente);
```

---

## 12. Strategy Pattern

### Conceito

Define uma família de algoritmos, encapsula cada um e os torna intercambiáveis.

### Exemplo no Projeto

```java
// Interface Strategy
public interface PagamentoStrategy {
    boolean processar(double valor, CompraTokensRequestDTO dados);
    String getTipoPagamento();
}

// Estratégias concretas
@Component
public class PagamentoPixStrategy implements PagamentoStrategy {
    @Override
    public boolean processar(double valor, CompraTokensRequestDTO dados) {
        // Lógica específica do PIX
    }
}

@Component
public class PagamentoCartaoStrategy implements PagamentoStrategy {
    @Override
    public boolean processar(double valor, CompraTokensRequestDTO dados) {
        // Lógica específica do Cartão
    }
}

// Uso
@Service
public class TokenController {
    private final PagamentoFactory pagamentoFactory;
    
    public void comprar(CompraTokensRequestDTO request) {
        PagamentoStrategy estrategia = pagamentoFactory.buscarEstrategia(request.getMetodoPagamento());
        estrategia.processar(request.getValor(), request);
    }
}
```

---

## 13. Factory Pattern

### Conceito

Padrão que cria objetos sem especificar a classe exata do objeto que será
criado.

### Exemplo no Projeto

```java
@Service
public class PagamentoFactory {
    private final Map<String, PagamentoStrategy> estrategias;

    public PagamentoFactory(List<PagamentoStrategy> listaEstrategias) {
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
                .orElseThrow(() -> new IllegalArgumentException("Método não suportado"));
    }
}
```

---

## 14. JWT (JSON Web Token)

### O que é?

Token compactado e autocontido que transmite claims (declarações) entre partes.

### Estrutura do JWT

```
xxxxx.yyyyy.zzzzz
Header.Payload.Signature
```

### Exemplo no Projeto

```java
@Component
public class JwtUtil {
    
    public String generateToken(String subject) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(subject)  // Email do usuário
                .issuedAt(now)
                .expiration(exp)
                .signWith(getSigningKey())
                .compact();
    }

    public String extractUsername(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }
}
```

---

## 15. Bean Validation

### Conceito

Framework para validar objetos usando anotações.

### Exemplo no Projeto

```java
public class SignupDTO {
    @NotBlank(message = "O CPF é obrigatório.")
    @Pattern(regexp = "\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}", message = "Formato: 000.000.000-00")
    private String cpf;

    @NotBlank(message = "O e-mail é obrigatório.")
    @Email(message = "Insira um e-mail válido.")
    private String email;

    @NotBlank(message = "A senha é obrigatória.")
    @Size(min = 8, message = "A senha deve ter no mínimo 8 caracteres.")
    private String senha;
}
```

---

## 16. Lombok - Simplificando Código

### @RequiredArgsConstructor + final

```java
@Service
@RequiredArgsConstructor  // Gera construtor com todos os campos final
public class ClienteService {
    private final ClienteRepository clienteRepository;
    private final CartaoRepository cartaoRepository;
    private final EmailService emailService;
}

// Equivalente a:
@Service
public class ClienteService {
    private final ClienteRepository clienteRepository;
    private final CartaoRepository cartaoRepository;
    private final EmailService emailService;
    
    @Autowired
    public ClienteService(ClienteRepository cr, CartaoRepository car, EmailService es) {
        this.clienteRepository = cr;
        this.cartaoRepository = car;
        this.emailService = es;
    }
}
```

### @Builder para Objetos Complexos

```java
// Sem Builder
LivroAnuncio livro = new LivroAnuncio();
livro.setTitulo("Titulo");
livro.setAutor("Autor");
livro.setAprovado(false);

// Com Builder
LivroAnuncio livro = LivroAnuncio.builder()
    .titulo("Titulo")
    .autor("Autor")
    .aprovado(false)
    .build();
```

---

## 17. Mapeamento Objeto-Relacional (JPA/Hibernate)

### Relacionamentos

```java
// ManyToOne - Muitos para Um
@ManyToOne
private Cliente vendedor;  // Um livro tem um vendedor

// OneToMany - Um para Muitos
@OneToMany(mappedBy = "vendedor")
private List<LivroAnuncio> livros;  // Um cliente pode vender muitos livros

// ManyToMany - Muitos para Muitos
@ManyToMany
@JoinTable(name = "cliente_cartao")
private Set<Cartao> cartoes;  // Um cliente pode ter vários cartões
```

### Fetch Types

```java
// EAGER - Carrega imediatamente
@ManyToOne(fetch = FetchType.EAGER)
private Cliente vendedor;

// LAZY - Carrega sob demanda (preferível para performance)
@ManyToMany(fetch = FetchType.LAZY)
private Set<Cartao> cartoes;
```

---

## 18. Cascading Operations

```java
// Cascade PERSIST - Salva o relacionado junto
@ManyToMany(cascade = CascadeType.PERSIST)
private Set<Cartao> cartoes;

// Cascade MERGE - Atualiza o relacionado junto
@ManyToMany(cascade = CascadeType.MERGE)

// Cascade REMOVE - Remove o relacionado junto
@OneToMany(cascade = CascadeType.REMOVE)

// Cascade ALL - Todas as operações
@OneToMany(cascade = CascadeType.ALL)
```

---

## 19. MapStruct - Mapeamento automático

```java
@Mapper(componentModel = "spring")
public interface ClienteMapper {
    
    // Converte SignupDTO para Entidade
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "senha", ignore = true)  // Será definida manualmente
    Cliente toEntity(SignupDTO dto);

    // Converte Entidade para DTO
    @Mapping(target = "senha", ignore = true)  // Nunca expõe senha
    ClienteDTO toDTO(Cliente cliente);

    // Atualiza entidade existente
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "email", ignore = true)
    void updateEntityFromDto(ClienteDTO dto, @MappingTarget Cliente entity);
}
```

---

## 20. Resumo de Conversões

### Traditional Loop vs Stream

```java
// FILTRAR
// Loop tradicional
List<Livro> aprovados = new ArrayList<>();
for (Livro l : livros) {
    if (l.getAprovado()) {
        aprovados.add(l);
    }
}

// Stream
List<Luke> aprovados = livros.stream()
    .filter(l -> l.getAprovado())
    .collect(Collectors.toList());


// TRANSFORMAR
// Loop tradicional
List<String> nomes = new ArrayList<>();
for (Cliente c : clientes) {
    nomes.add(c.getNome());
}

// Stream
List<String> nomes = clientes.stream()
    .map(Cliente::getNome)
    .collect(Collectors.toList());


// ENCONTRAR
// Loop tradicional
Cliente encontrado = null;
for (Cliente c : clientes) {
    if (c.getEmail().equals(email)) {
        encontrado = c;
        break;
    }
}

// Stream
Cliente encontrado = clientes.stream()
    .filter(c -> c.getEmail().equals(email))
    .findFirst()
    .orElse(null);


// CONTAR
// Loop tradicional
int count = 0;
for (Livro l : livros) {
    if (l.getAprovado()) {
        count++;
    }
}

// Stream
long count = livros.stream()
    .filter(Livro::getAprovado)
    .count();
```

---

## Conclusão

Estes conceitos formam a base do código moderno em Java. A combinação de:

- **Expressões Lambda** + **Streams** = Código funcional e conciso
- **Optional** = Tratamento seguro de nulos
- **Design Patterns** (Factory, Strategy, Builder) = Código manutenível
- **Spring** = Injeção de dependência e inversão de controle
- **Lombok** = Redução de boilerplate
- **JPA/Hibernate** = Mapeamento objeto-relacional

Torna o código mais legível, manutenível e eficiente.
