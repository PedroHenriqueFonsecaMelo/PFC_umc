# PFC UMC — Aplicação Backoffice / Cliente (Resumo do Projeto)

Visão geral

- Projeto: PFC UMC — aplicação web em Spring Boot que provê funcionalidades de
  cadastro/gestão de clientes, cartões e endereços, autenticação via JWT,
  auditoria e páginas voltadas ao usuário.
- Objetivo: backend completo para fluxo de clientes (cadastro, login,
  recuperação de senha), administração (audit), segurança (JWT) e integração com
  front-ends via templates Thymeleaf ou APIs REST.

Arquitetura e padrões

- Framework: Spring Boot (MVC, Security, Data JPA).
- Camadas:
  - controller — endpoints HTTP / páginas (Thymeleaf) e pontos de entrada do
    frontend.
  - backstage — configuração, segurança, tratamento global de exceções, serviços
    de infra.
  - model — entidades JPA, DTOs, mappers e repositórios (persistência).
  - resources — templates, assets estáticos e arquivos de configuração por
    profile.
- Padrões adotados: injeção por construtor, DTOs para entrada/saída, mappers
  para conversão e mascaramento, services com @Transactional onde necessário.

Estrutura principal do projeto (resumo)

- src/main/java/umc/exs/
  - Main.java, ServletInitializer.java — inicialização da aplicação.
  - backstage/
    - config: SecurityConfig, WebSocketConfig
    - security: JwtUtil, JwtRequestFilter, JwtUserDetailsService,
      JwtAuthenticationEntryPoint
    - handler: GlobalExceptionHandler
    - log: LogAuditoriaService, SecurityLogger
    - service: AuthHelper, ClienteService, EmailService, FieldValidation
  - controller/prod/
    - AuthController, ClientController, IndexController, AuditController,
      SecurityDebugController
  - model/
    - daos/mappers: CartaoMapper, ClienteMapper, EnderecoMapper
    - daos/repository: ClienteRepository, CartaoRepository, EnderecoRepository,
      AdminRepository, LogAuditoriaRepository, RecuperacaoSenhaRepository
    - dtos: auth, user, interfaces
    - entidades: usuario (Cliente, Cartao, Endereco), foundation (Administrador,
      LogAuditoria, RecuperacaoSenha)
- src/main/resources/
  - application*.properties (profiles: local, gmail, render), templates
    Thymeleaf, static assets, certs.

Requisitos de desenvolvimento (mínimos)

- Java 17+ (recomendado alinhado ao projeto)
- Maven (ou Gradle, conforme build do projeto)
- Banco de dados compatível com JPA/Hibernate (H2 para testes, Postgres/MySQL em
  produção)
- IDE: VS Code / IntelliJ IDEA

Como executar localmente (Windows)

1. Abrir terminal integrado no VS Code na raiz do projeto:
   - mvn clean install
   - mvn spring-boot:run
2. Alternativa: executar a classe Main pela IDE.
3. Profiles:
   - Usar `-Dspring.profiles.active=local` para configurações locais:
     - mvn spring-boot:run -Dspring-boot.run.profiles=local

Comandos úteis

- Compilar: mvn clean package
- Executar testes: mvn test
- Executar jar: java -jar target/<artifact>.jar --spring.profiles.active=local

Observações de configuração

- application-*.properties contém configurações de e-mail, datasource e SMTP.
  Ajuste antes de rodar em produção.
- Certificados/keystores: não comitar arquivos sensíveis em repositórios
  públicos. Use variáveis de ambiente ou service secrets.

Segurança e privacidade (principais cuidados)

- Tokens JWT:
  - Gerados por JwtUtil; armazenar apenas identificador não sensível no subject
    (ex.: userId).
  - Enviar em cookie HttpOnly + Secure (em HTTPS) ou Authorization header
    (Bearer).
- Senhas:
  - Hashear com BCrypt (PasswordEncoder) e nunca armazenar texto claro.
- Cartões & CPF:
  - CVV não deve ser persistido.
  - Números de cartão e CPF devem ser tokenizados/criptografados; ao expor, usar
    mascaramento (ex.: **** **** **** 1234).
- Logs:
  - Auditoria via LogAuditoriaService — gravar referências, não dados sensíveis.
- CORS:
  - Configurar origem permitida estritamente em produção.

Documentação do código e arquivos de apoio

- Docs consolidados estão na pasta `docs/`:
  - docs/backstage_DOCUMENTACAO.md — documentação do pacote backstage.
  - docs/controller_DOCUMENTACAO.md — documentação dos controllers.
  - docs/model_DOCUMENTACAO.md — documentação do modelo (entidades, mappers,
    DTOs).
  - docs/DOCUMENTACAO_CONSOLIDADA.md, docs/CONCEITOS_JAVA_AVANCADO.md etc.
    (arquivos maiores e consolidados).
- Para gerar documentação detalhada método-a-método, há documentos separados por
  pacote dentro de docs. Atualize-os conforme altera o código.

Boas práticas de desenvolvimento (diretrizes)

- Preferir constructor injection para beans Spring.
- Validar DTOs com `@Valid` e anotações de jakarta/validation.
- Usar mappers (ex.: CartaoMapper) para conversão DTO ↔ Entity; sempre
  sanitizar/formatar antes de persistir (ex.: YearMonth → String).
- Tratar exceções centralmente com GlobalExceptionHandler e retornar mensagens
  genéricas ao cliente.
- Escrever testes unitários para mappers, validações e serviços críticos; testes
  de integração (@DataJpaTest) para repositórios.

Como contribuir / workflow

- Criar branch por tarefa: feature/<descrição> ou fix/<descrição>
- Rodar testes localmente antes de push.
- Abrir Pull Request com descrição das mudanças, riscos e instruções de deploy.
- Revisão de segurança obrigatória para mudanças que toquem autenticação,
  tokens, armazenamento de dados sensíveis.

Checklist antes de deploy

- Remover/ocultar arquivos de certificado e secrets do repositório.
- Confirmar políticas de CORS e secure cookies.
- Ajustar propriedades de e-mail e datasource para ambiente de produção.
- Executar battery de testes automáticos e validação manual de fluxos críticos
  (login, recuperação de senha, cadastro).

Links rápidos

- Código-fonte: src/main/java/umc/exs
- Templates: src/main/resources/templates
- Documentação do workspace: docs/
- Executar local (Windows): mvn spring-boot:run -Dspring-boot.run.profiles=local

Contatos e manutenção

- Mantido por: equipe do projeto (adicionar emails ou canais internos conforme
  política interna).
- Atualização dos docs: manter docs/ sincronizado sempre que houver alterações
  nas APIs, DTOs ou regras de segurança.

Licença

- Defina a licença do projeto no arquivo LICENSE na raiz (ex.: MIT, Apache-2.0)
  conforme política da organização.
