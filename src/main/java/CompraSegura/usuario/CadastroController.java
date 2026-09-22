package CompraSegura.usuario;

import jakarta.validation.Valid;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CadastroController {
    private final CadastroService cadastroService;

    public CadastroController(CadastroService cadastroService) {
        this.cadastroService = cadastroService;
    }

    @InitBinder("cadastro")
    void configurarCampos(WebDataBinder binder) {
        binder.setAllowedFields("nome", "email", "senha");
    }

    @GetMapping("/cadastro")
    String formulario(Model model) {
        model.addAttribute("cadastro", new CadastroForm());
        return "cadastro";
    }

    @PostMapping("/cadastro")
    String cadastrar(@Valid @ModelAttribute("cadastro") CadastroForm form,
                     BindingResult erros, RedirectAttributes redirect) {
        if (!erros.hasErrors()) {
            try {
                cadastroService.cadastrar(form);
                redirect.addFlashAttribute("cadastroConcluido", true);
                return "redirect:/cadastro/sucesso";
            } catch (EmailJaCadastradoException ex) {
                erros.rejectValue("email", "duplicado", ex.getMessage());
            } catch (DataAccessException ex) {
                erros.reject("indisponivel", "Não foi possível salvar seu cadastro agora. Tente novamente em instantes.");
            }
        }
        form.setSenha(null);
        return "cadastro";
    }

    @GetMapping("/cadastro/sucesso")
    String sucesso(Model model) {
        if (!Boolean.TRUE.equals(model.getAttribute("cadastroConcluido"))) {
            return "redirect:/cadastro";
        }
        return "cadastro-sucesso";
    }
}
