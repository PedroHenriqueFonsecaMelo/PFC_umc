# Documentação — pacote umc.exs.controller

Este documento descreve o pacote `umc.exs.controller` do projeto PFC UMC. Contém descrição concisa e completa das responsabilidades dos controllers, métodos/fluxos principais, regras de segurança recomendadas e guia prático sobre conceitos Java usados aqui (anotações, streams e lambdas). Use este arquivo como referência única para manutenção e revisão de endpoints da camada web.

---

## Visão geral do pacote controller

O pacote `controller` expõe a API HTTP/rotas web da aplicação. O sub-pacote `prod` contém os controllers voltados ao produto/usuário final (páginas e endpoints usados pelo frontend). Cada controller coordena validação de entrada, autorização, chamadas a serviços, transformação para DTOs e construção de respostas (HTML/JSON).

Controllers neste pacote:
- AuditController
- AuthController
- ClientController
- IndexController
- SecurityDebugController

As regras gerais:
- Validar DTOs com `@Valid` e retornar erros amigáveis.
- Não retornar dados sensíveis (CPF cru, CVV, senhas). Use mappers para mascaramento.
- Preferir injeção por construtor para dependências.
- Proteger endpoints com roles/authorities (`@PreAuthorize`/`@Secured`) quando aplicável.
- Tratar exceções via `GlobalExceptionHandler`.

---

## AuditController.java

Propósito:
- Expor endpoints para consultar logs/auditoria usados por administradores.

Responsabilidades:
- Listar logs por filtros (usuário, intervalo de datas, ação).
- Paginar resultados.
- Fornecer visualizações (HTML) para painéis administrativos.

Métodos/fluxos típicos (conceito):
- GET /admin/auditoria
  - Parâmetros: page, size, usuarioId?, dataInicio?, dataFim?
  - Valida autorização (ROLE_ADMIN).
  - Chama LogAuditoriaService para buscar logs.
  - Converte entidades para DTOs com LogMapper e retorna view/JSON.

Segurança:
- Deve exigir autenticação com role administrativa.
- Evitar expor detalhes sensíveis no campo `detalhes` — já ofuscados pelo serviço de log.

Boas práticas:
- Paginacão e limites por request para evitar sobrecarga.
- Audit logs consultados apenas por usuários com permissão.

---

## AuthController.java

Propósito:
- Endpoints de autenticação: login, logout, cadastro (signup), endpoints de confirmação e recuperação de senha.

Responsabilidades:
- Validar credenciais (`LoginDTO`) e gerar cookie JWT seguro.
- Processar cadastro (`SignupDTO`), validar duplicidade e enviar e-mail de confirmação.
- Processar logout (remover cookie).
- Redirecionar/montar páginas de login/cadastro.

Métodos/fluxos típicos:
- GET /login — exibe formulário de login.
- POST /login — valida `LoginDTO`, autentica via AuthHelper/AuthenticationManager:
  - Verificar usuário e senha com PasswordEncoder.
  - Gerar token JWT com `JwtUtil`.
  - Criar cookie HttpOnly/Secure (quando em produção) e enviar no Response.
- POST /signup — validar `SignupDTO`, criar cliente via ClienteService, enviar e-mail de confirmação.
- GET/POST /recuperar-senha — iniciar fluxo de reset (gerar token, email).
- GET /reset-senha?token=... — validar token, exibir formulário; POST para aplicar nova senha.

Segurança:
- Não logar senha ou token no servidor.
- Cookies devem ser HttpOnly, SameSite=strict/lax conforme necessidade, Secure em HTTPS.
- Limitar tentativas de login e registrar falhas via SecurityLogger.

Validação:
- Usar `@Valid` nos DTOs e checar `BindingResult` para devolver mensagens específicas.

---

## ClientController.java

Propósito:
- Gerenciar páginas e ações do cliente: visualizar/atualizar perfil, gerenciar cartões e endereços, páginas privadas.

Responsabilidades:
- Mostrar homepage do cliente, formulário de edição, listagem de cartões/endereços.
- Atualizar dados do cliente (delegar a ClienteService).
- Adicionar/remover cartões e endereços com validação (Luhn para cartões, validade, formato de CEP).
- Remover conta (verificações adicionais e confirmação).

Métodos/fluxos típicos:
- GET /clientes/homepage — carregar cliente atual (usuário autenticado), buscar via ClienteService, mapear para ClienteDTO (mascarado) e renderizar.
- POST /clientes/atualizar — receber ClienteDTO com `@Valid`, validar e chamar ClienteService.atualizarCliente.
- POST /clientes/cartoes — adicionar cartão; fluxo:
  - validarDadosCartao(dto) — número Luhn, validade (YearMonth), bandeira.
  - Converter validade YearMonth → String antes de persistência (via CartaoMapper.yearMonthToString).
  - Procurar cartão idêntico no repositório para reutilizar ou criar novo.
- POST /clientes/remover — confirmar e remover cliente (soft delete é preferível).

Segurança:
- Verificar que ações afetam apenas o cliente autenticado (comparar principal ID).
- Usar CSRF tokens em forms se não for SPA que usa cookie.

Boas práticas:
- Ao retornar ClienteDTO, mascarar CPF e número do cartão; nunca retornar CVV.
- Fazer operações de escrita dentro de transação.

Uso de Streams/Lambdas:
- Converter listas de entidades para DTOs:
  - clientes.stream().map(ClienteMapper::fromEntity).collect(Collectors.toList())
- Filtrar cartões válidos:
  - cliente.getCartoes().stream()
      .filter(c -> CartaoMapper.stringToYearMonth(c.getValidade()).isAfter(YearMonth.now()))
      .map(CartaoMapper::fromEntity)
      .collect(Collectors.toList());

