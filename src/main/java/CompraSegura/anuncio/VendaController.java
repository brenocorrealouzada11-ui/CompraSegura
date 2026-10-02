package CompraSegura.anuncio;

import CompraSegura.usuario.UsuarioAutenticado;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller @RequestMapping("/minha-conta/anuncios")
public class VendaController {
    private final AnuncioService anuncios;
    private final VendaService vendas;
    public VendaController(AnuncioService anuncios, VendaService vendas) { this.anuncios = anuncios; this.vendas = vendas; }
    @GetMapping("/{id}/vender")
    String confirmar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado principal, Model model) {
        var anuncio = anuncios.buscar(id, principal.getId());
        if (!anuncio.isPublicado()) return "redirect:/minha-conta/anuncios/" + id;
        model.addAttribute("anuncio", anuncio);
        return "vender-anuncio";
    }
    @PostMapping("/{id}/vender")
    String concluir(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado principal, @RequestParam long versao, RedirectAttributes redirect) {
        try {
            vendas.concluir(id, principal, versao);
            redirect.addFlashAttribute("sucessoPublicacao", "Venda registrada. O anúncio saiu da pesquisa e foi preservado no seu histórico.");
        } catch (EdicaoDesatualizadaException ex) {
            redirect.addFlashAttribute("erroPublicacao", "O anúncio foi alterado em outra aba. Confira os dados antes de confirmar a venda.");
        } catch (DataAccessException ex) {
            redirect.addFlashAttribute("erroPublicacao", "Não foi possível confirmar a venda agora. Confira o estado do anúncio antes de tentar novamente.");
        }
        return "redirect:/minha-conta/anuncios/" + id;
    }
}
