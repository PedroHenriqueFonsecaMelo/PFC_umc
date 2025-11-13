package umc.exs.backstage.service;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import umc.exs.model.daos.mappers.CartaoMapper;
import umc.exs.model.daos.mappers.ClienteMapper;
import umc.exs.model.daos.mappers.EnderecoMapper;
import umc.exs.model.daos.repository.CartaoRepository;
import umc.exs.model.daos.repository.ClienteRepository;
import umc.exs.model.daos.repository.EnderecoRepository;
import umc.exs.model.dtos.auth.SignupDTO;
import umc.exs.model.dtos.user.CartaoDTO;
import umc.exs.model.dtos.user.ClienteDTO;
import umc.exs.model.dtos.user.EnderecoDTO;
import umc.exs.model.entidades.foundation.enums.Genero;
import umc.exs.model.entidades.usuario.Cartao;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.entidades.usuario.Endereco;

@Service
public class ClienteService {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private CartaoRepository cartaoRepository;

    @Autowired
    private EnderecoRepository enderecoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

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

    // ==========================================================
    // 💾 ATUALIZAR
    // ==========================================================

    @Transactional
    public ClienteDTO atualizarClienteEAssociacoes(Long clienteId, ClienteDTO clienteAtualizadoDTO) {

        Cliente clienteExistente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado."));

        // 1. Validação e Sanitização dos Dados Básicos
        validarDadosAtualizacao(clienteAtualizadoDTO);

        // 2. Atualiza Dados Básicos (Nome, DataNasc, Gênero)
        clienteExistente.setNome(FieldValidation.sanitize(clienteAtualizadoDTO.getNome()));
        clienteExistente.setDatanasc(clienteAtualizadoDTO.getDatanasc());

        // ** GARANTIA DE IMUTABILIDADE **
        // CPF e Email do cliente NUNCA podem ser alterados após o cadastro.
        // O clienteExistente.setCpf() e setEmail() NÃO DEVEM ser chamados aqui.

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

        // 3. Atualiza Associações (Endereços) - PERMITIDO ATUALIZAR/ADICIONAR/DELETAR

        // Conjunto de IDs de endereços existentes que VÊM no DTO
        Set<Long> idsRecebidos = clienteAtualizadoDTO.getEnderecos().stream()
                .map(EnderecoDTO::getId)
                .filter(id -> id != null && id != 0)
                .collect(Collectors.toSet());

        // Identifica e deleta endereços que existiam, mas foram removidos no formulário
        Set<Endereco> enderecosParaRemover = clienteExistente.getEnderecos().stream()
                .filter(e -> !idsRecebidos.contains(e.getId()))
                .collect(Collectors.toSet());

        // Deleta (remove o relacionamento e a entidade se não tiver mais clientes)
        enderecosParaRemover.forEach(e -> deletarEnderecoDoCliente(clienteId, e.getId()));

        // Atualiza/Cria os endereços restantes
        Set<Endereco> enderecosAtualizados = new HashSet<>();
        for (EnderecoDTO dto : clienteAtualizadoDTO.getEnderecos()) {
            this.validarDadosEndereco(dto);
            Endereco endereco;

            if (dto.getId() != null && dto.getId() != 0) {
                // Endereço existente: carrega, atualiza e persiste
                endereco = enderecoRepository.findById(dto.getId())
                        .orElseThrow(
                                () -> new IllegalArgumentException("Endereço ID " + dto.getId() + " não encontrado."));

                endereco = EnderecoMapper.updateEntityFromDto(endereco, dto);
                endereco = enderecoRepository.save(endereco); // Persiste a atualização

            } else {
                // Endereço NOVO: cria e persiste individualmente
                endereco = EnderecoMapper.toEntity(dto);
                endereco = enderecoRepository.save(endereco); // CORREÇÃO CRÍTICA: Persiste o novo objeto

                endereco.getClientes().add(clienteExistente); // Adiciona relacionamento
            }
            enderecosAtualizados.add(endereco);
        }
        // É importante setar a coleção para garantir que o Hibernate a gerencie
        // corretamente
        clienteExistente.setEnderecos(enderecosAtualizados);

        // 4. Atualiza Associações (Cartões) - APENAS ADICIONA NOVOS, NÃO PERMITE EDIÇÃO
        // DE EXISTENTES

        Set<Cartao> cartoesExistentes = clienteExistente.getCartoes();

        for (CartaoDTO dto : clienteAtualizadoDTO.getCartoes()) {

            // Se o ID é nulo/0, é um novo cartão que precisa ser persistido
            if (dto.getId() == null || dto.getId() == 0) {

                this.validarDadosCartao(dto);
                Cartao novoCartao = CartaoMapper.toEntity(dto);

                // CORREÇÃO CRÍTICA: Salva o novo cartão primeiro para obter um ID persistente
                novoCartao = cartaoRepository.save(novoCartao);

                novoCartao.getClientes().add(clienteExistente);
                cartoesExistentes.add(novoCartao);
            }
            // Se o ID existe, o cartão não é editado, apenas mantido na coleção.
        }

        Cliente salvo = clienteRepository.save(clienteExistente);
        return ClienteMapper.fromEntity(salvo);
    }

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

    public Optional<ClienteDTO> buscarClientePorId(Long clienteId) {
        return clienteRepository.findById(clienteId)
                .map(ClienteMapper::fromEntity);
    }

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
}