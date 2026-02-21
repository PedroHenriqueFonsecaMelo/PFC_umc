package umc.exs.service;

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
    private EmailSender emailService;

    @Autowired
    private EnderecoService enderecoService;

    @Autowired
    private CartaoService cartaoService;

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

        Cliente cliente = ClienteMapper.toEntity(signupDTO);
        cliente.setSenha(passwordEncoder.encode(signupDTO.getSenha()));

        Endereco endereco = enderecoService.saveOrReuseEndereco(enderecoDTO);
        endereco.getClientes().add(cliente);
        cliente.getEnderecos().add(endereco);

        Cartao cartao = cartaoService.saveOrReuseCartao(cartaoDTO);
        cartao.getClientes().add(cliente);
        cliente.getCartoes().add(cartao);

        Cliente salvo = clienteRepository.save(cliente);
        return ClienteMapper.fromEntity(salvo);
    }

    @Transactional
    public ClienteDTO atualizarClienteEAssociacoes(Long clienteId, ClienteDTO clienteAtualizadoDTO) {

        Cliente clienteExistente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado."));

        validarDadosAtualizacao(clienteAtualizadoDTO);

        clienteExistente.setNome(FieldValidation.sanitize(clienteAtualizadoDTO.getNome()));
        clienteExistente.setDatanasc(clienteAtualizadoDTO.getDatanasc());

        String generoStr = clienteAtualizadoDTO.getGen();
        if (generoStr != null && !generoStr.trim().isEmpty()) {
            try {
                Genero novoGenero = Genero.valueOf(generoStr.toUpperCase());
                clienteExistente.setGen(novoGenero);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Valor de gênero inválido. Use M, F ou OUTRO.");
            }
        }

        if (clienteAtualizadoDTO.getSenha() != null && !clienteAtualizadoDTO.getSenha().trim().isEmpty()) {
            if (!FieldValidation.isValidPassword(clienteAtualizadoDTO.getSenha())) {
                throw new IllegalArgumentException("Nova senha não atende aos requisitos de segurança.");
            }
            clienteExistente.setSenha(passwordEncoder.encode(clienteAtualizadoDTO.getSenha()));
        }

        Set<Long> idsRecebidos = clienteAtualizadoDTO.getEnderecos().stream()
                .map(EnderecoDTO::getId)
                .filter(id -> id != null && id != 0)
                .collect(Collectors.toSet());

        Set<Endereco> enderecosParaRemover = clienteExistente.getEnderecos().stream()
                .filter(e -> !idsRecebidos.contains(e.getId()))
                .collect(Collectors.toSet());

        enderecosParaRemover.forEach(e -> enderecoService.deletarEnderecoDoCliente(clienteExistente, e.getId()));

        Set<Endereco> enderecosAtualizados = new HashSet<>();

        for (EnderecoDTO dto : clienteAtualizadoDTO.getEnderecos()) {
            this.validarDadosEndereco(dto);
            Endereco endereco;

            if (dto.getId() != null && dto.getId() != 0) {
                endereco = enderecoRepository.findById(dto.getId())
                        .orElseThrow(
                                () -> new IllegalArgumentException("Endereço ID " + dto.getId() + " não encontrado."));

                endereco = EnderecoMapper.updateEntityFromDto(endereco, dto);
                endereco = enderecoRepository.save(endereco);

            } else {
                endereco = enderecoService.saveOrReuseEndereco(dto);
                endereco.getClientes().add(clienteExistente);
            }
            enderecosAtualizados.add(endereco);
        }

        clienteExistente.setEnderecos(enderecosAtualizados);

        Set<Long> cartoesIdsRecebidos = clienteAtualizadoDTO.getCartoes().stream()
                .map(CartaoDTO::getId)
                .filter(id -> id != null && id != 0)
                .collect(Collectors.toSet());

        Set<Cartao> cartoesParaRemover = clienteExistente.getCartoes().stream()
                .filter(c -> !cartoesIdsRecebidos.contains(c.getId()))
                .collect(Collectors.toSet());

        cartoesParaRemover.forEach(c -> cartaoService.deletarCartaoDoCliente(clienteExistente, c.getId()));

        Set<Cartao> cartoesAtualizados = new HashSet<>();

        for (CartaoDTO dto : clienteAtualizadoDTO.getCartoes()) {
            this.validarDadosCartao(dto);
            Cartao cartao;

            if (dto.getId() != null && dto.getId() != 0) {
                cartao = cartaoRepository.findById(dto.getId())
                        .orElseThrow(
                                () -> new IllegalArgumentException("Cartão ID " + dto.getId() + " não encontrado."));
            } else {
                cartao = cartaoService.saveOrReuseCartao(dto);
                cartao.getClientes().add(clienteExistente);
            }
            cartoesAtualizados.add(cartao);
        }

        clienteExistente.setCartoes(cartoesAtualizados);

        Cliente salvo = clienteRepository.save(clienteExistente);

        return ClienteMapper.fromEntity(salvo);
    }

    @Transactional
    public void deletarClientePorId(Long clienteId) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado para deleção."));

        cliente.getEnderecos().forEach(e -> e.getClientes().remove(cliente));
        cliente.getEnderecos().clear();

        cliente.getCartoes().forEach(c -> c.getClientes().remove(cliente));
        cliente.getCartoes().clear();

        clienteRepository.save(cliente);

        clienteRepository.delete(cliente);
    }

    public Optional<ClienteDTO> autenticarCliente(String email, String senha) {
        String safeEmail;
        try {
            safeEmail = FieldValidation.sanitizeEmail(email);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
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
            return Optional.empty();
        }

        return clienteRepository.findByEmail(safeEmail)
                .map(ClienteMapper::fromEntity);
    }

    public Optional<ClienteDTO> buscarClientePorId(Long clienteId) {
        return clienteRepository.findById(clienteId)
                .map(ClienteMapper::fromEntity);
    }

    private void validarDadosSignup(SignupDTO dto) {
        if (!FieldValidation.validarCampos(dto)) {
            throw new IllegalArgumentException("Todos os campos obrigatórios do cadastro devem ser preenchidos.");
        }

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

        if (!FieldValidation.isSafe(dto.getCpf()) || !FieldValidation.isValidCPF(dto.getCpf())) {
            throw new IllegalArgumentException("CPF inválido ou com padrões de segurança inaceitáveis.");
        }

        LocalDate dataNascimento = FieldValidation.isValidBirthDate(dto.getDatanasc());
        if (dataNascimento == null || !FieldValidation.isOver18(dataNascimento)) {
            throw new IllegalArgumentException("Data de nascimento inválida ou menor de 18 anos.");
        }

        if (!FieldValidation.isValidPassword(dto.getSenha())) {
            throw new IllegalArgumentException(
                    "A senha deve ter no mínimo 8 caracteres, letras maiúsculas/minúsculas, números e símbolos.");
        }

        if (!FieldValidation.isSafe(dto.getNome())) {
            throw new IllegalArgumentException("Nome contém caracteres de segurança inaceitáveis.");
        }
        dto.setNome(FieldValidation.sanitize(dto.getNome()));
        dto.setGen(FieldValidation.sanitize(dto.getGen()));
    }

    private void validarDadosAtualizacao(ClienteDTO dto) {
        if (dto.getNome() != null && !FieldValidation.isSafe(dto.getNome())) {
            throw new IllegalArgumentException("Nome contém caracteres de segurança inaceitáveis.");
        }

        if (dto.getDatanasc() != null) {
            LocalDate dataNascimento = FieldValidation.isValidBirthDate(dto.getDatanasc());
            if (dataNascimento == null || !FieldValidation.isOver18(dataNascimento)) {
                throw new IllegalArgumentException("Data de nascimento inválida ou menor de 18 anos.");
            }
        }

        if (dto.getNome() != null)
            dto.setNome(FieldValidation.sanitize(dto.getNome()));
        if (dto.getGen() != null)
            dto.setGen(FieldValidation.sanitize(dto.getGen()));
    }

    private void validarDadosEndereco(EnderecoDTO dto) {
        if (!FieldValidation.validarCampos(dto)) {
            throw new IllegalArgumentException("Todos os campos obrigatórios do endereço devem ser preenchidos.");
        }

        if (!FieldValidation.isSafe(dto.getRua()) ||
                !FieldValidation.isSafe(dto.getBairro()) ||
                !FieldValidation.isSafe(dto.getCidade()) ||
                !FieldValidation.isSafe(dto.getComplemento())) {
            throw new IllegalArgumentException("Dados de Endereço contêm padrões de segurança inaceitáveis.");
        }

        if (!FieldValidation.isValidCEP(dto.getCep())) {
            throw new IllegalArgumentException("CEP inválido.");
        }

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

        if (!FieldValidation.isSafe(dto.getNomeTitular())) {
            throw new IllegalArgumentException("Nome do Titular contém padrões de segurança inaceitáveis.");
        }

        if (!FieldValidation.isValidCardExpiry(dto.getValidade())) {
            throw new IllegalArgumentException("A data de validade do cartão é inválida ou expirou.");
        }

        dto.setNomeTitular(FieldValidation.sanitize(dto.getNomeTitular()));
    }

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

        String assunto = "Recuperação de Senha";
        String texto = "Olá,\n\nClique no link abaixo para redefinir sua senha:\n\n" +
                link + "\n\nSe você não solicitou, apenas ignore este e-mail.";

        emailService.enviar(email, assunto, texto);
    }

    private String gerarTokenManual() {
        return java.util.UUID.randomUUID().toString().replace("-", "");
    }

    @Transactional
    public boolean validarTokenRecuperacao(String token) {

        Optional<RecuperacaoSenha> opt = recuperacaoSenhaRepository.findByToken(token);

        if (opt.isEmpty()) {
            return false;
        }

        RecuperacaoSenha rec = opt.get();

        return LocalDateTime.now().isBefore(rec.getDataExpiracao());
    }

    @Transactional
    public String alterarSenhaComToken(String token, String novaSenha) {

        RecuperacaoSenha rec = recuperacaoSenhaRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token inválido ou expirado."));

        if (LocalDateTime.now().isAfter(rec.getDataExpiracao())) {
            throw new IllegalArgumentException("Token expirado.");
        }

        Cliente cliente = rec.getCliente();

        cliente.setSenha(passwordEncoder.encode(novaSenha));
        clienteRepository.save(cliente);

        recuperacaoSenhaRepository.delete(rec);

        return cliente.getEmail();
    }

}
