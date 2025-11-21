package umc.exs.backstage.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import umc.exs.model.daos.mappers.CartaoMapper;
import umc.exs.model.daos.mappers.ClienteMapper;
import umc.exs.model.daos.mappers.EnderecoMapper;
import umc.exs.model.daos.repository.CartaoRepository;
import umc.exs.model.daos.repository.ClienteRepository;
import umc.exs.model.daos.repository.EnderecoRepository;
import umc.exs.model.daos.repository.RecuperacaoSenhaRepository;
import umc.exs.model.dtos.auth.SignupDTO;
import umc.exs.model.dtos.user.CartaoDTO;
import umc.exs.model.dtos.user.ClienteDTO;
import umc.exs.model.dtos.user.EnderecoDTO;
import umc.exs.model.entidades.foundation.RecuperacaoSenha;
import umc.exs.model.entidades.foundation.enums.Genero;
import umc.exs.model.entidades.usuario.Cartao;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.entidades.usuario.Endereco;

@Service
public class ClienteService {

    @Value("${app.base-url}")
    private String baseUrl;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private CartaoRepository cartaoRepository;

    @Autowired
    private EnderecoRepository enderecoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RecuperacaoSenhaRepository recuperacaoSenhaRepository;

    @Autowired
    private EmailService emailService;

    /** 
     * @param signupDTO
     * @return ClienteDTO
     */
    // ==========================================================
    // 🔹 SALVAR (CADASTRO)
    // ==========================================================

    @Transactional
    public ClienteDTO salvarCliente(SignupDTO signupDTO) {
        validarDadosSignup(signupDTO);

        Cliente cliente = ClienteMapper.toEntity(signupDTO);
        cliente.setSenha(passwordEncoder.encode(signupDTO.getSenha()));

        Cliente salvo = clienteRepository.save(cliente);
        return ClienteMapper.fromEntity(salvo);
    }

    /** 
     * @param signupDTO
     * @param enderecoDTO
     * @param cartaoDTO
     * @return ClienteDTO
     */
    @Transactional
    public ClienteDTO salvarClienteCompleto(SignupDTO signupDTO, EnderecoDTO enderecoDTO, CartaoDTO cartaoDTO) {
        validarDadosSignup(signupDTO);
        validarDadosEndereco(enderecoDTO);
        validarDadosCartao(cartaoDTO);

        // 1. Mapeamento e Hash da Senha
        Cliente cliente = ClienteMapper.toEntity(signupDTO);
        cliente.setSenha(passwordEncoder.encode(signupDTO.getSenha()));

        // 2. Associações Endereço
        // Salva o endereço primeiro para que ele não seja Transient
        Endereco endereco = EnderecoMapper.toEntity(enderecoDTO);
        endereco = enderecoRepository.save(endereco); // CRÍTICO: Persiste a entidade

        endereco.getClientes().add(cliente);
        cliente.getEnderecos().add(endereco);

        // 3. Associações Cartão
        // Salva o cartão primeiro para que ele não seja Transient
        Cartao cartao = CartaoMapper.toEntity(cartaoDTO);
        cartao = cartaoRepository.save(cartao); // CRÍTICO: Persiste a entidade

        cartao.getClientes().add(cliente);
        cliente.getCartoes().add(cartao);

        // 4. Persistência
        Cliente salvo = clienteRepository.save(cliente);
        return ClienteMapper.fromEntity(salvo);
    }

    /** 
     * @param clienteId
     * @param clienteAtualizadoDTO
     * @return ClienteDTO
     */
    // ==========================================================
    // 💾 ATUALIZAR
    // ==========================================================

