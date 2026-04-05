# 📖 Dicionário de Anotações (Annotations)

Este documento explica todas as anotações (annotations) utilizadas no projeto,
organizadas por categoria.

---

## 1. Anotações do Spring Framework

### @SpringBootApplication

**Definição**: Anotação principal que marca a classe principal de uma aplicação
Spring Boot. É uma combinação de três anotações:

- `@Configuration`: Indica que a classe define beans
- `@EnableAutoConfiguration`: Habilita a configuração automática do Spring
- `@ComponentScan`: Escaneia componentes no pacote atual

**Exemplo de uso**:

```java
@SpringBootApplication
public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }
}
```

---

### @Controller

**Definição**: Marca uma classe como um Controller do Spring MVC que faz
requisições web retornando views (Thymeleaf/JSP).

**Exemplo de uso**:

```java
@Controller
@RequestMapping("/clientes")
public class ClientController {
    // Métodos que retornam nomes de views
}
```

---

### @RestController

**Definição**: Combinação de `@Controller` e `@ResponseBody`. Usado para criar
APIs REST que retornam dados JSON/XML diretamente no corpo da resposta.

**Exemplo de uso**:

```java
@RestController
@RequestMapping("/api/livros")
public class LivroControllerApi {
    @GetMapping("/todos")
    public List<LivroAnuncio> listarTodos() {
        return livroService.listarLivrosAprovados();
    }
}
```

---

### @RequestMapping

**Definição**: Mapeia requisições HTTP para métodos específicos em controllers.
Pode ser usado no nível da classe ou do método.

**Parâmetros principais**:

- `value`/`path`: URL da rota
- `method`: Método HTTP (GET, POST, PUT, DELETE)
- `consumes`: Tipo de conteúdo aceito (ex:
  `MediaType.MULTIPART_FORM_DATA_VALUE`)
- `produces`: Tipo de conteúdo produzido (ex: `application/json`)

**Exemplo de uso**:

```java
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    @GetMapping("/livros/pendentes")
    public ResponseEntity<List<LivroAnuncio>> listarLivrosPendentes() { ... }
}
```

---

### @GetMapping, @PostMapping, @PutMapping, @DeleteMapping

**Definição**: Anotações especializadas para mapear métodos HTTP específicos.
São formas mais específicas de `@RequestMapping`.

**Exemplo de uso**:

```java
@PostMapping("/login")
public String realizarLogin(@Valid @ModelAttribute LoginDTO loginDTO, ...) { ... }

@GetMapping("/meu-perfil")
public String exibirPerfil(@AuthenticationPrincipal UserDetails user, Model model) { ... }
```

---

### @RequestParam

**Definição**: Extrai parâmetros de query string ou parâmetros de formulário da
requisição HTTP.

**Exemplo de uso**:

```java
@PostMapping("/comprar-tokens")
public String comprarTokens(
    @RequestParam Double valor,
    @RequestParam String metodo,
    @RequestParam(required = false) String numCartao,
    @AuthenticationPrincipal UserDetails user, 
    RedirectAttributes ra
) { ... }
```

---

### @PathVariable

**Definição**: Extrai variáveis da URL (path variables).

**Exemplo de uso**:

```java
@GetMapping("/{isbn}/historia")
public String paginaHistoriaLivro(@PathVariable String isbn) {
    return "produto/historia_livro";
}
```

---

### @ModelAttribute

**Definição**: Vincula parâmetros de requisição ou objetos de sessão a objetos
de método handler. Usado em formulários Thymeleaf.

**Exemplo de uso**:

```java
@PostMapping("/atualizar")
public String atualizarCliente(
    @ModelAttribute("cliente") ClienteDTO clienteDTO,
    @AuthenticationPrincipal UserDetails user, 
    RedirectAttributes ra
) { ... }
```

---

### @RequestBody

**Definição**: Vincula o corpo da requisição HTTP a um objeto. Usado em APIs
REST para receber JSON.

**Exemplo de uso**:

```java
@PostMapping("/login")
public ResponseEntity<?> login(@RequestBody LoginDTO loginDto, HttpServletResponse response) {
    // O JSON do corpo da requisição é convertido para LoginDTO automaticamente
}
```

