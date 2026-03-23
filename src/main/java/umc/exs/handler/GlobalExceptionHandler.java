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
    public String handleIllegalArgument(IllegalArgumentException ex,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        System.err.println("Erro de Regra de Negócio: " + ex.getMessage());

        redirectAttributes.addFlashAttribute("erro", ex.getMessage());

        String referer = request.getHeader("Referer");
        if (referer != null && referer.contains("/clientes/login")) {
            return "redirect:/clientes/login";
        }

        return (referer != null) ? "redirect:" + referer : "redirect:/";
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleGenericException(HttpServletRequest req, Exception ex) {
        ModelAndView mav = new ModelAndView("error/500");
        System.err.println("Erro Crítico em: " + req.getRequestURL() + " -> " + ex.getMessage());

        mav.addObject("mensagem", "Ocorreu um erro interno. Tente novamente mais tarde.");
        mav.addObject("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        return mav;
    }
}