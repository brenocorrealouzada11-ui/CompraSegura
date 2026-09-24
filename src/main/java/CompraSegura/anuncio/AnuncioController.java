package CompraSegura.anuncio;

import CompraSegura.usuario.UsuarioAutenticado;
import jakarta.validation.Valid;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller @RequestMapping("/minha-conta/anuncios")
public class AnuncioController {
    private final AnuncioService service;
    public AnuncioController(AnuncioService service) { this.service = service; }
    @ModelAttribute("tipos") TipoAparelho[] tipos() { return TipoAparelho.values(); }
    @ModelAttribute("condicoes") EstadoConservacao[] condicoes() { return EstadoConservacao.values(); }
    @ModelAttribute("historicosReparos") HistoricoReparos[] historicosReparos() { return HistoricoReparos.values(); }
    @InitBinder("anuncio") void campos(WebDataBinder binder) {
        binder.setAllowedFields("titulo", "descricao", "preco", "tipo", "marca", "modelo", "condicao", "reparos", "imeis");
    }
    @GetMapping String listar(@AuthenticationPrincipal UsuarioAutenticado principal, @RequestParam(defaultValue = "0") int pagina, Model model) {
        model.addAttribute("pagina", service.listar(principal.getId(), pagina));
        return "meus-anuncios";
    }
    @GetMapping("/novo") String novo(Model model) {
        model.addAttribute("anuncio", new AnuncioForm());
        return "novo-anuncio";
    }
    @PostMapping("/novo") String criar(@AuthenticationPrincipal UsuarioAutenticado principal,
            @Valid @ModelAttribute("anuncio") AnuncioForm form, BindingResult erros, RedirectAttributes redirect) {
        if (!erros.hasErrors()) {
            try {
                Long id = service.criar(principal, form);
                redirect.addFlashAttribute("anuncioSalvo", true);
                return "redirect:/minha-conta/anuncios/" + id;
            } catch (IllegalArgumentException ex) {
                erros.rejectValue("imeis", "invalido", ex.getMessage());
            } catch (DataAccessException ex) {
                erros.reject("indisponivel", "Não foi possível salvar agora. Tente novamente em instantes.");
            }
        }
        return "novo-anuncio";
    }
    @GetMapping("/{id}/excluir")
    String confirmarExclusao(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado principal, Model model) {
        model.addAttribute("anuncio", service.buscar(id, principal.getId()));
        return "excluir-anuncio";
    }

    @PostMapping("/{id}/excluir")
    String excluir(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado principal, Model model, RedirectAttributes redirect) {
        try {
            service.excluir(id, principal);
            redirect.addFlashAttribute("anuncioExcluido", true);
            return "redirect:/minha-conta/anuncios";
        } catch (DataAccessException ex) {
            model.addAttribute("anuncio", service.buscar(id, principal.getId()));
            model.addAttribute("erroExclusao", "Não foi possível excluir agora. Tente novamente em instantes.");
            return "excluir-anuncio";
        }
    }

    @GetMapping("/{id}") String detalhes(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado principal, Model model) {
        model.addAttribute("anuncio", service.buscar(id, principal.getId()));
        return "detalhe-anuncio";
    }
}
