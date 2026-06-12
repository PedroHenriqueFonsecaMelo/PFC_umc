package umc.exs.repository.negocios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import umc.exs.model.entidades.foundation.EmailEnviado;

import java.util.List;

/** Gerencia o histórico de e-mails disparados pelo admin no banco de dados. */
@Repository
public interface EmailEnviadoRepository extends JpaRepository<EmailEnviado, Long> {

    /**
     * Lista todos os disparos de e-mail do mais recente ao mais antigo para
     * exibição no histórico do painel admin.
     */
    List<EmailEnviado> findAllByOrderByDataRegistroDesc();
}
