package umc.exs.service.core;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import umc.exs.DTOs.auth.SignupDTO;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.service.senha.FieldValidation;

@Service
@Slf4j
@RequiredArgsConstructor
public class ClientMainService {

    private final ClienteRepository clienteRepository;

    // ==========================================================
    // 🛡️ VALIDAÇÕES DE REGRA DE NEGÓCIO
    // ==========================================================

    /**
     * Valida se um novo cadastro de cliente é permitido.
     * Segue o princípio de Fail-Fast (lança exceção no primeiro erro).
     */
    public void validarNovoCliente(SignupDTO dto) {
        // 1. Campos obrigatórios
        if (!FieldValidation.validarCampos(dto)) {
            throw new IllegalArgumentException("Campos obrigatórios ausentes no cadastro.");
        }

        // 2. Email Único e Sanitizado
        String safeEmail = FieldValidation.sanitizeEmail(dto.getEmail());
        if (clienteRepository.findByEmail(safeEmail).isPresent()) {
            throw new IllegalArgumentException("O e-mail informado já está cadastrado.");
        }
        dto.setEmail(safeEmail);

        // 3. Validação de CPF
        if (!FieldValidation.isValidCPF(dto.getCpf())) {
            throw new IllegalArgumentException("O CPF informado é inválido.");
        }

        // 4. Regra de Maioridade (18 anos)
        LocalDate dataNasc = FieldValidation.isValidBirthDate(dto.getDatanasc());
        if (dataNasc == null || !FieldValidation.isOver18(dataNasc)) {
            throw new IllegalArgumentException("O cliente deve ter pelo menos 18 anos de idade.");
        }

        // 5. Complexidade de Senha (se fornecida)
        if (dto.getSenha() != null && !dto.getSenha().trim().isEmpty()) {
            if (!FieldValidation.isValidPassword(dto.getSenha())) {
                throw new IllegalArgumentException("A senha não atende aos requisitos mínimos de segurança.");
            }
        }
    }

    /**
     * Valida atualizações em um cliente existente.
     */
    public void validarAtualizacao(String nome, String senha) {
        if (nome == null || nome.trim().isEmpty()) {
            throw new IllegalArgumentException("O nome não pode ser vazio.");
        }

        if (senha != null && !senha.trim().isEmpty() && !FieldValidation.isValidPassword(senha)) {
            throw new IllegalArgumentException("A nova senha informada é inválida.");
        }
    }

    // ==========================================================
    // 🔍 BUSCAS ATÔMICAS (ENTITY LEVEL)
    // ==========================================================

    @SuppressWarnings("null")
    @Transactional
    public Cliente buscarPorId(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado com o ID: " + id));
    }

    @Transactional
    public Cliente buscarPorEmail(String email) {
        return clienteRepository.findByEmail(FieldValidation.sanitizeEmail(email))
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado com o e-mail: " + email));
    }

    public Optional<Cliente> encontrarPorEmail(String email) {
        return clienteRepository.findByEmail(FieldValidation.sanitizeEmail(email));
    }
}