    @Transactional
    public ClienteDTO atualizarClienteEAssociacoes(Long clienteId, ClienteDTO clienteAtualizadoDTO) {

        Cliente clienteExistente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado."));

        // 1. Validação e Sanitização dos Dados Básicos
        validarDadosAtualizacao(clienteAtualizadoDTO);

        // 2. Atualiza Dados Básicos
        clienteExistente.setNome(FieldValidation.sanitize(clienteAtualizadoDTO.getNome()));
        clienteExistente.setDatanasc(clienteAtualizadoDTO.getDatanasc());

        // Atualiza Gênero
        String generoStr = clienteAtualizadoDTO.getGen();
        if (generoStr != null && !generoStr.trim().isEmpty()) {
            try {
                Genero novoGenero = Genero.valueOf(generoStr.toUpperCase());
                clienteExistente.setGen(novoGenero);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Valor de gênero inválido. Use M, F ou OUTRO.");
            }
        }

        // TRATAMENTO DA SENHA: Se a senha for fornecida, ela deve ser hasheada
        if (clienteAtualizadoDTO.getSenha() != null && !clienteAtualizadoDTO.getSenha().trim().isEmpty()) {
            if (!FieldValidation.isValidPassword(clienteAtualizadoDTO.getSenha())) {
                throw new IllegalArgumentException("Nova senha não atende aos requisitos de segurança.");
            }
            clienteExistente.setSenha(passwordEncoder.encode(clienteAtualizadoDTO.getSenha()));
        }

        // --- 3. Atualiza Associações (Endereços) ---

        // 3.1. Identifica e Deleta Endereços Removidos
        Set<Long> idsRecebidos = clienteAtualizadoDTO.getEnderecos().stream()
                .map(EnderecoDTO::getId)
                .filter(id -> id != null && id != 0)
                .collect(Collectors.toSet());

        Set<Endereco> enderecosParaRemover = clienteExistente.getEnderecos().stream()
                .filter(e -> !idsRecebidos.contains(e.getId()))
                .collect(Collectors.toSet());

        // Deleta (remove o relacionamento e a entidade se não tiver mais clientes)
        enderecosParaRemover.forEach(e -> deletarEnderecoDoCliente(clienteId, e.getId()));

        // 3.2. Atualiza/Cria/Reutiliza Endereços Recebidos
        Set<Endereco> enderecosAtualizados = new HashSet<>();

        for (EnderecoDTO dto : clienteAtualizadoDTO.getEnderecos()) {
            this.validarDadosEndereco(dto);
            Endereco endereco;

            if (dto.getId() != null && dto.getId() != 0) {
                // Endereço existente: carrega, atualiza e persiste
                endereco = enderecoRepository.findById(dto.getId())
                        .orElseThrow(
                                () -> new IllegalArgumentException("Endereço ID " + dto.getId() + " não encontrado."));

                // Assumindo que EnderecoMapper.updateEntityFromDto existe
                endereco = EnderecoMapper.updateEntityFromDto(endereco, dto);
                endereco = enderecoRepository.save(endereco); // Persiste a atualização

            } else {
                // Endereço NOVO (ID nulo/0): Busca por reutilização ou Cria

                // 🔑 Tenta encontrar um endereço idêntico no banco
                Optional<Endereco> enderecoReutilizado = enderecoRepository.findByValueFields(
                        dto.getCep(),
                        dto.getRua(),
                        dto.getNumero(),
                        dto.getComplemento(),
                        dto.getBairro(),
                        dto.getCidade(),
                        dto.getEstado());

                if (enderecoReutilizado.isPresent()) {
                    // Endereço idêntico encontrado: REUTILIZA
                    endereco = enderecoReutilizado.get();
                } else {
                    // Endereço não encontrado: CRIA NOVO
                    endereco = EnderecoMapper.toEntity(dto);
                    endereco = enderecoRepository.save(endereco); // Persiste o novo objeto para ter um ID
                }

                // Adiciona o relacionamento (afeta apenas a tabela de junção se já não existir)
                endereco.getClientes().add(clienteExistente);
            }
            enderecosAtualizados.add(endereco);
        }

        // Define a nova coleção (essencial para persistir o relacionamento na tabela de
        // junção)
        clienteExistente.setEnderecos(enderecosAtualizados);

        // --- 4. Atualiza Associações (Cartões) - LÓGICA REFATORADA ---

        // 4.1. Identifica e Deleta Cartões Removidos (Omitidos no DTO)
        Set<Long> cartoesIdsRecebidos = clienteAtualizadoDTO.getCartoes().stream()
                .map(CartaoDTO::getId)
                .filter(id -> id != null && id != 0)
                .collect(Collectors.toSet());

        Set<Cartao> cartoesParaRemover = clienteExistente.getCartoes().stream()
                .filter(c -> !cartoesIdsRecebidos.contains(c.getId()))
                .collect(Collectors.toSet());

        // Deleta (remove o relacionamento e a entidade se não tiver mais clientes)
        cartoesParaRemover.forEach(c -> deletarCartaoDoCliente(clienteId, c.getId()));

        // 4.2. Atualiza/Cria/Reutiliza Cartões Recebidos (Simétrico ao Endereço)
        Set<Cartao> cartoesAtualizados = new HashSet<>();

        for (CartaoDTO dto : clienteAtualizadoDTO.getCartoes()) {
            this.validarDadosCartao(dto);
            Cartao cartao;

            if (dto.getId() != null && dto.getId() != 0) {
                // Cartão existente: carrega (geralmente não se edita, mas precisa carregar)
                cartao = cartaoRepository.findById(dto.getId())
                        .orElseThrow(
                                () -> new IllegalArgumentException("Cartão ID " + dto.getId() + " não encontrado."));

                // Aqui seria o ponto para CartaoMapper.updateEntityFromDto se edição fosse
                // permitida/necessária.
            } else {
                // Cartão NOVO (ID nulo/0): Busca por reutilização ou Cria

                // ASSUMIR MÉTODO EXISTENTE: Tenta encontrar um cartão idêntico no banco
                // converter validade (YearMonth) para String antes de consultar o repositório
                String validadeStr = CartaoMapper.yearMonthToString(dto.getValidade());
                Optional<Cartao> cartaoReutilizado = cartaoRepository.findByValueFields(
                        dto.getNumero(),
                        dto.getNomeTitular(),
                        validadeStr,
                        dto.getBandeira(),
                        dto.getCpfTitular());

                if (cartaoReutilizado.isPresent()) {
                    // Cartão idêntico encontrado: REUTILIZA
                    cartao = cartaoReutilizado.get();
                } else {
                    // Cartão não encontrado: CRIA NOVO
                    cartao = CartaoMapper.toEntity(dto);
                    cartao = cartaoRepository.save(cartao); // Persiste o novo objeto
                }

                // Adiciona o relacionamento (afeta apenas a tabela de junção se já não existir)
                cartao.getClientes().add(clienteExistente);
            }
            cartoesAtualizados.add(cartao);
        }

        // Define a nova coleção
        clienteExistente.setCartoes(cartoesAtualizados);

        // 5. Salva o Cliente
        // Persiste todas as mudanças feitas na entidade 'clienteExistente', incluindo
        // as atualizações na tabela de junção (Join Table) dos Endereços e Cartões.
        Cliente salvo = clienteRepository.save(clienteExistente);

        return ClienteMapper.fromEntity(salvo);
    }

