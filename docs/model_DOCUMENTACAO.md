# Documentação — pacote umc.exs.model

Documento unificado e detalhado do pacote `umc.exs.model`. Contém descrição das responsabilidades, resumo de classes e métodos principais das subpastas (daos, dtos, entidades) e um guia prático sobre conceitos Java aplicados aqui: anotações, streams e lambdas.

---

## Visão Geral do Pacote model

Responsabilidade principal:
- Representar o modelo do domínio (entidades JPA), contratos de entrada/saída (DTOs), mapeadores (mappers) e repositórios (DAOs).
- Isolar a persistência da camada de apresentação por meio de DTOs e mappers.
- Aplicar validações e regras de segurança de dados sensíveis (CPF, cartão).

Estrutura relevante:
- daos/
  - mappers/: CartaoMapper, ClienteMapper, EnderecoMapper
  - repository/: AdminRepository, CartaoRepository, ClienteRepository, EnderecoRepository, LogAuditoriaRepository, RecuperacaoSenhaRepository
- dtos/
  - auth/: LoginDTO, SignupDTO, ResponseDTO, LogDTO
  - interfaces/: CartaoConvertible, ClienteConvertible, EnderecoConvertible
  - user/: CartaoDTO, ClienteDTO, EnderecoDTO, SenhaResetDTO
- entidades/
  - foundation/: Administrador, LogAuditoria, RecuperacaoSenha, enums
  - usuario/: Cliente, Cartao, Endereco

---

## daos — Mappers e Repositórios

Objetivo:
- Mappers convertem entre entidades JPA e DTOs, aplicando regras de formatação (ex.: validade do cartão) e mascaramento.
- Repositórios (Spring Data JPA) expõem operações CRUD e queries customizadas.

### Mappers (CartaoMapper, ClienteMapper, EnderecoMapper)
- Responsabilidades gerais:
  - Converter entidade → DTO (mascarar dados sensíveis, ocultar CVV, converter formatos).
  - Converter DTO → entidade (formatar validade, normalizar strings).
  - Fornecer utilitários de conversão: YearMonth ↔ String, maskCardNumber.

- CartaoMapper (pontos principais):
  - fromEntity(Cartao c) → CartaoDTO:
    - mascara número do cartão (ex.: xxxx xxxx xxxx 1234).
    - converte validade (String "MM/yyyy") → YearMonth.
    - seta cvv = null em DTO (nunca retornar CVV).
  - toEntity(CartaoConvertible dto) → Cartao:
    - converte validade YearMonth → String (format "MM/yyyy").
    - garante formatação do número (números apenas).
  - utilitários:
    - YearMonth stringToYearMonth(String s) — suporta "MM/yyyy" e "yyyy-MM".
    - String yearMonthToString(YearMonth ym) — retorna "MM/yyyy".
    - String maskCardNumber(String number) — mantém últimos 4 dígitos.

- ClienteMapper:
  - fromEntity(Cliente c) → ClienteDTO:
    - mascaramento de CPF (ex.: 000.000.***-**).
    - converte listas de Cartao/Endereco usando CartaoMapper/EnderecoMapper.
  - toEntity/ updateEntityFromDto — converte DTO para entidade, aplica saneamento.

- EnderecoMapper:
  - Converte entre Endereco e EnderecoDTO (normalização de CEP, uppercase em estado/cidade quando desejado).

Boas práticas para mappers:
- Não ligar regras de negócio complexas aqui — apenas transformação/normalização.
- Manter mappers puros (sem chamadas a repositórios).
- Testar masking e parsing intensivamente.

### Repositórios (AdminRepository, CartaoRepository, ClienteRepository, ...)
- Base: extends JpaRepository<T, ID>
- Exemplos de métodos custom:
  - ClienteRepository:
    - Optional<Cliente> findByCpf(String cpf);
    - Optional<Cliente> findByEmail(String email);
  - CartaoRepository:
    - Optional<Cartao> findByValueFields(String numero, String nomeTitular, String validade, String bandeira, String cpfTitular);
      - Observação: validade armazenada como String; passar String, não YearMonth, para evitar IllegalArgumentException do Hibernate.
  - RecuperacaoSenhaRepository, LogAuditoriaRepository: queries por token, por usuário, por período.
- Boas práticas:
  - Evitar consultas que retornam dados sensíveis em texto claro.
  - Tipar parâmetros de consulta de acordo com a coluna no banco.

---

## dtos — Contratos de Entrada/Saída

