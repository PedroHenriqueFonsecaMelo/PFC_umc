package umc.exs.backstage.service;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import umc.exs.model.daos.repository.ClienteRepository;
import umc.exs.model.dtos.auth.SignupDTO;
import umc.exs.model.dtos.interfaces.ClienteConvertible;
import umc.exs.model.dtos.user.CartaoDTO;
import umc.exs.model.dtos.user.ClienteDTO;
import umc.exs.model.dtos.user.EnderecoDTO;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.daos.mappers.CartaoMapper;
import umc.exs.model.daos.mappers.ClienteMapper;
import umc.exs.model.daos.mappers.EnderecoMapper;
import umc.exs.model.entidades.usuario.Endereco;

@Service
public class ClienteService {

    @Autowired
    private ClienteRepository clienteRepository;

    public ClienteDTO salvarCliente(ClienteConvertible dto) {
        Cliente cliente = ClienteMapper.toEntity(dto);
        Cliente salvo = clienteRepository.save(cliente);
        return ClienteMapper.fromEntity(salvo);
    }

    public Optional<ClienteDTO> buscarClientePorEmail(String email) {
        return clienteRepository.findByEmail(email)
                .map(ClienteMapper::fromEntity);
    }

    public Optional<ClienteDTO> buscarClientePorId(Long id) {
        return clienteRepository.findById(id)
                .map(ClienteMapper::fromEntity);
    }

    public List<ClienteDTO> listarTodos() {
        return clienteRepository.findAll()
                .stream()
                .map(ClienteMapper::fromEntity)
                .collect(Collectors.toList());
    }

    public ClienteDTO salvarClienteCompleto(SignupDTO signupDTO, EnderecoDTO enderecoDTO, CartaoDTO cartaoDTO) {

        Cliente cliente = ClienteMapper.toEntity(signupDTO);
        cliente.getEnderecos().add(EnderecoMapper.toEntity(enderecoDTO));
        cliente.getCartoes().add(CartaoMapper.toEntity(cartaoDTO));

        Cliente salvo = clienteRepository.save(cliente);
        return ClienteMapper.fromEntity(salvo);

    }

    public void deletarEnderecoDoCliente(Long clienteId, Long enderecoId) {
        clienteRepository.findById(clienteId).ifPresent(cliente -> {
            cliente.getEnderecos().removeIf(endereco -> endereco.getId().equals(enderecoId));
            clienteRepository.save(cliente);
        });
    }

    public void deletarCartaoDoCliente(Long clienteId, Long cartaoId) {
        clienteRepository.findById(clienteId).ifPresent(cliente -> {
            cliente.getCartoes().removeIf(cartao -> cartao.getId().equals(cartaoId));
            clienteRepository.save(cliente);
        });
    }

    public static void copyNonNullProperties(ClienteDTO src, ClienteDTO target) {
        System.out.println("Copiando propriedades de " + src + " para " + target);
        System.out.println("Fonte: " + src.toString());
        System.out.println("Destino: " + target.toString());

        if (src == null || target == null) {
            throw new IllegalArgumentException("Fonte e destino não podem ser nulos.");
        }

        String[] nullOrEmptyPropertyNames = Arrays.stream(src.getClass().getDeclaredFields())
                .filter(field -> {
                    field.setAccessible(true);
                    try {
                        Object value = field.get(src);

                        // Ignora propriedades nulas ou vazias
                        if (value == null) {
                            return true;
                        }

                        return value instanceof String str && str.trim().isEmpty();

                    } catch (IllegalAccessException e) {
                        return false;
                    }
                })
                .map(Field::getName)
                .toArray(String[]::new);

        // Copia apenas os campos válidos
        BeanUtils.copyProperties(src, target, nullOrEmptyPropertyNames);
    }

}
