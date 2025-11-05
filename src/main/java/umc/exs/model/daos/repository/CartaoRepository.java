package umc.exs.model.daos.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import umc.exs.model.entidades.usuario.Cartao;

public interface CartaoRepository extends JpaRepository<Cartao, Long> {
}
