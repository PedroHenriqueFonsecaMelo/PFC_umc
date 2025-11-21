# Documentação — pacote umc.exs.backstage

Este documento descreve o pacote `umc.exs.backstage` do projeto PFC UMC. Contém resumo das responsabilidades, descrição de cada classe presente no diretório `backstage` (config, handler, log, security, service) e um guia prático sobre conceitos Java usados no projeto (anotações, streams e lambdas). O objetivo é fornecer uma visão única, concisa e aplicável para manutenção, revisão de segurança e extensão do código.

---

## Visão geral do pacote

O pacote `backstage` concentra infraestrutura e regras transversais da aplicação:
- config: configurações Spring (security, websocket).
- handler: tratamento global de exceções.
- log: serviços/utilitários de auditoria e logging de segurança.
- security: JWT, filtros e integrações com Spring Security.
- service: serviços de domínio e auxiliares (autenticação, validação, email, lógica de cliente).

Boas práticas adotadas/esperadas:
- Injeção por construtor para beans Spring.
- Métodos críticos transacionais anotados com `@Transactional`.
- Dados sensíveis (senha, CVV, CPF) não são logados nem retornados em DTOs.
- Tokens JWT devem conter identificador não sensível (ex.: userId) e ter expiração curta.

---

## Descrição por classe / arquivo

Cada entrada tem: propósito, responsabilidade principal, métodos relevantes (descrição), e observações de segurança ou uso.

### config/SecurityConfig.java
- Propósito: configurar Spring Security (autenticação, autorização, filtros, CORS, proteção CSRF quando aplicável).
- Responsabilidades:
  - Definir regras HTTP (rotas públicas vs protegidas).
  - Registrar filtros personalizados (ex.: JwtRequestFilter).
  - Fornecer beans: `PasswordEncoder` (BCrypt), `AuthenticationManager`.
- Métodos relevantes (conceituais):
  - configure(HttpSecurity http): define políticas de acesso, filtros e CSRF.
  - authenticationManagerBean(): expõe `AuthenticationManager`.
  - passwordEncoder(): retorna `BCryptPasswordEncoder`.
- Observações:
  - Garantir uso de HttpOnly + Secure cookies para tokens em produção.
  - Evitar permitir origens amplas em CORS para endpoints sensíveis.

### config/WebSocketConfig.java
- Propósito: configuração de WebSocket/STOMP se suportado.
- Responsabilidades:
  - Registrar endpoints STOMP, configurar broker simples/remoto.
  - Ajustar allowed origins e políticas de handshake.
- Observações:
  - Autenticação via token deve ser validada durante handshake.

### handler/GlobalExceptionHandler.java
- Propósito: capturar exceções não tratadas e devolver respostas amigáveis e consistentes.
- Responsabilidades:
  - Mapear exceções comuns (DataIntegrityViolation, AccessDenied, custom domain exceptions).
  - Logar exceções relevantes via `LogAuditoriaService`.
- Métodos típicos:
  - handlers para Exception, RuntimeException, DataAccessException, MethodArgumentNotValidException.
- Observações:
  - Não incluir stacktrace nas respostas em produção; retornar códigos HTTP apropriados e mensagens genéricas.

### log/LogAuditoriaService.java
- Propósito: persistir e gerenciar logs de auditoria.
- Responsabilidades:
  - Gravar ações críticas do usuário (login, alteração de dados, operações administrativas).
- Métodos típicos:
  - registrarAcao(String acao, String detalhes, String usuarioId)
  - listarLogs(...), consultarPorUsuario(...)
- Observações:
  - Não gravar dados sensíveis (ex.: CVV); gravar referências/IDs e mensagens ofuscadas.

### log/SecurityLogger.java
- Propósito: utilitários para logging focado em segurança.
- Responsabilidades:
  - Logar tentativas de login, bloqueios, falhas de autenticação.
- Uso:
  - Chamado por filtro JWT e serviços de autenticação; delega persistência ao `LogAuditoriaService`.

### security/JwtUtil.java
- Propósito: gerar, assinar e validar tokens JWT.
- Responsabilidades:
  - Criar token com claims essenciais (sub, iat, exp, roles).
  - Validar assinatura e expiração.
  - Extrair claims úteis (subject / id).
- Métodos típicos:
  - String generateToken(UserDetails userDetails)
  - Boolean validateToken(String token, UserDetails userDetails)
  - String extractUsername(String token)
  - Date extractExpiration(String token)
- Observações:
  - Use chave/segredo seguro e renovação de chaves em produção. Não colocar dados sensíveis no `sub`.

### security/JwtUserDetailsService.java
- Propósito: integrar repositório de usuários com Spring Security.
- Responsabilidades:
  - Implementar `UserDetailsService::loadUserByUsername`.
  - Converter entidade do BD para `UserDetails` com roles e estado da conta.
- Observações:
  - Buscar por identificador seguro (email/id); não expor CPF no fluxo de autenticação.

### security/JwtRequestFilter.java
- Propósito: filtro que intercepta requests, extrai token e popula `SecurityContext`.
- Responsabilidades:
  - Ler token do cookie/Authorization header.
  - Validar token via `JwtUtil` e construir `UsernamePasswordAuthenticationToken`.
- Método principal:
  - doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
- Observações:
  - Tratar corretamente casos de token expirado e logar eventos relevantes.

