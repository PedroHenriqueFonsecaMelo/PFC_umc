package umc.exs.backstage.handler;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;

// Importações (Omitidas, mas necessárias: @ControllerAdvice, @ExceptionHandler, RedirectAttributes, HttpServletRequest, Exception)

@ControllerAdvice
public class GlobalExceptionHandler {

    // Trata especificamente a exceção de validação (IllegalArgumentException)
    // que é lançada no seu ClienteService
    @ExceptionHandler(IllegalArgumentException.class)
    public String handleValidationExceptions(IllegalArgumentException ex, 
                                             HttpServletRequest request, 
                                             RedirectAttributes redirectAttributes) {

        // 1. Registra o erro para auditoria/debugging (Opcional, mas recomendado)
        System.err.println("Erro de Validação/Argumento no Serviço: " + ex.getMessage());

        // 2. Adiciona a mensagem de erro para ser exibida na próxima requisição (Flash Attribute)
        redirectAttributes.addFlashAttribute("erro", ex.getMessage());

        // 3. Obtém a URL da página anterior (referência)
        String referer = request.getHeader("Referer");
        
        // 4. Redireciona de volta para a URL da página anterior
        if (referer != null && !referer.isEmpty()) {
            // Se o referer for a página de login, use um valor padrão para evitar loops
            if (referer.contains("/clientes/login")) {
                 return "redirect:/clientes/login";
            }
            // Retorna o usuário para a página exata de onde ele veio (cadastro, atualização, etc.)
            return "redirect:" + referer;
        }

        // Caso não haja referer (acesso direto ou falha na requisição), retorna para a homepage
        return "redirect:/clientes/homepage"; 
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleAllExceptions(HttpServletRequest req, Exception ex) {
        
        ModelAndView mav = new ModelAndView("error/500"); // Redireciona para o template 500.html
        
        // Loga o erro COMPLETO no servidor para DEBUG, mas não expõe ao usuário
        System.err.println("URL: " + req.getRequestURL() + " | Exceção: " + ex.getMessage());
        ex.printStackTrace(); // <--- CRUCIAL: Loga o stack trace no console/log do servidor

        // Você pode passar uma mensagem AMIGÁVEL, mas NUNCA a mensagem da exceção (ex. ex.getMessage())
        mav.addObject("mensagem", "Não foi possível processar sua requisição."); 
        mav.addObject("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        
        return mav;
    }
    // Você pode adicionar outro @ExceptionHandler(RuntimeException.class)
    // para tratar erros genéricos, se necessário.
}