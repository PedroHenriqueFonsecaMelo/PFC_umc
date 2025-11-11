package umc.exs.backstage.service;

import java.time.LocalDate;
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
import umc.exs.model.daos.repository.ClienteRepository;
import umc.exs.model.dtos.auth.SignupDTO;
import umc.exs.model.dtos.user.CartaoDTO;
import umc.exs.model.dtos.user.ClienteDTO;
import umc.exs.model.dtos.user.EnderecoDTO;
import umc.exs.model.entidades.foundation.enums.Genero;
import umc.exs.model.entidades.usuario.Cartao;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.entidades.usuario.Endereco;

@Service // <-- ESTA ANOTAÇÃO É CRUCIAL
public class ClienteService {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ==========================================================
    // 🔹 SALVAR (CADASTRO)
    // ==========================================================

    @Transactional
    public ClienteDTO salvarCliente(SignupDTO signupDTO) {
        validarDadosSignup(signupDTO);

        // 1. Mapeamento
        Cliente cliente = ClienteMapper.toEntity(signupDTO);

        // 2. Hash da Senha (CRÍTICO)
        cliente.setSenha(passwordEncoder.encode(signupDTO.getSenha()));

        // 3. Persistência
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

        // 2. Associações (Mapper converte DTO para Entidade)
        Endereco endereco = EnderecoMapper.toEntity(enderecoDTO);
        endereco.addCliente(cliente);
        cliente.getEnderecos().add(endereco);

        Cartao cartao = CartaoMapper.toEntity(cartaoDTO);
        cartao.addCliente(cliente);

        // Criptografia/Tokenização do cartão deve ser tratada aqui.

        cliente.getCartoes().add(cartao);

        // 3. Persistência (cascade deve salvar associações)
        Cliente salvo = clienteRepository.save(cliente);
        return ClienteMapper.fromEntity(salvo);
    }

    // ==========================================================
    // 🔒 AUTENTICAÇÃO
    // ==========================================================

    public Optional<ClienteDTO> autenticarCliente(String email, String senha) {
        // Sanitiza o email antes de buscar
        String safeEmail = FieldValidation.sanitize(email);
        if (!FieldValidation.isValidEmail(safeEmail)) {
            return Optional.empty();
        }

        Optional<Cliente> clienteOpt = clienteRepository.findByEmail(safeEmail);

        if (clienteOpt.isEmpty()) {
            // Cliente não encontrado
            return Optional.empty();
        }

        Cliente cliente = clienteOpt.get();

        // Verifica a senha (compara hash no DB com senha crua do input)
        if (passwordEncoder.matches(senha, cliente.getSenha())) {
            return Optional.of(ClienteMapper.fromEntity(cliente));
        } else {
            // Senha incorreta
            return Optional.empty();
        }
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

        // 2. Atualiza Dados Básicos
        clienteExistente.setNome(FieldValidation.sanitize(clienteAtualizadoDTO.getNome()));
        clienteExistente.setDatanasc(clienteAtualizadoDTO.getDatanasc());
        String generoStr = clienteAtualizadoDTO.getGen();

        if (generoStr != null && !generoStr.trim().isEmpty()) {
            try {
                // 1. Converte a String (do DTO) para a Enum Genero (da Entidade).
                // A conversão falhará (lançará IllegalArgumentException) se o valor não for M,
                // F ou OUTRO.
                Genero novoGenero = Genero.valueOf(generoStr.toUpperCase());

                clienteExistente.setGen(novoGenero); // Atribui a Enum validada

            } catch (IllegalArgumentException e) {
                // Captura o erro da conversão e o relança como erro de validação
                throw new IllegalArgumentException("Valor de gênero inválido. Use M, F ou Outro.");
            }
        }
        // TRATAMENTO DA SENHA: Se a senha for fornecida, ela deve ser hasheada
        if (clienteAtualizadoDTO.getSenha() != null && !clienteAtualizadoDTO.getSenha().trim().isEmpty()) {
            if (!FieldValidation.isValidPassword(clienteAtualizadoDTO.getSenha())) {
                throw new IllegalArgumentException("Nova senha não atende aos requisitos de segurança.");
            }
            clienteExistente.setSenha(passwordEncoder.encode(clienteAtualizadoDTO.getSenha()));
        }

        // 3. Atualiza Associações (Endereços)
        Set<Endereco> novosEnderecos = clienteAtualizadoDTO.getEnderecos().stream()
                .peek(this::validarDadosEndereco) // Valida cada endereço
                .map(EnderecoMapper::toEntity)
                .collect(Collectors.toSet());

        // Garante a referência bidirecional
        novosEnderecos.forEach(e -> e.addCliente(clienteExistente));
        clienteExistente.setEnderecos(novosEnderecos);

        // 4. Atualiza Associações (Cartões)
        Set<Cartao> novosCartoes = clienteAtualizadoDTO.getCartoes().stream()
                .peek(this::validarDadosCartao) // Valida cada cartão
                .map(CartaoMapper::toEntity)
                .collect(Collectors.toSet());

        // Garante a referência bidirecional
        novosCartoes.forEach(c -> c.addCliente(clienteExistente));
        clienteExistente.setCartoes(novosCartoes);

        Cliente salvo = clienteRepository.save(clienteExistente);
        return ClienteMapper.fromEntity(salvo);
    }

