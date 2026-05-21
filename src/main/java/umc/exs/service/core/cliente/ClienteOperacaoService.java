package umc.exs.service.core.cliente;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import umc.exs.dto.request.cliente.ClienteUpdateRequest;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.service.log.LogAuditoriaService;

@Service
@RequiredArgsConstructor
public class ClienteOperacaoService {

    private final ClienteDomainService domainService;
    private final ClienteRepositoryService repositoryService;
    private final LogAuditoriaService auditoria;

    @Transactional
    public Cliente atualizarCliente(Long clienteId, ClienteUpdateRequest dto) {
        Cliente atualizado = domainService.atualizarDados(clienteId, dto);

        auditoria.registrarLog(
                "ATUALIZACAO_DADOS",
                clienteId,
                atualizado.getEmail(),
                "Dados atualizados.");

        return atualizado;
    }

    @Transactional
    public String uploadFotoPerfil(Long clienteId, MultipartFile foto) {
        String url = domainService.gerenciarUploadFoto(clienteId, foto);

        Cliente cliente = repositoryService.buscarPorId(clienteId);

        auditoria.registrarLog(
                "UPLOAD_FOTO",
                clienteId,
                cliente.getEmail(),
                "Foto atualizada.");

        return url;
    }

    @Transactional
    public Cliente adicionarTokens(Long clienteId, Double valor) {
        Cliente dto = domainService.adicionarTokens(clienteId, valor);

        auditoria.registrarLog(
                "RECARGA_TOKENS",
                clienteId,
                dto.getEmail(),
                String.format("Recarga de %.2f via PIX", valor));

        return dto;
    }
}