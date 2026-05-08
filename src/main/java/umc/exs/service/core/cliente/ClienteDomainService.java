package umc.exs.service.core.cliente;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import umc.exs.DTOs.auth.SignupDTO;
import umc.exs.DTOs.user.CartaoDTO;
import umc.exs.DTOs.user.ClienteDTO;
import umc.exs.DTOs.user.EnderecoDTO;
import umc.exs.mappers.ClienteMapper;
import umc.exs.model.entidades.foundation.Transacao;
import umc.exs.model.entidades.logic.RecuperacaoSenha;
import umc.exs.model.entidades.usuario.Cartao;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.entidades.usuario.Endereco;
import umc.exs.repository.usuario.RecuperacaoSenhaRepository;
import umc.exs.service.carteira.CarteiraService;
import umc.exs.service.core.control.ArquivosService;
import umc.exs.service.senha.FieldValidation;
import umc.exs.service.senha.SenhaService;

@Service
@Slf4j
@RequiredArgsConstructor
public class ClienteDomainService {

    private final ClienteRepositoryService repositoryService;
    private final EnderecoService enderecoService;
    private final CartaoService cartaoService;
    private final CarteiraService carteiraService;
    private final ClienteMapper clienteMapper;
    private final PasswordEncoder passwordEncoder;
    private final SenhaService senhaService;
    private final RecuperacaoSenhaRepository tokenRepository;

    @Transactional
    public ClienteDTO cadastrarCliente(SignupDTO signupDTO) {
        Cliente cliente = clienteMapper.paraEntidade(signupDTO);
        cliente.setSenha(passwordEncoder.encode(signupDTO.getSenha()));
        cliente.setSaldoTokens(0.0);
        return clienteMapper.paraDTO(repositoryService.salvar(cliente));
    }

    @Transactional
    public ClienteDTO cadastrarClienteCompleto(SignupDTO signupDTO, EnderecoDTO enderecoDTO, CartaoDTO cartaoDTO) {
        Cliente cliente = clienteMapper.paraEntidade(signupDTO);
        cliente.setSenha(passwordEncoder.encode(signupDTO.getSenha()));
        cliente.setSaldoTokens(0.0);

        Endereco endereco = enderecoService.saveOrReuseEndereco(enderecoDTO);
        cliente.getEnderecos().add(endereco);
        endereco.getClientes().add(cliente);

        Cartao cartao = cartaoService.saveOrReuseCartao(cartaoDTO);
        cliente.getCartoes().add(cartao);
        cartao.getClientes().add(cliente);

        return clienteMapper.paraDTO(repositoryService.salvar(cliente));
    }

    @Transactional
    public ClienteDTO atualizarDados(@NonNull Long id, ClienteDTO dto) {
        Cliente cliente = repositoryService.buscarPorId(id);

        cliente.setNome(FieldValidation.sanitize(dto.getNome()));
        cliente.setDatanasc(dto.getDatanasc());
        // CPF nunca é alterado após o cadastro (@Column updatable=false garante no BD)

        if (dto.getSenha() != null && !dto.getSenha().trim().isEmpty()) {
            cliente.setSenha(passwordEncoder.encode(dto.getSenha()));
        }

        enderecoService.sincronizarEnderecos(cliente, dto.getEnderecos());
        // cartaoService.sincronizarCartoes(cliente, dto.getCartoes()); // cartões removidos da UI

        return clienteMapper.paraDTO(repositoryService.salvar(cliente));
    }

    @Transactional
    public String gerenciarUploadFoto(@NonNull Long id, MultipartFile foto) {
        Cliente cliente = repositoryService.buscarPorId(id);

        String url = ArquivosService.salvarArquivoFisico(foto);
        cliente.setFotoPerfil(url);
        repositoryService.salvar(cliente);
        return url;

    }

    @Transactional
    public ClienteDTO processarAutenticacao(String email, String senha) {
        Cliente cliente = repositoryService.encontrarPorEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("E-mail ou senha inválidos."));

        if (!cliente.isEmailVerificado()) {
            throw new IllegalArgumentException("E-mail não verificado. Verifique sua caixa de entrada.");
        }

        if (cliente.isBloqueada()) {
            throw new IllegalArgumentException("Conta bloqueada. Entre em contato com o suporte.");
        }

        if (!passwordEncoder.matches(senha, cliente.getSenha())) {
            repositoryService.registrarFalhaLogin(cliente);
            if (cliente.isBloqueada()) {
                throw new IllegalArgumentException("Conta bloqueada. Entre em contato com o suporte.");
            }
            int restantes = Math.max(0, 5 - cliente.getTentativas());
            throw new IllegalArgumentException(
                    "Senha incorreta. Você tem " + restantes + " tentativa(s) restantes.");
        }

        repositoryService.resetarTentativasLogin(cliente);
        return clienteMapper.paraDTO(cliente);
    }

    @Transactional
    public ClienteDTO adicionarTokens(@NonNull Long clienteId, Double valor) {
        Cliente cliente = repositoryService.buscarPorId(clienteId);

        if (valor <= 0) {
            throw new IllegalArgumentException("O valor para recarga deve ser positivo.");
        }

        Double saldoAtual = cliente.getSaldoTokens() != null ? cliente.getSaldoTokens() : 0.0;
        cliente.setSaldoTokens(saldoAtual + valor);

        carteiraService.adicionarTokens(cliente, valor, "PIX", null);

        return clienteMapper.paraDTO(repositoryService.salvar(cliente));
    }

    @Transactional
    public Cliente redefinirSenha(String token, String novaSenha) {
        RecuperacaoSenha registro = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token inválido ou inexistente."));

        if (registro.isExpirado()) {
            tokenRepository.delete(registro);
            throw new IllegalArgumentException("Este link de recuperação expirou.");
        }

        Cliente cliente = registro.getCliente();
        cliente.setSenha(passwordEncoder.encode(novaSenha));

        repositoryService.salvar(cliente);
        tokenRepository.delete(registro);

        log.info("Senha redefinida com sucesso para: {}", cliente.getEmail());
        return cliente;
    }
    
    @Transactional
    public void alterarSenhaComVerificacao(String email, String senhaAtual, String novaSenha) {
        Cliente cliente = repositoryService.buscarPorEmailOuFalhar(email);
        if (!passwordEncoder.matches(senhaAtual, cliente.getSenha())) {
            throw new IllegalArgumentException("Senha atual incorreta.");
        }
        cliente.setSenha(passwordEncoder.encode(novaSenha));
        repositoryService.salvar(cliente);
    }

    public void gerarTokenRecuperacao(Cliente cliente) {
        senhaService.iniciarRecuperacao(cliente);
    }

    @Transactional
    public void aprovarPagamento(String pagamentoId) {
        carteiraService.confirmarPagamentoPix(pagamentoId);
    }

    public boolean verificarSeFoiPago(String pagamentoId) {
        return carteiraService.verificarStatusPagamento(pagamentoId);
    }

    @Transactional
    public void registrarTransacaoPendente(Cliente cliente, Double valor, String pagamentoId) {
        carteiraService.registrarIntencaoPagamento(cliente, valor, pagamentoId);
    }

    public List<Transacao> listarHistoricoTransacoes(Cliente cliente) {
        return carteiraService.listarHistoricoPorCliente(cliente.getId());
    }

    public ClienteDTO converterParaDTO(Cliente cliente) {
        return clienteMapper.paraDTO(cliente);
    }

    public boolean validarToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }

        return tokenRepository.findByToken(token)
                .map(registro -> !registro.isExpirado())
                .orElse(false);
    }
}