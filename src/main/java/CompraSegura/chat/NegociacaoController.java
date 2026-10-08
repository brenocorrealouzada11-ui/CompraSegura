package CompraSegura.chat;

import CompraSegura.usuario.UsuarioAutenticado;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class NegociacaoController {
    private final NegociacaoService negociacoes;
    public NegociacaoController(NegociacaoService negociacoes) { this.negociacoes = negociacoes; }
    @GetMapping("/minha-conta/negociacoes")
    String listar(@AuthenticationPrincipal UsuarioAutenticado principal,
            @RequestParam(defaultValue = "TODOS") NegociacaoService.Papel papel,
            @RequestParam(defaultValue = "TODAS") NegociacaoService.Situacao situacao,
            @RequestParam(defaultValue = "0") int pagina, Model model) {
        model.addAttribute("papel", papel.name()); model.addAttribute("situacao", situacao.name());
        model.addAttribute("pagina", negociacoes.listar(principal, papel, situacao, pagina));
        return "negociacoes";
    }
}
