package CompraSegura.usuario;

import java.security.Principal;
import CompraSegura.anuncio.AnuncioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@Controller
public class ContaController {
    private final UsuarioRepository usuarios;
    private final AnuncioRepository anuncios;

    public ContaController(UsuarioRepository usuarios, AnuncioRepository anuncios) {
        this.usuarios = usuarios;
        this.anuncios = anuncios;
    }

    @GetMapping("/login")
    String login(Principal principal) {
        return principal == null ? "login" : "redirect:/minha-conta";
    }

    @GetMapping("/minha-conta")
    String minhaConta(@AuthenticationPrincipal UsuarioAutenticado principal, Model model) {
        var usuario = usuarios.findById(principal.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        // Apenas dados de exibicao; o hash nunca e enviado ao template.
        model.addAttribute("nome", usuario.getNome());
        model.addAttribute("email", usuario.getEmail());
        model.addAttribute("inicial", usuario.getNome().substring(0, usuario.getNome().offsetByCodePoints(0, 1)).toUpperCase(java.util.Locale.ROOT));
        model.addAttribute("totalAnuncios", anuncios.countByVendedorId(principal.getId()));
        return "minha-conta";
    }
}
