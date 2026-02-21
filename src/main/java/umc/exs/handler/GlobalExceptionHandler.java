package umc.exs.handler;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public String handleValidationExceptions(IllegalArgumentException ex,
                                             HttpServletRequest request,
                                             RedirectAttributes redirectAttributes) {

        System.err.println("Erro de Validação/Argumento no Serviço: " + ex.getMessage());
        redirectAttributes.addFlashAttribute("erro", ex.getMessage());

        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isEmpty()) {
            if (referer.contains("/clientes/login")) {
                 return "redirect:/clientes/login";
            }
            return "redirect:" + referer;
        }

        return "redirect:/clientes/homepage";
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleAllExceptions(HttpServletRequest req, Exception ex) {
        ModelAndView mav = new ModelAndView("error/500");
        System.err.println("URL: " + req.getRequestURL() + " | Exceção: " + ex.getMessage());
        mav.addObject("mensagem", "Não foi possível processar sua requisição.");
        mav.addObject("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        return mav;
    }
}
