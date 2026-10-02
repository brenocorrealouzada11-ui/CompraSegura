package CompraSegura.anuncio;

import CompraSegura.usuario.UsuarioAutenticado;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller @RequestMapping("/minha-conta/anuncios")
public class PublicacaoController {
    private final PublicacaoService publicacao;
    private final AnuncioService anuncios;
    public PublicacaoController(PublicacaoService publicacao, AnuncioService anuncios) { this.publicacao = publicacao; this.anuncios = anuncios; }
    @GetMapping("/{id}/publicar")
    String confirmar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado principal, Model model) {
        var anuncio = anuncios.buscar(id, principal.getId());
        if (!anuncio.isRascunho()) return "redirect:/minha-conta/anuncios/" + id;
        model.addAttribute("anuncio", anuncio);
        model.addAttribute("avisoPublicacao", publicacao.aviso(principal));
        return "publicar-anuncio";
    }
    @PostMapping("/{id}/publicar")
    String publicar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado principal, @RequestParam long versao, RedirectAttributes redirect) {
        return alterar(id, principal, versao, true, redirect);
    }
    @PostMapping("/{id}/retirar")
    String retirar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado principal, @RequestParam long versao, RedirectAttributes redirect) {
        return alterar(id, principal, versao, false, redirect);
    }
    private String alterar(Long id, UsuarioAutenticado principal, long versao, boolean publicar, RedirectAttributes redirect) {
        try {
            String aviso = publicacao.alterar(id, principal, versao, publicar);
            redirect.addFlashAttribute("avisoPublicacao", aviso);
            redirect.addFlashAttribute("sucessoPublicacao", publicar ? "Anúncio publicado. Ele já aparece na pesquisa." : "Anúncio retirado da pesquisa e salvo como rascunho.");
        } catch (EdicaoDesatualizadaException ex) {
            redirect.addFlashAttribute("erroPublicacao", "O anúncio foi alterado em outra aba. Confira os dados e tente novamente.");
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("erroPublicacao", ex.getMessage());
        } catch (DataAccessException ex) {
            redirect.addFlashAttribute("erroPublicacao", "Não foi possível alterar a publicação. Tente novamente.");
        }
        return "redirect:/minha-conta/anuncios/" + id;
    }
}
