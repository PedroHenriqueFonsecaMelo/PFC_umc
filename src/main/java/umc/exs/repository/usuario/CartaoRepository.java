package umc.exs.repository.usuario;

import java.time.YearMonth;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import umc.exs.model.entidades.usuario.Cartao;

/**
 * Gerencia os cartões de crédito/débito dos clientes no banco, com número e
 * CPF criptografados.
 */
@Repository
public interface CartaoRepository extends JpaRepository<Cartao, Long> {

        /**
         * Busca um Cartão existente no banco de dados com base em todos os seus campos
         * de valor.
         * Isso é utilizado para reuso de entidades em relacionamentos Many-to-Many.
         */
        @Query("SELECT c FROM Cartao c WHERE " +
                        "c.numero = :numero AND " +
                        "c.nomeTitular = :nomeTitular AND " +
                        "c.validade = :validade AND " +
                        "c.bandeira = :bandeira AND " +
                        "c.cpfTitular = :cpfTitular")
        Optional<Cartao> findByValueFields(
                        @Param("numero") String numero,
                        @Param("nomeTitular") String nomeTitular,
                        @Param("validade") YearMonth validade,
                        @Param("bandeira") String bandeira,
                        @Param("cpfTitular") String cpfTitular);

        /**
         * Sobrecarga que aceita validade como String (formato MM/yy) em vez de
         * YearMonth, para compatibilidade com diferentes formatos de entrada.
         */
        @Query("SELECT c FROM Cartao c WHERE " +
                        "c.numero = :numero AND " +
                        "c.nomeTitular = :nomeTitular AND " +
                        "c.validade = :validade AND " +
                        "c.bandeira = :bandeira AND " +
                        "c.cpfTitular = :cpfTitular")
        Optional<Cartao> findByValueFields(
                        @Param("numero") String numero,
                        @Param("nomeTitular") String nomeTitular,
                        @Param("validade") String validade,
                        @Param("bandeira") String bandeira,
                        @Param("cpfTitular") String cpfTitular);

}