### security/JwtAuthenticationEntryPoint.java
- Propósito: responder para requisições não autenticadas (401).
- Responsabilidades:
  - Implementar `AuthenticationEntryPoint::commence` retornando payload apropriado (JSON ou redirect).
- Observações:
  - Não vazar detalhes de validação do token na resposta.

### service/AuthHelper.java
- Propósito: funcionalidades utilitárias para autenticação/login.
- Responsabilidades:
  - Authentic flow: validar credenciais, criar token, criar cookie seguro.
- Métodos típicos:
  - autenticar(String idOuEmail, String senhaPlain)
  - criarCookieToken(String token, boolean rememberMe)
- Observações:
  - Nunca comparar senhas em texto puro; usar `PasswordEncoder.matches`.

### service/ClienteService.java
- Propósito: lógica de negócio para clientes (CRUD e associações).
- Responsabilidades:
  - Criar e atualizar cliente, associar cartões e endereços, validar dados de cartão.
- Métodos típicos:
  - salvarCliente(ClienteDTO dto)
  - atualizarCliente(ClienteDTO dto)
  - validarDadosCartao(CartaoDTO dto)
- Observações:
  - Use `@Transactional` onde necessário; converter tipos (ex.: YearMonth → String) antes de persistir.

### service/EmailService.java
- Propósito: envio de e-mails (confirmação, recuperação de senha).
- Responsabilidades:
  - Montar templates, enviar via SMTP configurado em application-*.properties.
- Métodos típicos:
  - enviarConfirmacaoCadastro(String email, String token)
  - enviarRecuperacaoSenha(String email, String token)
- Observações:
  - Não incluir dados sensíveis nos templates; usar links com tokens com expiração curta.

### service/FieldValidation.java
- Propósito: helpers de validação (CPF, e-mail, data).
- Métodos típicos:
  - validarCPF(String cpf)
  - validarEmail(String email)
  - validarDataNascimento(LocalDate d)
- Observações:
  - Reutilize validadores nos DTOs via annotations e em serviços para validação adicional.

---

## Guia rápido: anotações, streams e lambdas em Java (aplicado ao projeto)

Esta seção reúne conceitos essenciais usados no código.

### Anotações (Annotations)
- O que são: metadados que instruem ferramentas/frames em tempo de compilação ou runtime.
- Anotações comuns do projeto:
  - Spring: `@Configuration`, `@Bean`, `@Service`, `@Repository`, `@Controller`, `@RestController`, `@Autowired` (evitar field injection; preferir constructor injection), `@Component`.
  - Security: `@EnableWebSecurity`.
  - Transação: `@Transactional` — garante commit/rollback automático.
  - MVC/validations: `@ControllerAdvice`, `@ExceptionHandler`, `@Valid`, `@NotNull`, `@Email`, `@Size`.
- Boas práticas:
  - Anotar DTOs com validações (`@NotBlank`, `@Email`) e validar em controllers com `@Valid` + BindingResult.
  - Usar `@ControllerAdvice` para centralizar erros.

### Streams (java.util.stream)
- Conceito: pipeline funcional para processar coleções de forma declarativa e possivelmente paralela.
- Operações comuns:
  - Intermediate: filter(predicate), map(func), flatMap(func), sorted(), distinct()
  - Terminal: collect(Collectors.toList()), findFirst(), forEach(), reduce()
- Exemplo prático (mapper de entidades para DTOs):
  ```
  clientes.stream()
    .map(ClienteMapper::fromEntity)
    .collect(Collectors.toList());
  ```
- Boas práticas:
  - Evitar streams muito complexos (divida em passos claros).
  - Use Collectors.groupingBy para agregações e toMap quando necessário.
  - Atenção a operações que lançam checked exceptions; trate-as antes de stream.

### Lambdas e Method References
- Lambdas: `(params) -> expressão` ou bloco `{ ... }`.
  - Ex.: `it -> it.getEmail().equals(email)`
- Method references: `Classe::metodo` — atalho para lambdas que chamam um método existente.
  - Ex.: `ClienteMapper::fromEntity`
- Interfaces funcionais standard: `Function`, `Consumer`, `Predicate`, `Supplier`.
- Vantagens:
  - Código mais conciso e legível ao trabalhar com streams e APIs reativas.

### Exemplo integrado (validação + stream)
- Filtrar cartões válidos e mapear para DTO:
  ```
  List<CartaoDTO> validos = cliente.getCartoes().stream()
    .filter(c -> CartaoMapper.stringToYearMonth(c.getValidade()).isAfter(YearMonth.now()))
    .map(CartaoMapper::fromEntity)
    .collect(Collectors.toList());
  ```

---

## Segurança e práticas recomendadas (resumo)
- Nunca persistir CVV; evitar logar CPF em claro.
- Tokens JWT: armazenar em cookie HttpOnly+Secure; renovar período de expiração.
- CORS: restringir origins em produção.
- Senhas: usar BCrypt com strength adequado; não comparar texto bruto.
- Exceções: mapear e logar internamente; fornecer mensagens genéricas ao cliente.

---

## Como usar este documento
- Manter este arquivo atualizado ao alterar classes em `backstage`.
- Para documentação método-a-método automática, executar processo de extração do código-fonte e regenerar seção de métodos (eu posso gerar se solicitar).
- Use este documento como referência rápida para revisão de segurança e onboarding de desenvolvedores.
