package CompraSegura.reputacao;

import CompraSegura.usuario.UsuarioAutenticado;
import jakarta.validation.Valid;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller @RequestMapping("/minha-conta/vendedores/{id}/avaliacao")
public class AvaliacaoPerfilController {
    private final AvaliacaoPerfilService avaliacoes;
    public AvaliacaoPerfilController(AvaliacaoPerfilService avaliacoes) { this.avaliacoes = avaliacoes; }
    @InitBinder("avaliacaoPerfil") void campos(WebDataBinder binder) { binder.setAllowedFields("nota", "comentario"); }
    @PostMapping
    String salvar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado principal,
            @Valid @ModelAttribute("avaliacaoPerfil") AvaliacaoPerfilForm form, BindingResult erros, RedirectAttributes redirect) {
        avaliacoes.verificarPermissao(id, principal);
        if (erros.hasErrors()) {
            redirect.addFlashAttribute("errosAvaliacaoPerfil", erros.getAllErrors().stream().map(e ->
                e instanceof org.springframework.validation.FieldError f && f.isBindingFailure() ? "Escolha uma nota de 1 a 5." : e.getDefaultMessage()).toList());
            redirect.addFlashAttribute("avaliacaoPerfil", form);
        } else try {
            avaliacoes.salvar(id, principal, form);
            redirect.addFlashAttribute("sucessoAvaliacaoPerfil", "Sua avaliação foi salva.");
        } catch (DataAccessException ex) {
            redirect.addFlashAttribute("errosAvaliacaoPerfil", java.util.List.of("Não foi possível salvar agora. Tente novamente."));
            redirect.addFlashAttribute("avaliacaoPerfil", form);
        }
        return "redirect:/vendedores/" + id + "#avaliacoes";
    }
    @PostMapping("/remover")
    String remover(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado principal, RedirectAttributes redirect) {
        boolean visivel;
        try { visivel = avaliacoes.remover(id, principal); }
        catch (DataAccessException ex) {
            redirect.addFlashAttribute("errosAvaliacaoPerfil", java.util.List.of("Não foi possível remover agora. Tente novamente."));
            return "redirect:/vendedores/" + id + "#avaliacoes";
        }
        redirect.addFlashAttribute("sucessoAvaliacaoPerfil", "Sua avaliação foi removida.");
        return visivel ? "redirect:/vendedores/" + id + "#avaliacoes" : "redirect:/vendedores";
    }
}
