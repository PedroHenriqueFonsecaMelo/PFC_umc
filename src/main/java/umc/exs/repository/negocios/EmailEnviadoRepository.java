package umc.exs.repository.negocios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import umc.exs.model.entidades.foundation.EmailEnviado;

import java.util.List;

@Repository
public interface EmailEnviadoRepository extends JpaRepository<EmailEnviado, Long> {
    List<EmailEnviado> findAllByOrderByDataRegistroDesc();
}
