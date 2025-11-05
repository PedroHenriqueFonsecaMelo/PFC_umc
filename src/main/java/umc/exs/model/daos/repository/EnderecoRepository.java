package umc.exs.model.daos.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import umc.exs.model.entidades.usuario.Endereco;

@Repository
public interface EnderecoRepository extends JpaRepository<Endereco, Long> {

}
