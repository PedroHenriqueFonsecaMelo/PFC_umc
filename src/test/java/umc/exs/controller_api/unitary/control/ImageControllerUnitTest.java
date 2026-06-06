package umc.exs.controller_api.unitary.control;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

import umc.exs.controller.api.control.ImageController;

class ImageControllerUnitTest {

    private final ImageController controller = new ImageController();

    @Test
    void serveImage_QuandoArquivoNaoExiste_RetornaNoImage() {
        ResponseEntity<Resource> resp = controller.serveImage("arquivo-inexistente-xyz.png");

        assertEquals(200, resp.getStatusCodeValue());
        assertNotNull(resp.getBody());
    }

    @Test
    void serveBlogImage_QuandoArquivoNaoExiste_Retorna404() {
        ResponseEntity<Resource> resp = controller.serveBlogImage("arquivo-nao-existe-xyz.png");

        assertEquals(404, resp.getStatusCodeValue());
    }
}