Objetivo:
- DTOs validam entrada (controllers) e definem saída que será exposta (mascarada).

### auth (LoginDTO, SignupDTO, ResponseDTO, LogDTO)
- LoginDTO:
  - Campos: login (email ou cpf), senha.
  - Anotações típicas: @NotBlank, @Size.
- SignupDTO:
  - Campos: nome, email (@Email), senha (@Size), cpf (@Pattern), dataNascimento.
  - Validação aplicada via annotations e validação adicional em FieldValidation.
- ResponseDTO / LogDTO:
  - Estruturas usadas para padronizar respostas e registros de auditoria.

### interfaces (CartaoConvertible, ClienteConvertible, EnderecoConvertible)
- Objetivo: definir contratos para mappers aceitarem múltiplas representações.
- Ex.: CartaoConvertible define getNumero(), getValidade(), getNomeTitular(), getCpfTitular(), getBandeira(), getCvv().

### user (CartaoDTO, ClienteDTO, EnderecoDTO, SenhaResetDTO)
- CartaoDTO:
  - Campos: id (Long), numero (String), validade (YearMonth), bandeira, nomeTitular, cpfTitular, cvv (String) — cvv somente em DTOs de entrada; ao enviar para front cvv = null.
  - Validações: @NotBlank, custom Luhn validator para numero, validade futura.
- ClienteDTO:
  - Campos: id, nome, email, cpf (mascarado para saída), dataNascimento, List<CartaoDTO>, List<EnderecoDTO>.
  - Validações: @Email, @NotBlank.
- EnderecoDTO:
  - Campos: id, rua, numero, cep, cidade, estado, pais, complemento.
  - Validações de CEP e tamanho de campos.
- SenhaResetDTO:
  - Campos: token, novaSenha, confirmacaoSenha.

Boas práticas de DTOs:
- Usar validações por annotations (@NotBlank, @Email, @Pattern, @Size).
- Não incluir senha/ CVV em DTOs de saída.
- Anotar DTOs de entrada com mensagens de validação claras.

---

## entidades — Entidades JPA (foundation e usuario)

Objetivo:
- Representar persistência do domínio com JPA/Hibernate.

### Convenções comuns
- Anotações JPA:
  - @Entity, @Table(name = "..."), @Id, @GeneratedValue(strategy = ...)
  - @Column(nullable = ..., length = ..., unique = ...)
  - Relacionamentos: @OneToMany, @ManyToOne, @ManyToMany, @JoinColumn, @JoinTable
- Constraint e índices definem unicidade (ex.: cpf único).
- Campos sensíveis:
  - senha (armazenar hashed), cpf (poder armazenar criptografado), numeroCartao (tokenizado/criptografado), cvv NÃO deve ser persistido.

### Entidades principais (resumo)
- Cliente:
  - Campos: id, nome, email, senhaHash, cpf (criptografado/opcional hash), dataNascimento, ativo, dataCriacao.
  - Relacionamentos: List<Cartao> cartoes (@ManyToMany ou @OneToMany com join), List<Endereco> enderecos.
  - Métodos: getters/setters, helper de bloqueio/desbloqueio.
- Cartao:
  - Campos: id, numero (token/criptografado), validade (String "MM/yyyy"), nomeTitular, cpfTitular, bandeira.
  - Importante: não persistir CVV; validar validade antes de aceitar.
  - Associação com Cliente: Set<Cliente> clientes (se compartilhar).
- Endereco:
  - Campos: id, rua, numero, bairro, cidade, estado, cep, pais, tipoResidencia.
- Administrador, LogAuditoria, RecuperacaoSenha:
  - Administrador para controle administrativo.
  - LogAuditoria para auditoria de ações (acao, detalhes, usuarioId, dataHora).
  - RecuperacaoSenha para controle de tokens de reset (token, expiracao, email).

Boas práticas de entidades:
- Usar DTOs para qualquer comunicação externa.
- Evitar exibir entidades em controllers.
- Definir cascades e fetch types conscientes (evitar EAGER para listas grandes).
- Indexar colunas usadas em consultas frequentes (email, cpf).

---

## Exemplos e Convenções de Uso (práticos)

1) Conversão YearMonth ↔ String (CartaoMapper)
- Para persistir:
  - String validadeStr = CartaoMapper.yearMonthToString(dto.getValidade()); // "MM/yyyy"
- Para exibir:
  - YearMonth ym = CartaoMapper.stringToYearMonth(entity.getValidade());

