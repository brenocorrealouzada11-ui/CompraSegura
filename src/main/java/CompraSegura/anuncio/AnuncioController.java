package CompraSegura.anuncio;

import CompraSegura.usuario.UsuarioAutenticado;
import CompraSegura.evidencia.EvidenciaService;
import jakarta.validation.Valid;
import CompraSegura.confiabilidade.VerificacaoService;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller @RequestMapping("/minha-conta/anuncios")
public class AnuncioController {
    private final AnuncioService service;
    private final EvidenciaService evidencias;
    private final PreparacaoAnuncioService preparacao;
    private final VerificacaoService verificacoes;
    public AnuncioController(AnuncioService service, EvidenciaService evidencias, PreparacaoAnuncioService preparacao, VerificacaoService verificacoes) {
        this.service = service; this.evidencias = evidencias; this.preparacao = preparacao; this.verificacoes = verificacoes;
    }
    @ModelAttribute("tipos") TipoAparelho[] tipos() { return TipoAparelho.values(); }
    @ModelAttribute("condicoes") EstadoConservacao[] condicoes() { return EstadoConservacao.values(); }
    @ModelAttribute("historicosReparos") HistoricoReparos[] historicosReparos() { return HistoricoReparos.values(); }
    @InitBinder("anuncio") void campos(WebDataBinder binder) {
        binder.setAllowedFields("titulo", "descricao", "preco", "tipo", "marca", "modelo", "condicao", "reparos", "imeis");
    }
    @GetMapping String listar(@AuthenticationPrincipal UsuarioAutenticado principal, @RequestParam(defaultValue = "0") int pagina, Model model) {
        model.addAttribute("pagina", service.listar(principal.getId(), pagina));
        return "meus-anuncios";
    }
    @GetMapping("/novo") String novo(Model model) {
        model.addAttribute("anuncio", new AnuncioForm());
        return "novo-anuncio";
    }
    @PostMapping("/novo") String criar(@AuthenticationPrincipal UsuarioAutenticado principal,
            @Valid @ModelAttribute("anuncio") AnuncioForm form, BindingResult erros,
            @RequestParam(name = "arquivo", required = false) MultipartFile arquivo, Model model, RedirectAttributes redirect) {
        if (!erros.hasErrors()) {
            try {
                Long id = preparacao.salvar(principal, form, arquivo);
                redirect.addFlashAttribute("anuncioSalvo", true);
                return "redirect:/minha-conta/anuncios/" + id;
            } catch (PreparacaoAnuncioException ex) {
                rejeitar(erros, ex);
            } catch (IllegalArgumentException ex) {
                erros.rejectValue("imeis", "invalido", ex.getMessage());
            } catch (DataAccessException ex) {
                erros.reject("indisponivel", "Não foi possível salvar agora. Tente novamente em instantes.");
            }
        }
        if (arquivo != null && !arquivo.isEmpty()) model.addAttribute("reselecionarImagem", true);
        return "novo-anuncio";
    }

    @PostMapping("/novo/avaliar")
    String avaliar(@Valid @ModelAttribute("anuncio") AnuncioForm form, BindingResult erros,
                   @RequestParam(name = "arquivo", required = false) MultipartFile arquivo, Model model, HttpServletResponse response) {
        if (!erros.hasErrors()) {
            try { model.addAttribute("relatorio", preparacao.previa(form, arquivo)); }
            catch (PreparacaoAnuncioException ex) { rejeitar(erros, ex); }
        }
        if (erros.hasErrors()) {
            response.setStatus(422);
            model.addAttribute("errosVerificacao", erros.getAllErrors().stream().map(e -> e.getDefaultMessage()).toList());
        }
        response.setHeader("Cache-Control", "no-store");
        return "fragments/avaliacao :: resultado";
    }

    private void rejeitar(BindingResult erros, PreparacaoAnuncioException ex) {
        if (ex.getCampo().equals("imeis")) erros.rejectValue("imeis", "invalido", ex.getMessage());
        else erros.reject("evidencia", ex.getMessage());
    }

    @PostMapping("/{id}/avaliar")
    String reavaliar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado principal, RedirectAttributes redirect) {
        // O filtro de sessao valida as credenciais; o servico limita a consulta ao dono.
        try {
            verificacoes.reavaliar(id, principal.getId());
            redirect.addFlashAttribute("avaliacaoAtualizada", true);
        } catch (DataAccessException ex) {
            redirect.addFlashAttribute("erroAvaliacao", "Não foi possível salvar a avaliação agora. Tente novamente.");
        }
        return "redirect:/minha-conta/anuncios/" + id + "#avaliacao";
    }
    @GetMapping("/{id}/excluir")
    String confirmarExclusao(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado principal, Model model) {
        model.addAttribute("anuncio", service.buscar(id, principal.getId()));
        return "excluir-anuncio";
    }

    @PostMapping("/{id}/excluir")
    String excluir(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado principal, Model model, RedirectAttributes redirect) {
        try {
            service.excluir(id, principal);
            redirect.addFlashAttribute("anuncioExcluido", true);
            return "redirect:/minha-conta/anuncios";
        } catch (DataAccessException ex) {
            model.addAttribute("anuncio", service.buscar(id, principal.getId()));
            model.addAttribute("erroExclusao", "Não foi possível excluir agora. Tente novamente em instantes.");
            return "excluir-anuncio";
        }
    }

    @GetMapping("/{id}") String detalhes(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado principal, Model model) {
        model.addAttribute("anuncio", service.buscar(id, principal.getId()));
        model.addAttribute("evidencia", evidencias.resumo(id, principal.getId()));
        model.addAttribute("relatorio", verificacoes.ultimo(id, principal.getId()));
        return "detalhe-anuncio";
    }
}
