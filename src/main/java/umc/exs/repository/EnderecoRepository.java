package umc.exs.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import umc.exs.model.Endereco;

public interface EnderecoRepository extends JpaRepository<Endereco, Long> {

}