---

## IndexController.java

Propósito:
- Rotas públicas e página index; redirecionamentos iniciais e recursos públicos.

Responsabilidades:
- Renderizar index.html e páginas públicas (sobre, ajuda).
- Fornecer endpoints públicos como status/health-check (se presente).

Métodos/fluxos típicos:
- GET / — retorna index view.
- GET /status — retorna JSON com estado simples da aplicação (não incluir segredos).

Boas práticas:
- Evitar expor detalhes do ambiente (paths, secrets) em endpoints públicos.

---

## SecurityDebugController.java

Propósito:
- Endpoints para debug de segurança em ambientes controlados (dev/staging).

Responsabilidades:
- Mostrar informações úteis de debugging (roles do usuário, token details) apenas se perfil de execução permitir.
- Utilizado para troubleshooting local; deve estar desativado/seguro em produção.

Segurança:
- Proteger com `@Profile("dev")` e com roles específicas.
- Nunca habilitar em produção.

---

## Padrões e convenções adotadas pelos controllers

- Injeção por construtor (ex.: public AuthController(AuthHelper authHelper) { ... }).
- Validar entrada com `@Valid` e usar BindingResult para mensagens.
- Usar DTOs para entrada e saída; entidades nunca expostas diretamente.
- Converter com mappers (ClienteMapper, CartaoMapper).
- Exceções do serviço são tratadas por `GlobalExceptionHandler` para padronizar respostas.
- Limitar payloads grandes e usar paginação em listagens.
- Documentar endpoints (opcional: Swagger/OpenAPI).

---

## Guia prático: Anotações, Streams e Lambdas (aplicado aos Controllers)

A seguir um resumo prático em português, com exemplos fáceis de aplicar no contexto dos controllers.

### Anotações (Annotations) mais usadas em controllers
- `@Controller` / `@RestController` — define componente Spring MVC para tratar requisições web.
  - `@RestController` = `@Controller` + `@ResponseBody` (retorna JSON por padrão).
- `@RequestMapping`, `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping` — mapeamento de rotas.
  - Ex.: `@PostMapping("/login")`
- `@PathVariable`, `@RequestParam`, `@RequestBody` — parâmetros de rota, query e corpo.
- `@Valid` — ativa validação bean (jakarta/validation) para DTOs.
- `@PreAuthorize("hasRole('ADMIN')")` / `@Secured` — restrição por roles.
- `@ControllerAdvice` / `@ExceptionHandler` — tratamento global de erros.
- `@Autowired` — injeção de dependência (preferir constructor injection em vez de field injection).
- `@Transactional` (em services) — garantir atomicidade em operações de escrita.

Boas práticas:
- Marcar controllers com `@RequestMapping("/clientes")` para agrupar rotas.
- Devolver ResponseEntity<T> para controlar status code e headers.

### Streams (java.util.stream) — uso prático
- Conceito: processar coleções com pipeline declarativo.
- Operadores comuns:
  - filter(Predicate) — filtra elementos.
  - map(Function) — transforma elementos.
  - flatMap(Function) — transforma e achata.
  - collect(Collectors.toList()) — coleta o resultado.
  - findFirst(), anyMatch(), allMatch() — checks/queries.
- Exemplo aplicado (converter lista de clientes em DTOs):
  - Exemplo implícito:
    (apenas texto de exemplo)
      clientes.stream()
          .map(ClienteMapper::fromEntity)
          .collect(Collectors.toList());

- Uso em controllers:
  - Depois de obter uma lista do serviço, use stream para mapear e filtrar antes de retornar ao cliente.

Cuidados:
- Evitar lambdas que lancem checked exceptions diretamente; capture/trate fora da stream.
- Streams paralelos só quando operação é CPU-bound e thread-safe.

### Lambdas e Method References — uso prático
- Lambda simples:
  - `lista.forEach(item -> log.info(item.getNome()));`
- Method reference:
  - `lista.forEach(System.out::println);`
- Em controllers/mappers:
  - `clientes.stream().map(ClienteMapper::fromEntity).collect(Collectors.toList());`

Vantagens:
- Código conciso e legível; facilita transformações entre camadas.

### Exemplo integrado (validação e fluxo de controller)
Fluxo de adicionar cartão no ClientController (conceitual):
1. Recebe `CartaoDTO` via `@PostMapping("/clientes/cartoes") @Valid @RequestBody CartaoDTO dto`.
2. No controller:
   - Verificar `BindingResult` e retornar 400 se inválido.
   - Chamar `clienteService.adicionarCartao(clienteId, dto)`.
3. No service:
   - validarDadosCartao(dto) — Luhn, validade.
   - String validadeStr = CartaoMapper.yearMonthToString(dto.getValidade());
   - Optional<Cartao> existente = cartaoRepository.findByValueFields(dto.getNumero(), dto.getNomeTitular(), validadeStr, dto.getBandeira(), dto.getCpfTitular());
   - Reutilizar ou criar novo, persistir, associar ao cliente (transação).
4. Controller retorna 200/201 com representação mascarada do cartão via CartaoMapper.

---

## Recomendações finais e checklist de segurança para controllers

- Usar DTOs com campos mascarados e validações (`@NotBlank`, `@Email`, `@Pattern`).
- Proteger endpoints sensíveis com `@PreAuthorize`.
- Configurar CORS com whitelist de origens.
- Assegurar cookies JWT com HttpOnly + Secure + SameSite apropriado.
- Implementar rate limiting em endpoints de autenticação/recuperação.
- Registrar eventos críticos com `LogAuditoriaService` sem incluir dados sensíveis.
- Documentar endpoints (OpenAPI/Swagger) para facilitar integração front/backend.

---
