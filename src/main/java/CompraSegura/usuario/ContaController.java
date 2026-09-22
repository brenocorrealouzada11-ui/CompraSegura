package CompraSegura.usuario;

import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@Controller
public class ContaController {
    private final UsuarioRepository usuarios;

    public ContaController(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
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
        return "minha-conta";
    }
}