    /** 
     * @param clienteId
     * @param enderecoId
     */
    // ==========================================================
    // 🗑️ MÉTODOS DE DELEÇÃO (M2M)
    // ==========================================================

    @Transactional
    public void deletarEnderecoDoCliente(Long clienteId, Long enderecoId) {
        // ⚠️ MUDANÇA AQUI: Usa a query customizada para carregar a lista de endereços
        Cliente cliente = clienteRepository.findByIdWithEnderecos(clienteId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado."));

        // O restante da lógica agora tem a garantia de que a lista de endereços está
        // carregada
        Endereco endereco = cliente.getEnderecos().stream()
                .filter(e -> e.getId().equals(enderecoId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Endereço não encontrado ou não pertence ao cliente."));

        // Remove o relacionamento (limpa a tabela de junção)
        cliente.getEnderecos().remove(endereco);
        endereco.getClientes().remove(cliente);

        // Salva o cliente para persistir a remoção da tabela de junção
        clienteRepository.save(cliente);

        // Deleta o Endereço se ele não estiver mais em uso
        if (endereco.getClientes().isEmpty()) {
            enderecoRepository.delete(endereco);
        }
    }

    /** 
     * @param clienteId
     * @param cartaoId
     */
    @Transactional
    public void deletarCartaoDoCliente(Long clienteId, Long cartaoId) {
        // ⚠️ MUDANÇA AQUI: usando o método personalizado que força o JOIN FETCH
        Cliente cliente = clienteRepository.findByIdWithCartoes(clienteId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado."));

        // O restante do seu código está correto para um relacionamento bidirecional:
        Cartao cartao = cliente.getCartoes().stream()
                .filter(c -> c.getId().equals(cartaoId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Cartão não encontrado ou não pertence ao cliente."));

        // Desassocia de ambos os lados
        cliente.getCartoes().remove(cartao);
        cartao.getClientes().remove(cliente);

        clienteRepository.save(cliente);

        // Deleta o Cartão se ele não estiver mais em uso
        if (cartao.getClientes().isEmpty()) {
            cartaoRepository.delete(cartao);
        }
    }

    /** 
     * @param clienteId
     */
    @Transactional
    public void deletarClientePorId(Long clienteId) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado para deleção."));

        // LIMPEZA DOS RELACIONAMENTOS MANY-TO-MANY (CRUCIAL)
        cliente.getEnderecos().forEach(e -> e.getClientes().remove(cliente));
        cliente.getEnderecos().clear();

        cliente.getCartoes().forEach(c -> c.getClientes().remove(cliente));
        cliente.getCartoes().clear();

        clienteRepository.save(cliente);

        // Deleta a entidade Cliente
        clienteRepository.delete(cliente);
    }

    /** 
     * @param email
     * @param senha
     * @return Optional<ClienteDTO>
     */
    // ==========================================================
    // 🔒 AUTENTICAÇÃO E BUSCA
    // ==========================================================

    public Optional<ClienteDTO> autenticarCliente(String email, String senha) {
        // Uso do sanitizeEmail para garantir que o email buscado atenda à regra
        String safeEmail;
        try {
            safeEmail = FieldValidation.sanitizeEmail(email);
        } catch (IllegalArgumentException e) {
            return Optional.empty(); // Email inválido
        }

        Optional<Cliente> clienteOpt = clienteRepository.findByEmail(safeEmail);

        if (clienteOpt.isEmpty()) {
            return Optional.empty();
        }

        Cliente cliente = clienteOpt.get();

        if (passwordEncoder.matches(senha, cliente.getSenha())) {
            return Optional.of(ClienteMapper.fromEntity(cliente));
        } else {
            return Optional.empty();
        }
    }

    /** 
     * @param email
     * @return Optional<ClienteDTO>
     */
    public Optional<ClienteDTO> buscarClientePorEmail(String email) {
        String safeEmail;
        try {
            safeEmail = FieldValidation.sanitizeEmail(email);
        } catch (IllegalArgumentException e) {
            return Optional.empty(); // Email inválido
        }

        return clienteRepository.findByEmail(safeEmail)
                .map(ClienteMapper::fromEntity);
    }

    /** 
     * @param clienteId
     * @return Optional<ClienteDTO>
     */
    public Optional<ClienteDTO> buscarClientePorId(Long clienteId) {
        return clienteRepository.findById(clienteId)
                .map(ClienteMapper::fromEntity);
    }

    /** 
     * @param dto
     */
    // ==========================================================
    // 🔒 MÉTODOS DE VALIDAÇÃO PRIVADOS (Com uso de FieldValidation)
    // ==========================================================

    private void validarDadosSignup(SignupDTO dto) {
        if (!FieldValidation.validarCampos(dto)) {
            throw new IllegalArgumentException("Todos os campos obrigatórios do cadastro devem ser preenchidos.");
        }

        // 1. Email (USO DO NOVO sanitizeEmail)
        String safeEmail;
        try {
            safeEmail = FieldValidation.sanitizeEmail(dto.getEmail());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Email incorreto. Caracteres especiais (exceto @, . e _) não permitidos.");
        }

        if (clienteRepository.findByEmail(safeEmail).isPresent()) {
            throw new IllegalArgumentException("Email já cadastrado.");
        }
        dto.setEmail(safeEmail);

        // 2. CPF e Validação Anti-SQLi
        if (!FieldValidation.isSafe(dto.getCpf()) || !FieldValidation.isValidCPF(dto.getCpf())) {
            throw new IllegalArgumentException("CPF inválido ou com padrões de segurança inaceitáveis.");
        }

        // 3. Data de Nascimento e Maioridade
        LocalDate dataNascimento = FieldValidation.isValidBirthDate(dto.getDatanasc());
        if (dataNascimento == null || !FieldValidation.isOver18(dataNascimento)) {
            throw new IllegalArgumentException("Data de nascimento inválida ou menor de 18 anos.");
        }

        // 4. Senha
        if (!FieldValidation.isValidPassword(dto.getSenha())) {
            throw new IllegalArgumentException(
                    "A senha deve ter no mínimo 8 caracteres, letras maiúsculas/minúsculas, números e símbolos.");
        }

        // 5. Sanitização de Nome e Gênero (e validação Anti-SQLi para texto livre)
        if (!FieldValidation.isSafe(dto.getNome())) {
            throw new IllegalArgumentException("Nome contém caracteres de segurança inaceitáveis.");
        }
        dto.setNome(FieldValidation.sanitize(dto.getNome()));
        dto.setGen(FieldValidation.sanitize(dto.getGen()));
    }

    /** 
     * @param dto
     */
    private void validarDadosAtualizacao(ClienteDTO dto) {
        // Validação Anti-SQLi para o Nome (dados editáveis)
        if (dto.getNome() != null && !FieldValidation.isSafe(dto.getNome())) {
            throw new IllegalArgumentException("Nome contém caracteres de segurança inaceitáveis.");
        }

        // Validação de Data de Nascimento e Maioridade
        if (dto.getDatanasc() != null) {
            LocalDate dataNascimento = FieldValidation.isValidBirthDate(dto.getDatanasc());
            if (dataNascimento == null || !FieldValidation.isOver18(dataNascimento)) {
                throw new IllegalArgumentException("Data de nascimento inválida ou menor de 18 anos.");
            }
        }

        // Sanitização de Nome e Gênero
        if (dto.getNome() != null)
            dto.setNome(FieldValidation.sanitize(dto.getNome()));
        if (dto.getGen() != null)
            dto.setGen(FieldValidation.sanitize(dto.getGen()));
    }

    /** 
     * @param dto
     */
    private void validarDadosEndereco(EnderecoDTO dto) {
        if (!FieldValidation.validarCampos(dto)) {
            throw new IllegalArgumentException("Todos os campos obrigatórios do endereço devem ser preenchidos.");
        }

        // Validação Anti-SQLi para todos os campos de Endereço (texto livre)
        if (!FieldValidation.isSafe(dto.getRua()) ||
                !FieldValidation.isSafe(dto.getBairro()) ||
                !FieldValidation.isSafe(dto.getCidade()) ||
                !FieldValidation.isSafe(dto.getComplemento())) {
            throw new IllegalArgumentException("Dados de Endereço contêm padrões de segurança inaceitáveis.");
        }

        if (!FieldValidation.isValidCEP(dto.getCep())) {
            throw new IllegalArgumentException("CEP inválido.");
        }

        // Sanitização de Strings
        dto.setRua(FieldValidation.sanitize(dto.getRua()));
        dto.setBairro(FieldValidation.sanitize(dto.getBairro()));
        dto.setCidade(FieldValidation.sanitize(dto.getCidade()));
        dto.setEstado(FieldValidation.sanitize(dto.getEstado()));
        dto.setComplemento(FieldValidation.sanitize(dto.getComplemento()));
    }

    /** 
     * @param dto
     */
    private void validarDadosCartao(CartaoDTO dto) {
        if (!FieldValidation.validarCampos(dto)) {
            throw new IllegalArgumentException("Todos os campos obrigatórios do cartão devem ser preenchidos.");
        }

        // Validação Anti-SQLi para o nome do titular
        if (!FieldValidation.isSafe(dto.getNomeTitular())) {
            throw new IllegalArgumentException("Nome do Titular contém padrões de segurança inaceitáveis.");
        }

        // Validade do Cartão (YearMonth)
        if (!FieldValidation.isValidCardExpiry(dto.getValidade())) {
            throw new IllegalArgumentException("A data de validade do cartão é inválida ou expirou.");
        }

        // Sanitização de Nome e CPF
        dto.setNomeTitular(FieldValidation.sanitize(dto.getNomeTitular()));
    }

    /** 
     * @param email
     */
    // ==========================================================
    // 🔒 MÉTODOS DE EMAIL
    // ==========================================================

    @Transactional
    public void iniciarRecuperacaoSenha(String email) {

        Cliente cliente = clienteRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Email não encontrado."));

        recuperacaoSenhaRepository.deleteByCliente(cliente);

        String token = gerarTokenManual();
        LocalDateTime validade = LocalDateTime.now().plusMinutes(30);

        RecuperacaoSenha rec = new RecuperacaoSenha(token, cliente, validade);
        rec.setEmail(email);
        recuperacaoSenhaRepository.save(rec);

        String link = baseUrl + "/clientes/reset-senha?token=" + token;

        System.out.println("Seu link de recuperação: " + link);


        String assunto = "Recuperação de Senha";
        String texto = "Olá,\n\nClique no link abaixo para redefinir sua senha:\n\n" +
                link + "\n\nSe você não solicitou, apenas ignore este e-mail.";

        emailService.enviar(email, assunto, texto);
    }

    /** 
     * @return String
     */
    private String gerarTokenManual() {
        // Token UUID tradicional e sem traços
        return java.util.UUID.randomUUID().toString().replace("-", "");
    }

    /** 
     * @param token
     * @return boolean
     */
    @Transactional
    public boolean validarTokenRecuperacao(String token) {

        Optional<RecuperacaoSenha> opt = recuperacaoSenhaRepository.findByToken(token);

        if (opt.isEmpty()) {
            return false;
        }

        RecuperacaoSenha rec = opt.get();

        // Verifica se ainda não expirou
        return LocalDateTime.now().isBefore(rec.getDataExpiracao());
    }

    /** 
     * @param token
     * @param novaSenha
     * @return String
     */
    @Transactional
    public String alterarSenhaComToken(String token, String novaSenha) {

        RecuperacaoSenha rec = recuperacaoSenhaRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token inválido ou expirado."));

        // Confirma expiração
        if (LocalDateTime.now().isAfter(rec.getDataExpiracao())) {
            throw new IllegalArgumentException("Token expirado.");
        }

        // Pega o cliente associado
        Cliente cliente = rec.getCliente();

        // Troca da senha (usando encoder se você usa)
        cliente.setSenha(passwordEncoder.encode(novaSenha));
        clienteRepository.save(cliente);

        // O token só pode ser usado uma vez
        recuperacaoSenhaRepository.delete(rec);

        return cliente.getEmail();
    }

}