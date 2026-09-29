package CompraSegura.chat;

import CompraSegura.usuario.UsuarioAutenticado;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.dao.DataAccessException;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

@Controller @RequestMapping("/minha-conta/mensagens")
public class ChatController {
    private final ChatService service;
    public ChatController(ChatService service) { this.service = service; }
    @InitBinder("mensagem") void campos(WebDataBinder binder) { binder.setAllowedFields("texto", "chave"); }
    @ModelAttribute void semCache(HttpServletResponse response) { response.setHeader("Cache-Control", "no-store"); }
    @PostMapping("/anuncio/{anuncioId}")
    String iniciar(@PathVariable Long anuncioId, @AuthenticationPrincipal UsuarioAutenticado principal) {
        return "redirect:/minha-conta/mensagens/" + service.iniciar(anuncioId, principal);
    }
    @GetMapping
    String listar(@AuthenticationPrincipal UsuarioAutenticado principal, @RequestParam(defaultValue = "0") int pagina, Model model) {
        model.addAttribute("pagina", service.listar(principal, pagina)); return "mensagens";
    }
    @GetMapping("/lista")
    String lista(@AuthenticationPrincipal UsuarioAutenticado principal, @RequestParam(defaultValue = "0") int pagina, Model model) {
        model.addAttribute("pagina", service.listar(principal, pagina)); return "fragments/conversas :: lista";
    }
    @GetMapping("/nao-lidas") @ResponseBody
    Map<String, Long> naoLidas(@AuthenticationPrincipal UsuarioAutenticado principal) { return Map.of("total", service.naoLidas(principal)); }
    @GetMapping("/{id}")
    String abrir(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado principal, @RequestParam(required = false) Long antes, Model model) {
        model.addAttribute("conversa", service.abrir(id, principal, antes)); model.addAttribute("mensagem", new MensagemForm()); return "conversa";
    }
    @GetMapping("/{id}/novas") @ResponseBody
    ChatService.Lote novas(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado principal, @RequestParam(defaultValue = "0") long apos) {
        return service.novas(id, principal, apos);
    }
    @GetMapping("/{id}/historico") @ResponseBody
    ChatService.Lote historico(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado principal, @RequestParam Long antes) {
        return service.anteriores(id, principal, antes);
    }
    private void preparar(Long id, UsuarioAutenticado principal, Model model) { model.addAttribute("conversa", service.abrir(id, principal, null)); }
    @PostMapping("/{id}")
    String enviarFormulario(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado principal,
            @Valid @ModelAttribute("mensagem") MensagemForm form, BindingResult erros, Model model) {
        preparar(id, principal, model);
        if (!erros.hasErrors()) {
            try { service.enviar(id, principal, form); return "redirect:/minha-conta/mensagens/" + id; }
            catch (DataAccessException ex) { erros.reject("indisponivel", "Não foi possível confirmar o envio. Seu texto foi mantido; tente novamente."); }
        }
        return "conversa";
    }
    public record Envio(ChatService.MensagemVista mensagem, String proximaChave) { }
    @PostMapping("/{id}/enviar") @ResponseBody
    ResponseEntity<?> enviar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado principal,
            @Valid @ModelAttribute("mensagem") MensagemForm form, BindingResult erros) {
        // Confere a participacao tambem quando o formulario e invalido.
        service.abrir(id, principal, null);
        if (erros.hasErrors()) return ResponseEntity.unprocessableEntity().body(Map.of("erro", erros.getAllErrors().get(0).getDefaultMessage()));
        try { return ResponseEntity.ok(new Envio(service.enviar(id, principal, form), UUID.randomUUID().toString())); }
        catch (DataAccessException ex) { return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("erro", "Não foi possível confirmar o envio. Tente novamente; seu texto foi mantido.")); }
    }
    @PostMapping("/{id}/lida") @ResponseBody
    ResponseEntity<Void> lida(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado principal, @RequestParam long ate) {
        service.marcarLida(id, principal, ate); return ResponseEntity.noContent().build();
    }
    @PostMapping("/{id}/marcar-lida")
    String lidaFormulario(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado principal, @RequestParam long ate) {
        service.marcarLida(id, principal, ate); return "redirect:/minha-conta/mensagens/" + id;
    }
}
