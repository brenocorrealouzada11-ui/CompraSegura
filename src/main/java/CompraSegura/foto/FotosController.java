package CompraSegura.foto;
import CompraSegura.usuario.UsuarioAutenticado;
import org.springframework.dao.DataAccessException;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
@Controller
public class FotosController {
    private final FotosService service;
    private final FotoPerfilRepository perfis;
    public FotosController(FotosService service, FotoPerfilRepository perfis) { this.service = service; this.perfis = perfis; }
    @GetMapping("/minha-conta/foto")
    String formulario(@AuthenticationPrincipal UsuarioAutenticado principal, Model model) {
        model.addAttribute("temFoto", perfis.existsByUsuarioId(principal.getId())); return "foto-perfil";
    }
    @PostMapping("/minha-conta/foto")
    String salvar(@AuthenticationPrincipal UsuarioAutenticado principal, @RequestParam(required = false) MultipartFile arquivo,
                  @RequestParam(defaultValue = "false") boolean remover, RedirectAttributes redirect) {
        try { service.perfil(principal, arquivo, remover); redirect.addFlashAttribute("fotoSalva", remover ? "Foto de perfil removida." : "Foto de perfil atualizada."); }
        catch (IllegalArgumentException ex) { redirect.addFlashAttribute("erroFoto", ex.getMessage()); }
        catch (DataAccessException ex) { redirect.addFlashAttribute("erroFoto", "Não foi possível salvar a foto. Tente novamente."); }
        return "redirect:/minha-conta/foto";
    }
    @GetMapping("/minha-conta/foto/imagem")
    ResponseEntity<byte[]> minhaFoto(@AuthenticationPrincipal UsuarioAutenticado principal) { return imagem(service.perfil(principal.getId(), true)); }
    @GetMapping("/vendedores/{id}/foto")
    ResponseEntity<byte[]> perfil(@PathVariable Long id) { return imagem(service.perfil(id, false)); }
    @GetMapping("/anuncios/{id}/fotos/{foto}")
    ResponseEntity<byte[]> publica(@PathVariable Long id, @PathVariable Long foto) { return imagem(service.aparelho(id, foto, null)); }
    @GetMapping("/minha-conta/anuncios/{id}/fotos/{foto}")
    ResponseEntity<byte[]> privada(@PathVariable Long id, @PathVariable Long foto, @AuthenticationPrincipal UsuarioAutenticado principal) { return imagem(service.aparelho(id, foto, principal.getId())); }
    private ResponseEntity<byte[]> imagem(CompraSegura.evidencia.ImagemEvidencia imagem) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(imagem.tipoConteudo())).cacheControl(CacheControl.noStore())
            .header("X-Content-Type-Options", "nosniff").header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"foto." + (imagem.tipoConteudo().equals("image/png") ? "png" : "jpg") + "\"").body(imagem.conteudo());
    }
}
