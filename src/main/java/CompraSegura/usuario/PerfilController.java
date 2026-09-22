package CompraSegura.usuario;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PerfilController {
    private final PerfilService service;
    public PerfilController(PerfilService service) { this.service = service; }

    @InitBinder("perfil")
    void camposPerfil(WebDataBinder binder) { binder.setAllowedFields("nome", "email", "senhaAtual"); }
    @InitBinder("senha")
    void camposSenha(WebDataBinder binder) { binder.setAllowedFields("senhaAtual", "novaSenha", "confirmacaoSenha"); }

    @GetMapping("/minha-conta/editar")
    String editar(@AuthenticationPrincipal UsuarioAutenticado principal, Model model) {
        model.addAttribute("perfil", service.carregar(principal));
        return "editar-perfil";
    }

    @PostMapping("/minha-conta/editar")
    String salvar(@AuthenticationPrincipal UsuarioAutenticado principal,
                  @Valid @ModelAttribute("perfil") PerfilForm form, BindingResult erros,
                  Authentication auth, HttpServletRequest request, HttpServletResponse response,
                  RedirectAttributes redirect) {
        if (!erros.hasErrors()) {
            try {
                if (service.atualizar(principal, form)) {
                    new SecurityContextLogoutHandler().logout(request, response, auth);
                    return "redirect:/login?emailAlterado";
                }
                redirect.addFlashAttribute("perfilSalvo", true);
                return "redirect:/minha-conta";
            } catch (PerfilException ex) {
                erros.rejectValue(ex.getCampo(), "invalido", ex.getMessage());
            } catch (DataIntegrityViolationException ex) {
                erros.rejectValue("email", "duplicado", "Não foi possível usar esse e-mail. Ele pode já estar cadastrado.");
            } catch (OptimisticLockingFailureException ex) {
                erros.reject("conflito", "Sua conta foi alterada em outra janela. Recarregue a página e tente novamente.");
            } catch (DataAccessException ex) {
                erros.reject("indisponivel", "Não foi possível salvar agora. Tente novamente em instantes.");
            }
        }
        form.setSenhaAtual(null);
        return "editar-perfil";
    }

    @GetMapping("/minha-conta/senha")
    String senha(Model model) {
        model.addAttribute("senha", new SenhaForm());
        return "alterar-senha";
    }

    @PostMapping("/minha-conta/senha")
    String alterarSenha(@AuthenticationPrincipal UsuarioAutenticado principal,
                        @Valid @ModelAttribute("senha") SenhaForm form, BindingResult erros,
                        Authentication auth, HttpServletRequest request, HttpServletResponse response) {
        if (!erros.hasErrors()) {
            try {
                service.alterarSenha(principal, form);
                new SecurityContextLogoutHandler().logout(request, response, auth);
                return "redirect:/login?senhaAlterada";
            } catch (PerfilException ex) {
                erros.rejectValue(ex.getCampo(), "invalido", ex.getMessage());
            } catch (OptimisticLockingFailureException ex) {
                erros.reject("conflito", "Sua conta foi alterada em outra janela. Recarregue a página e tente novamente.");
            } catch (DataAccessException ex) {
                erros.reject("indisponivel", "Não foi possível alterar a senha agora. Tente novamente em instantes.");
            }
        }
        form.limpar();
        return "alterar-senha";
    }
}
