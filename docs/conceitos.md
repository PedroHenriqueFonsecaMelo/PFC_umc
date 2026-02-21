# Conceitos Java úteis (em português) — anotações, streams e lambdas

1) Anotações (Annotations)
- O que são: metadados aplicados a classes, métodos e campos. Sintaxe: @Nome.
- Exemplos no projeto:
  - @Entity — marca classe JPA persistente.
  - @Table(name="...") — define nome da tabela.
  - @Id, @GeneratedValue — identificador e estratégia.
  - @Service, @Repository, @Controller, @Configuration — stereotype beans Spring.
  - @Autowired — injeção de dependência (preferir constructor injection).
  - @Transactional — define transação para métodos/classes.
  - @ControllerAdvice, @ExceptionHandler — tratamento global de exceções.
  - @Valid, @NotNull, @Email, @Size — validação de beans (jakarta/validation).
- Boas práticas:
  - Preferir constructor injection a field injection.
  - Anotações de validação aplicadas em DTOs (entrada).

2) Streams (java.util.stream)
- O que é: API para processamento funcional de coleções (stream pipeline).
- Operações principais:
  - source.stream().filter(p -> ...).map(x -> ...).collect(Collectors.toList())
  - filter — mantém elementos que satisfazem predicate.
  - map — transforma elementos.
  - flatMap — achata estruturas aninhadas.
  - sorted, distinct, limit, skip — manipulação de ordenação e tamanho.
  - collect — terminal operation: Collectors.toList(), toSet(), groupingBy(), joining(), toMap().
- Exemplos de uso em projeto (padrões):
  - converter lista de entidades em DTOs:
    clientes.stream().map(ClienteMapper::fromEntity).collect(Collectors.toList());
  - encontrar um item:
    produtos.stream().filter(p -> p.getId().equals(id)).findFirst().orElse(null);

3) Lambdas e interfaces funcionais
- Lambdas: sintaxe (params) -> expressão/bloco. Ex.: x -> x.getNome().
- Interfaces funcionais: possuem um único método abstrato (ex.: Predicate<T>, Function<T,R>, Consumer<T>, Supplier<T>).
- Method references: Classe::metodo — atalho para lambdas que chamam métodos existentes.
  - Exemplos: ClienteMapper::fromEntity, System.out::println
- Vantagens:
  - Código mais conciso e declarativo.
  - Facilita uso de Streams e APIs assíncronas.

4) Optional
- Uso para evitar null checks: Optional<T> opt = Optional.ofNullable(valor);
- Métodos: isPresent(), ifPresent(), orElse(), orElseGet(), orElseThrow(), map(), flatMap().

5) Tratamento de exceções e try-with-resources
- try-with-resources: try (Resource r = ...) { ... } — fecha recursos automaticamente.
- Capturar exceções esperadas e relançar como exceções de domínio quando necessário.

6) Boas práticas de segurança relacionadas a conceitos Java
- Nunca logar dados sensíveis (senha, CVV, CPF em claro).
- Comparar senhas usando PasswordEncoder.matches(raw, encoded) (não comparar strings).
- Evitar usar dados sensíveis como subject de tokens JWT; prefira ID interno ou token derivado.

7) Exemplo rápido
- converter e filtrar cartões válidos e mapear para DTO:
  clientes.stream()
    .flatMap(c -> c.getCartoes().stream())
    .filter(cartao -> cartao.getValidadeAnoMes().isAfter(YearMonth.now()))
    .map(CartaoMapper::fromEntity)
    .collect(Collectors.toList());

Fim do resumo de conceitos. Consulte a documentação oficial Oracle/OpenJDK para aprofundamento.