---

### @AuthenticationPrincipal

**Definição**: Injeta o objeto do usuário autenticado (do Spring Security) no
método do controller.

**Exemplo de uso**:

```java
@GetMapping("/meu-perfil")
public String exibirPerfil(@AuthenticationPrincipal UserDetails user, Model model) {
    if (user == null) return "redirect:/clientes/login";
    // user.getUsername() contém o email do usuário logado
}
```

---

### @Valid

**Definição**: Dispara a validação de beans usando Bean Validation (Jakarta EE).
Requer que o objeto tenha anotações de validação como `@NotBlank`, `@Email`,
etc.

**Exemplo de uso**:

```java
@PostMapping("/login")
public String realizarLogin(
    @Valid @ModelAttribute("loginData") LoginDTO loginDTO, 
    BindingResult result, ...
) { ... }
```

---

### @Transactional

**Definição**: Define que um método deve ser executado dentro de uma transação
de banco de dados. O Spring gerencia o commit/rollback automaticamente.

**Parâmetros principais**:

- `readOnly`: Se true, otimiza a transação para leituras
- `rollbackFor`: Define quais exceções disparam rollback

**Exemplo de uso**:

```java
@Transactional
public ClienteDTO salvarCliente(SignupDTO signupDTO) {
    validarDadosSignup(signupDTO);
    Cliente cliente = clienteMapper.toEntity(signupDTO);
    cliente.setSenha(passwordEncoder.encode(signupDTO.getSenha()));
    return clienteMapper.toDTO(clienteRepository.save(cliente));
}
```

---

### @Service

**Definição**: Marca uma classe como componente de serviço do Spring. É um
estereótipo especializado de `@Component`.

**Exemplo de uso**:

```java
@Service
@RequiredArgsConstructor
public class ClienteService {
    // Lógica de negócio relacionada a clientes
}
```

---

### @Component

**Definição**: Anotação genérica que marca uma classe como componente gerenciado
pelo Spring. Classes com esta anotação são automaticamente detectadas e
registradas como beans.

**Exemplo de uso**:

```java
@Component
public class JwtRequestFilter extends OncePerRequestFilter {
    // Filtro de segurança
}
```

---

### @Configuration

**Definição**: Indica que a classe define configurações do Spring. Pode conter
métodos `@Bean`.

**Exemplo de uso**:

```java
@Configuration
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

---

### @Bean

**Definição**: Indica que o método produz um bean gerenciado pelo Spring para
ser injetado em outros componentes.

**Exemplo de uso**:

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration cfg = new CorsConfiguration();
    // configuração...
    return source;
}
```

---

### @Autowired

**Definição**: Injeção de dependência automática. O Spring injeta beans
automaticamente onde necessário.

**Exemplo de uso**:

```java
@Autowired
private JwtUtil jwtUtil;

@Autowired
private ClienteService clienteService;
```

---

### @RequiredArgsConstructor (Lombok)

**Definição**: Gera um construtor com todos os campos finais (required).
Equivalente à injeção via `@Autowired` em campos, mas com código mais limpo.

**Exemplo de uso**:

```java
@Service
@RequiredArgsConstructor
public class ClienteService {
    private final ClienteRepository clienteRepository;
    private final CartaoRepository cartaoRepository;
    // Lombok gera: public ClienteService(ClienteRepository, CartaoRepository, ...)
}
```

---

### @Value

**Definição**: Injeta valores de propriedades (application.properties) em
campos.

**Exemplo de uso**:

```java
@Value("${app.base-url:https://localhost:8443}")
private String baseUrl;
```

---

## 2. Anotações de Persistência (JPA/Hibernate)

### @Entity

**Definição**: Marca uma classe como uma entidade JPA, mapeando-a para uma
tabela no banco de dados.

**Exemplo de uso**:

```java
@Entity
@Table(name = "users")
public class Cliente {
    // Mapeado para a tabela "users"
}
```

---

### @Table

**Definição**: Especifica o nome da tabela no banco de dados. Se omitida, usa o
nome da classe.

**Exemplo de uso**:

