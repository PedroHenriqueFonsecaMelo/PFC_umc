package umc.exs.model.daos.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import umc.exs.model.entidades.foundation.LivroAnuncio;

public interface LivroRepository extends JpaRepository<LivroAnuncio, Long> {

}