2) Encontrar cartão igual antes de criar
- Use o repositório com tipos corretos:
  - Optional<Cartao> opt = cartaoRepository.findByValueFields(numeroNormalizado, nomeTitular, validadeStr, bandeira, cpfTitular);

3) Exemplo de stream para mapear entidades → DTOs
- List<ClienteDTO> dtos = clientes.stream()
    .map(ClienteMapper::fromEntity)
    .collect(Collectors.toList());

4) Exemplo de Optional para evitar NPE
- Cliente cliente = clienteRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(...));

---

## Conceitos Java aplicados no pacote (Anotações, Streams, Lambdas)

Breve, prático e focado no uso no pacote model.

### Anotações (Annotations)
- Propósito: metadados que influenciam comportamento em tempo de compilação/runtime.
- Anotações comuns neste pacote:
  - jakarta.persistence: @Entity, @Table, @Id, @GeneratedValue, @Column, @ManyToOne, @OneToMany, @ManyToMany, @JoinColumn, @JoinTable, @Enumerated.
  - jakarta.validation: @NotNull, @NotBlank, @Email, @Size, @Pattern — usadas em DTOs para validar entrada.
  - spring stereotypes: @Repository (repositórios), @Service (serviços de domínio), @Component (helpers).
  - @Transactional (em serviços que usam repositórios) para garantir consistência.
- Boas práticas:
  - Validações via annotations em DTOs e validação adicional nas camadas de serviço.
  - Não colocar validações complexas somente via anotações — implementar regras em FieldValidation/service.

### Streams (java.util.stream)
- O que são: API de processamento funcional de coleções.
- Principais operações:
  - Intermediate: filter, map, flatMap, sorted, distinct, peek
  - Terminal: collect, forEach, reduce, findFirst, anyMatch, allMatch
- Exemplo prático (model):
  - Filtrar cartões válidos e transformar para DTOs:
    ```
    List<CartaoDTO> validos = cliente.getCartoes().stream()
      .filter(c -> CartaoMapper.stringToYearMonth(c.getValidade()).isAfter(YearMonth.now()))
      .map(CartaoMapper::fromEntity)
      .collect(Collectors.toList());
    ```
- Observações:
  - Streams não alteram a coleção original; operam sobre elementos.
  - Cuidado com exceções checked dentro de lambdas.

### Lambdas e Method References
- Sintaxe:
  - Lambda: params -> expressão/bloco
  - Method reference: Classe::metodo ou instancia::metodo
- Exemplos no contexto:
  - clientes.stream().map(ClienteMapper::fromEntity).collect(Collectors.toList());
  - lista.forEach(item -> log.info(item.getNome()));
  - lista.forEach(System.out::println);
- Interfaces funcionais úteis:
  - Predicate<T> (usado em filter), Function<T,R> (usado em map), Consumer<T> (forEach).
- Boas práticas:
  - Preferir method references quando possível para clareza.
  - Manter lambdas curtas e sem lógica complexa; mover lógica complexa para métodos nomeados.

### Optional
- Uso para evitar null checks:
  - Optional.ofNullable(x).map(...).orElse(...)
- Exemplo:
  - cartaoRepository.findByValueFields(...).ifPresentOrElse(...)

---

## Segurança, Privacidade e Boas Práticas Específicas do Model

- Nunca persistir CVV; se necessário para processos temporários, mantê-lo apenas em memória e zerar após uso.
- CPF e números de cartão devem ser armazenados tokenizados ou criptografados; use hashing/crypto adequado.
- Não retornar campos sensíveis em DTOs de saída (sempre mascarar).
- Aplicar validações Luhn para números de cartão e checar validade (YearMonth) antes de persistir.
- Documentar migrations e constraints (unique cpf, unique email).

---

## Testes recomendados para o pacote model

- Unit tests para mappers:
  - testar parsing string→YearMonth e formatting YearMonth→String.
  - testar mascaramento de números de cartão.
- Integration tests para repositórios:
  - @DataJpaTest verificando queries custom (findByValueFields).
- Validations:
  - DTOs validados por annotations (testar BindingResult e mensagens).
- Casos de borda:
  - formatos de validade inválidos, números de cartão fora do padrão Luhn, CPFs inválidos.

---

## Como manter este documento atualizado
- Ao alterar assinaturas de DTOs ou entidades, atualizar mappers e este documento.
- Ao adicionar novas queries no repositório, documentar parâmetros esperados (tipo e formato).
- Revisão periódica para alinhamento com políticas de segurança e GDPR/LGPD.

---