```java
@Entity
@Table(name = "users", indexes = @Index(columnList = "email"))
public class Cliente { ... }
```

---

### @Id

**Definição**: Marca um campo como a chave primária da entidade.

---

### @GeneratedValue

**Definição**: Especifica a estratégia de geração de valores para a chave
primária.

**Estratégias disponíveis**:

- `GenerationType.IDENTITY`: Auto-increment no banco
- `GenerationType.SEQUENCE`: Sequência do banco
- `GenerationType.AUTO`: O provedor JPA escolhe
- `GenerationType.TABLE`: Tabela de geração de IDs

**Exemplo de uso**:

```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```

---

### @Column

**Definição**: Mapeia o campo para uma coluna específica no banco de dados.

**Parâmetros**:

- `name`: Nome da coluna
- `nullable`: Se pode ser nulo
- `unique`: Se valores devem ser únicos
- `length`: Tamanho máximo (para Strings)

**Exemplo de uso**:

```java
@Column(nullable = false, unique = true, length = 14)
private String cpf;
```

---

### @ManyToOne

**Definição**: Define uma relação muitos-para-um entre entidades.

**Parâmetros principais**:

- `fetch`: EAGER (carrega junto) ou LAZY (carrega sob demanda)
- `cascade`: Operações em cascata (PERSIST, MERGE, REMOVE, etc.)

**Exemplo de uso**:

```java
@ManyToOne
private Cliente vendedor;
```

---

### @OneToMany / @ManyToMany

**Definição**: Define relações um-para-muitos ou muitos-para-muitos.

**Exemplo de uso**:

```java
@ManyToMany(fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE })
@JoinTable(
    name = "cliente_cartao", 
    joinColumns = @JoinColumn(name = "cliente_id"), 
    inverseJoinColumns = @JoinColumn(name = "cartao_id")
)
private Set<Cartao> cartoes = new HashSet<>();
```

---

### @JoinTable / @JoinColumn

**Definição**: Especifica a tabela de junção em relações muitos-para-muitos ou
as colunas de chave estrangeira.

---

### @Enumerated

**Definição**: Especifica como enumeradores (enums) devem ser persistidos.

**Estratégias**:

- `EnumType.STRING`: Persiste o nome do enum (ex: "NOVO")
- `EnumType.ORDINAL`: Persiste a posição ordinal (ex: 0)

**Exemplo de uso**:

```java
@Enumerated(EnumType.STRING)
private EstadoLivro estadoAprovado;
```

---

### @CreationTimestamp

**Definição**: (Hibernate) Define que o campo deve ser automaticamente populado
com a data/hora de criação quando a entidade for persistida.

**Exemplo de uso**:

```java
@CreationTimestamp
@Column(nullable = false, updatable = false)
private LocalDateTime dataCriacao;
```

---

## 3. Anotações de Validação (Bean Validation)

### @NotBlank

**Definição**: Valida que a String não é nula e não contém apenas espaços em
branco.

---

### @NotNull

**Definição**: Valida que o valor não é nulo.

---

### @Email

**Definição**: Valida que a String é um endereço de email válido.

---

### @Size

**Definição**: Valida o tamanho de Strings, Collections, Arrays.

**Parâmetros**:

- `min`: Tamanho mínimo
- `max`: Tamanho máximo

---

### @Min / @Max

**Definição**: Valida que o número é maior/menor que um valor específico.

---

### @Pattern

**Definição**: Valida que a String corresponde a uma expressão regular.

---

### @AssertTrue

**Definição**: Valida que o método retorna true. Usado para validações
customizadas.

---

## 4. Anotações do Lombok

### @Getter / @Setter

**Definição**: Gera automaticamente métodos getters e setters para todos os
campos.

---

### @AllArgsConstructor

**Definição**: Gera um construtor com todos os campos como parâmetros.

---

### @NoArgsConstructor

**Definição**: Gera um construtor sem argumentos.

---

### @RequiredArgsConstructor

**Definição**: Gera um construtor com apenas campos finais (`final`), que são
necessários para injeção de dependência.

---

### @Builder

**Definição**: Implementa o padrão Builder para criação de objetos. Permite
construção fluente.

**Exemplo de uso**:

