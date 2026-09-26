package CompraSegura.evidencia;

import CompraSegura.usuario.UsuarioAutenticado;
import org.springframework.dao.DataAccessException;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/minha-conta/anuncios/{id}/evidencia")
public class EvidenciaController {
    private final EvidenciaService service;
    public EvidenciaController(EvidenciaService service) { this.service = service; }

    @PostMapping
    String enviar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado principal,
                  @RequestParam(name = "arquivo", required = false) MultipartFile arquivo, RedirectAttributes redirect) {
        try {
            service.enviar(id, principal, arquivo);
            redirect.addFlashAttribute("evidenciaSalva", true);
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("erroEvidencia", ex.getMessage());
        } catch (DataAccessException ex) {
            redirect.addFlashAttribute("erroEvidencia", "Não foi possível salvar a imagem agora. Recarregue a página e tente novamente.");
        }
        return "redirect:/minha-conta/anuncios/" + id + "#evidencia";
    }

    @GetMapping
    ResponseEntity<byte[]> visualizar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado principal) {
        var imagem = service.carregar(id, principal.getId());
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(imagem.tipoConteudo()))
            .cacheControl(CacheControl.noStore())
            .header("X-Content-Type-Options", "nosniff")
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"evidencia." + (imagem.tipoConteudo().equals("image/png") ? "png" : "jpg") + "\"")
            .body(imagem.conteudo());
    }
}
