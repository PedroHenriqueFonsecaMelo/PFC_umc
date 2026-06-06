package umc.exs.service.cliente.delegado;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import umc.exs.dto.mapper.ClienteMapper;
import umc.exs.dto.request.cliente.ClienteUpdateRequest;
import umc.exs.dto.request.cliente.SignupRequest;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.entidades.usuario.Endereco;
import umc.exs.service.cliente.EnderecoService;
import umc.exs.service.log.AcaoAuditoria;
import umc.exs.service.log.LogAuditoriaService;
import umc.exs.service.storage.ArquivosService;

@Service
@RequiredArgsConstructor
public class ClientePerfilService {

        private final ClienteRepositoryService repositoryService;
        private final EnderecoService enderecoService;
        private final ClienteMapper clienteMapper;
        private final PasswordEncoder passwordEncoder;
        private final LogAuditoriaService auditoria;

        @Transactional
        public Cliente atualizarDados(Long clienteId, ClienteUpdateRequest dto) {

                Cliente cliente = repositoryService.buscarPorId(clienteId);

                if (dto.getDatanasc() != null) {
                        int idade = Period.between(dto.getDatanasc(), LocalDate.now()).getYears();

                        if (idade < 18)
                                throw new IllegalArgumentException("É necessário ser maior de 18 anos.");

                        if (idade > 120)
                                throw new IllegalArgumentException("Digite uma data de nascimento válida.");
                }

                cliente.setNome(dto.getNome());
                cliente.setDatanasc(dto.getDatanasc());

                if (dto.getSenha() != null && !dto.getSenha().isBlank()) {
                        cliente.setSenha(passwordEncoder.encode(dto.getSenha()));
                }

                List<Endereco> enderecosEntidade = clienteMapper.toEntityList(dto.getEnderecos());

                enderecoService.sincronizarEnderecos(cliente, enderecosEntidade);

                Cliente salvo = repositoryService.salvar(cliente);

                auditoria.registrarLog(
                        AcaoAuditoria.CLIENTE_DADOS_ATUALIZADOS.name(),
                        clienteId,
                        salvo.getEmail(),
                        "Atualização de dados do perfil");

                return salvo;
        }

        @Transactional
        public String atualizarFoto(Long clienteId, MultipartFile foto) {

                Cliente cliente = repositoryService.buscarPorId(clienteId);

                String url = ArquivosService.salvarArquivoFisico(foto);

                cliente.setFotoPerfil(url);

                repositoryService.salvar(cliente);

                auditoria.registrarLog(
                        AcaoAuditoria.CLIENTE_FOTO_ATUALIZADA.name(),
                        clienteId,
                        cliente.getEmail(),
                        "Foto de perfil atualizada");

                return url;
        }

        @Transactional
        public Cliente cadastrar(SignupRequest request) {

                Cliente cliente = clienteMapper.paraEntidade(request);

                cliente.setSenha(passwordEncoder.encode(request.getSenha()));
                cliente.setSaldoTokens(0.0);

                Cliente salvo = repositoryService.salvar(cliente);

                auditoria.registrarLog(
                        AcaoAuditoria.CLIENTE_CADASTRADO.name(),
                        salvo.getId(),
                        salvo.getEmail(),
                        "Novo cliente cadastrado");

                return salvo;
        }

        @Transactional
        public Cliente cadastrarCompleto(SignupRequest request, Endereco enderecoDTO) {

                Cliente cliente = clienteMapper.paraEntidade(request);

                cliente.setSenha(passwordEncoder.encode(request.getSenha()));
                cliente.setSaldoTokens(0.0);

                Endereco endereco = enderecoService.saveOrReuseEndereco(enderecoDTO);

                cliente.getEnderecos().add(endereco);
                endereco.getClientes().add(cliente);

                Cliente salvo = repositoryService.salvar(cliente);

                auditoria.registrarLog(
                        AcaoAuditoria.CLIENTE_CADASTRO_COMPLETO.name(),
                        salvo.getId(),
                        salvo.getEmail(),
                        "Cadastro completo com endereço");

                return salvo;
        }
}