```java
LivroAnuncio anuncio = LivroAnuncio.builder()
    .titulo(dto.getTitulo())
    .autor(dto.getAutor())
    .isbn(dto.getIsbn())
    .fotoUrl("/uploads/livros/" + nomeFoto)
    .vendedor(vendedor)
    .dataAnuncio(LocalDateTime.now())
    .aprovado(false)
    .build();
```

---

### @Data

**Definição**: Combinação de `@Getter`, `@Setter`, `@ToString`,
`@EqualsAndHashCode`, `@RequiredArgsConstructor`. Cuidado: pode causar problemas
em entidades JPA com relações bidirecionais.

---

### @EqualsAndHashCode

**Definição**: Gera métodos equals() e hashCode(). `of = "id"` usa apenas o ID
para comparação.

---

### @ToString

**Definição**: Gera método toString(). `exclude` evita circularidade em relações
bidirecionais.

---

## 5. Anotações de Segurança (Spring Security)

### @PreAuthorize

**Definição**: Define que o método só pode ser executado por usuários com
autoridades específicas.

**Exemplo de uso**:

```java
@PreAuthorize("hasAuthority('ADMIN')")
public String painelAdmin() { ... }
```

---

## 6. Anotações do MapStruct

### @Mapper

**Definição**: Marca uma interface como mapper do MapStruct, gerando código para
converter entre objetos.

**Parâmetros**:

- `componentModel = "spring"`: Integra com Spring (injeta como bean)
- `uses`: Mappers dependentes

**Exemplo de uso**:

```java
@Mapper(componentModel = "spring", uses = {EnderecoMapper.class, CartaoMapper.class})
public interface ClienteMapper {
    Cliente toEntity(SignupDTO dto);
    ClienteDTO toDTO(Cliente cliente);
}
```

---

### @Mapping

**Definição**: Define mapeamentos customizados entre campos de origem e destino.

**Exemplo de uso**:

```java
@Mapping(target = "id", ignore = true)
@Mapping(target = "senha", ignore = true)
Cliente toEntity(SignupDTO dto);
```

---

### @MappingTarget

**Definição**: Indica que o objeto deve ser atualizado (merge) em vez de criado
novo.

**Exemplo de uso**:

```java
void updateEntityFromDto(ClienteDTO dto, @MappingTarget Cliente entity);
```

---

## 7. Anotações Adicionais do Projeto

### @Profile

**Definição**: Ativa configuração apenas para um perfil específico do Spring
Boot (local, prod, test).

**Exemplo**:

```java
@Configuration
@Profile("local")
public class WebConfig implements WebMvcConfigurer { ... }
```

### @EnableWebSocketMessageBroker

**Definição**: Habilita suporte a WebSocket com STOMP protocol.

**Exemplo**:

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer { ... }
```

### @SuppressWarnings("null")

**Definição**: Suprime warnings específicos do compilador (null checks).

### @NonNull

**Definição**: Indica que parâmetro não pode ser null (Lombok/IDEs).

### @RequiredArgsConstructor (Lombok)

**Definição**: Gera construtor para campos `final`.

## 8. Resumo em Tabela (Texto)

| Annotation      | Pacote      | Uso Principal     |
| --------------- | ----------- | ----------------- |
| @SpringBootApp  | Spring Boot | Classe principal  |
| @Controller     | Spring MVC  | View controllers  |
| @RestController | Spring MVC  | REST APIs         |
| @Service        | Spring      | Services          |
| @Repository     | Spring Data | Repositories      |
| @Component      | Spring      | Components        |
| @Configuration  | Spring      | Configs           |
| @Autowired      | Spring      | DI                |
| @Transactional  | Spring Tx   | Transações        |
| @Entity         | JPA         | Entidades         |
| @Id             | JPA         | PK                |
| @ManyToOne      | JPA         | Relacionamentos   |
| @NotBlank       | Validation  | Validação         |
| @Builder        | Lombok      | Builder           |
| @Mapper         | MapStruct   | DTO mapping       |
| @Profile        | Spring      | Perfil específico |
| @Value          | Spring      | Properties        |

**Atualizado**: Inclui todas anotações do projeto. Tabela textual.
