package CompraSegura.catalogo;

import CompraSegura.usuario.UsuarioAutenticado;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class ComparacaoSegurancaController {
    private final ComparacaoService comparacao;
    public ComparacaoSegurancaController(ComparacaoService comparacao) { this.comparacao = comparacao; }
    @ModelAttribute("conectado") boolean conectado(@AuthenticationPrincipal UsuarioAutenticado principal) { return principal != null; }
    @GetMapping("/comparar")
    String comparar(@RequestParam(required = false) List<String> ids, Model model, HttpServletResponse response) {
        model.addAttribute("itens", List.of());
        if (ids != null) {
            try { model.addAttribute("itens", comparacao.comparar(ids)); }
            catch (IllegalArgumentException ex) { response.setStatus(400); model.addAttribute("erroComparacao", ex.getMessage()); }
            catch (ResponseStatusException ex) { response.setStatus(ex.getStatusCode().value()); model.addAttribute("erroComparacao", ex.getReason()); }
        }
        return "comparar-aparelhos";
    }
    @GetMapping("/seguranca") String seguranca() { return "central-seguranca"; }
}
