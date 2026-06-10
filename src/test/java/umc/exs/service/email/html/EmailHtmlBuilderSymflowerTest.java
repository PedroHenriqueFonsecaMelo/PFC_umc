package umc.exs.service.email.html;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class EmailHtmlBuilderSymflowerTest {
	@Test
	public void comunicadoAdmin1() {
		String nome = null;
		String mensagem = null;
		// assertThrows(java.lang.NullPointerException.class, () -> {
		EmailHtmlBuilder.comunicadoAdmin(nome, mensagem);
		// });
	}
}
