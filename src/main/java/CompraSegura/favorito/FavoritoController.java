package CompraSegura.favorito;

import CompraSegura.usuario.UsuarioAutenticado;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller @RequestMapping("/minha-conta/favoritos")
public class FavoritoController {
    private final FavoritoService favoritos;
    public FavoritoController(FavoritoService favoritos) { this.favoritos = favoritos; }
    @GetMapping
    String listar(@AuthenticationPrincipal UsuarioAutenticado principal, @RequestParam(defaultValue = "0") int pagina, Model model) {
        model.addAttribute("pagina", favoritos.listar(principal, pagina)); return "favoritos";
    }
    @PostMapping("/anuncio/{id}")
    String adicionar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado principal, RedirectAttributes redirect) {
        try { favoritos.adicionar(id, principal); redirect.addFlashAttribute("sucessoFavorito", "Anúncio salvo nos favoritos."); }
        catch (DataAccessException ex) { redirect.addFlashAttribute("erroFavorito", "Não foi possível salvar agora. Tente novamente."); }
        return "redirect:/anuncios/" + id;
    }
    @PostMapping("/{id}/remover")
    String remover(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado principal, RedirectAttributes redirect) {
        try { favoritos.remover(id, principal); redirect.addFlashAttribute("sucessoFavorito", "Anúncio removido dos favoritos."); }
        catch (DataAccessException ex) { redirect.addFlashAttribute("erroFavorito", "Não foi possível remover agora. Tente novamente."); }
        return "redirect:/minha-conta/favoritos";
    }
}
