package umc.exs.repository.usuario;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import umc.exs.model.entidades.usuario.Endereco;

@Repository
public interface EnderecoRepository extends JpaRepository<Endereco, Long> {

        // Versão com @Query, otimizada para tratar 'complemento' como opcional/nulo
        @Query("SELECT e FROM Endereco e WHERE " +
                        "e.cep = :cep AND " +
                        "e.rua = :rua AND " +
                        "e.numero = :numero AND " +
                        "e.bairro = :bairro AND " +
                        "e.cidade = :cidade AND " +
                        "e.estado = :estado AND " +
                        "((:complemento IS NULL AND e.complemento IS NULL) OR e.complemento = :complemento)")
        Optional<Endereco> findByValueFields(
                        @Param("cep") String cep,
                        @Param("rua") String rua,
                        @Param("numero") String numero,
                        @Param("complemento") String complemento,
                        @Param("bairro") String bairro,
                        @Param("cidade") String cidade,
                        @Param("estado") String estado);
}