    // ==========================================================
    // 🔒 MÉTODOS DE VALIDAÇÃO PRIVADOS
    // ==========================================================

    private void validarDadosSignup(SignupDTO dto) {
        // Validação de Integridade
        if (!FieldValidation.validarCampos(dto)) {
            throw new IllegalArgumentException("Todos os campos obrigatórios do cadastro devem ser preenchidos.");
        }

        // 1. Email
        String safeEmail = FieldValidation.sanitizeEmail(dto.getEmail());
        if (clienteRepository.findByEmail(safeEmail).isPresent()) {
            throw new IllegalArgumentException("Email já cadastrado.");
        }
        dto.setEmail(safeEmail); // Atualiza o DTO com o email sanitizado

        // 2. CPF
        if (!FieldValidation.isValidCPF(dto.getCpf())) {
            throw new IllegalArgumentException("CPF inválido.");
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

        // 5. Sanitização de Nome (e outros campos String simples)
        dto.setNome(FieldValidation.sanitize(dto.getNome()));
        dto.setGen(FieldValidation.sanitize(dto.getGen()));
    }

    private void validarDadosAtualizacao(ClienteDTO dto) {
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
        System.out.println("Cartao: " + dto.toString());
        
        if (!FieldValidation.validarCampos(dto)) {
            throw new IllegalArgumentException("Todos os campos obrigatórios do cartão devem ser preenchidos.");
        }

        // 1. Validade do Cartão (YearMonth)
        if (!FieldValidation.isValidCardExpiry(dto.getValidade())) {
            throw new IllegalArgumentException("A data de validade do cartão é inválida ou expirou.");
        }

        // 2. Sanitização de Nome e CPF
        dto.setNomeTitular(FieldValidation.sanitize(dto.getNomeTitular()));
    }

    // ==========================================================
    // 🔎 MÉTODOS DE BUSCA
    // ==========================================================

    public Optional<ClienteDTO> buscarClientePorEmail(String email) {
        // 1. Sanitiza o email antes de buscar no repositório
        String safeEmail = FieldValidation.sanitize(email);

        return clienteRepository.findByEmail(safeEmail)
                // 2. Mapeia a Entidade encontrada para DTO
                .map(ClienteMapper::fromEntity);
    }

    public Optional<ClienteDTO> buscarClientePorId(Long clienteId) {
        return clienteRepository.findById(clienteId)
                .map(ClienteMapper::fromEntity);
    }

    // ==========================================================
    // 🗑️ MÉTODOS DE DELEÇÃO
    // ==========================================================

    @Transactional
    public void deletarEnderecoDoCliente(Long clienteId, Long enderecoId) {

        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado."));

        // 1. Busca o endereço pelo ID na coleção do cliente (garante que o endereço
        // pertence ao cliente)
        Optional<Endereco> enderecoOpt = cliente.getEnderecos().stream()
                .filter(e -> e.getId().equals(enderecoId))
                .findFirst();

        if (enderecoOpt.isPresent()) {
            Endereco endereco = enderecoOpt.get();

            // 2. Remove o endereço da coleção
            cliente.getEnderecos().remove(endereco);

            // 3. Persiste a alteração no cliente e, se o JPA estiver configurado
            // corretamente
            // (orphanRemoval=true ou CASCADE.REMOVE), o endereço será deletado da tabela
            // Endereco.
            clienteRepository.save(cliente);
        } else {
            throw new IllegalArgumentException("Endereço não encontrado ou não pertence ao cliente.");
        }
    }

    @Transactional
    public void deletarCartaoDoCliente(Long clienteId, Long cartaoId) {

        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado."));

        // 1. Busca o cartão pelo ID na coleção do cliente (garante que o cartão
        // pertence ao cliente)
        Optional<Cartao> cartaoOpt = cliente.getCartoes().stream()
                .filter(c -> c.getId().equals(cartaoId))
                .findFirst();

        if (cartaoOpt.isPresent()) {
            Cartao cartao = cartaoOpt.get();

            // 2. Remove o cartão da coleção
            cliente.getCartoes().remove(cartao);

            // 3. Persiste a alteração no cliente e, se o JPA estiver configurado
            // corretamente,
            // o cartão será deletado da tabela Cartao.
            clienteRepository.save(cliente);
        } else {
            throw new IllegalArgumentException("Cartão não encontrado ou não pertence ao cliente.");
        }
    }

    @Transactional
    public void deletarClientePorId(Long clienteId) {
        // Verificação de existência é opcional, mas garante que a exceção seja mais
        // clara
        if (!clienteRepository.existsById(clienteId)) {
            throw new IllegalArgumentException("Cliente não encontrado para deleção.");
        }

        clienteRepository.deleteById(clienteId);
    }
}