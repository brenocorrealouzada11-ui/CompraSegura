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
    private final EdicaoRascunhoService edicao;
    private final CompraSegura.foto.FotosAnuncioService fotos;
    private final CompraSegura.foto.FotoAnuncioRepository imagensAnuncio;
    private final PublicacaoService publicacao;
    public AnuncioController(AnuncioService service, EvidenciaService evidencias, PreparacaoAnuncioService preparacao, VerificacaoService verificacoes, EdicaoRascunhoService edicao, CompraSegura.foto.FotosAnuncioService fotos, CompraSegura.foto.FotoAnuncioRepository imagensAnuncio, PublicacaoService publicacao) {
        this.service = service; this.evidencias = evidencias; this.preparacao = preparacao; this.verificacoes = verificacoes; this.edicao = edicao; this.fotos = fotos; this.imagensAnuncio = imagensAnuncio; this.publicacao = publicacao;
    }
    @ModelAttribute("tipos") TipoAparelho[] tipos() { return TipoAparelho.values(); }
    @ModelAttribute("condicoes") EstadoConservacao[] condicoes() { return EstadoConservacao.values(); }
    @ModelAttribute("historicosReparos") HistoricoReparos[] historicosReparos() { return HistoricoReparos.values(); }
    @InitBinder("anuncio") void campos(WebDataBinder binder) {
        binder.setAllowedFields("titulo", "descricao", "preco", "tipo", "marca", "modelo", "condicao", "reparos", "imeis", "versao");
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
            @RequestParam(name = "arquivo", required = false) MultipartFile arquivo, @RequestParam(name = "fotos", required = false) java.util.List<MultipartFile> arquivos, @RequestParam(name = "removerFotos", required = false) java.util.List<Long> removerFotos, Model model, RedirectAttributes redirect) {
        if (!erros.hasErrors()) {
            try {
                Long id = fotos.criar(principal, form, arquivo, arquivos);
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
        if ((arquivo != null && !arquivo.isEmpty()) || (arquivos != null && arquivos.stream().anyMatch(f -> !f.isEmpty()))) model.addAttribute("reselecionarImagem", true);
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
    @GetMapping("/{id}/editar")
    String editar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado principal, Model model) {
        var form = edicao.carregar(id, principal);
        model.addAttribute("anuncio", form);
        prepararEdicao(id, principal, form, model);
        return "novo-anuncio";
    }

    @PostMapping("/{id}/editar")
    String salvarEdicao(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado principal,
            @Valid @ModelAttribute("anuncio") EdicaoRascunhoForm form, BindingResult erros,
            @RequestParam(name = "arquivo", required = false) MultipartFile arquivo, @RequestParam(name = "fotos", required = false) java.util.List<MultipartFile> arquivos, @RequestParam(name = "removerFotos", required = false) java.util.List<Long> removerFotos, Model model, RedirectAttributes redirect) {
        var atual = edicao.carregar(id, principal); // Verifica dono e status mesmo com formulario invalido.
        if (!erros.hasErrors()) {
            try {
                boolean reavaliar = fotos.editar(id, principal, form, arquivo, arquivos, removerFotos);
                redirect.addFlashAttribute("anuncioEditado", true);
                redirect.addFlashAttribute("reavaliacaoNecessaria", reavaliar);
                return "redirect:/minha-conta/anuncios/" + id;
            } catch (PreparacaoAnuncioException ex) { rejeitar(erros, ex); }
            catch (EdicaoDesatualizadaException | org.springframework.dao.OptimisticLockingFailureException ex) {
                erros.reject("conflito", "Este rascunho foi alterado em outra aba. Reabra a edição antes de salvar.");
            } catch (DataAccessException ex) {
                erros.reject("indisponivel", "Não foi possível salvar as alterações agora. Tente novamente.");
            }
        }
        prepararEdicao(id, principal, atual, model);
        if ((arquivo != null && !arquivo.isEmpty()) || (arquivos != null && arquivos.stream().anyMatch(f -> !f.isEmpty()))) model.addAttribute("reselecionarImagem", true);
        return "novo-anuncio";
    }

    @PostMapping("/{id}/editar/avaliar")
    String avaliarEdicao(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado principal,
            @Valid @ModelAttribute("anuncio") EdicaoRascunhoForm form, BindingResult erros,
            @RequestParam(name = "arquivo", required = false) MultipartFile arquivo, Model model, HttpServletResponse response) {
        edicao.carregar(id, principal);
        if (!erros.hasErrors()) {
            try { model.addAttribute("relatorio", edicao.previa(id, principal, form, arquivo)); }
            catch (PreparacaoAnuncioException ex) { rejeitar(erros, ex); }
            catch (EdicaoDesatualizadaException ex) { erros.reject("conflito", ex.getMessage()); }
        }
        if (erros.hasErrors()) {
            response.setStatus(422);
            model.addAttribute("errosVerificacao", erros.getAllErrors().stream().map(e -> e.getDefaultMessage()).toList());
        }
        response.setHeader("Cache-Control", "no-store");
        return "fragments/avaliacao :: resultado";
    }

    private void prepararEdicao(Long id, UsuarioAutenticado principal, EdicaoRascunhoForm atual, Model model) {
        model.addAttribute("modoEdicao", true);
        model.addAttribute("anuncioId", id);
        model.addAttribute("fotosAtuais", imagensAnuncio.ids(id));
        model.addAttribute("condicaoAnterior", atual.getCondicaoAnterior());
        model.addAttribute("reparosAnteriores", atual.getReparosAnteriores());
        model.addAttribute("evidencia", evidencias.resumo(id, principal.getId()));
    }

    @GetMapping("/{id}/excluir")
    String confirmarExclusao(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado principal, Model model) {
        model.addAttribute("anuncio", service.buscar(id, principal.getId()));
        model.addAttribute("avisoPublicacao", publicacao.aviso(principal));
        return "excluir-anuncio";
    }

    @PostMapping("/{id}/excluir")
    String excluir(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado principal, @RequestParam(required = false) Long versao, Model model, RedirectAttributes redirect) {
        try {
            boolean publicado = service.buscar(id, principal.getId()).isPublicado();
            String aviso = service.excluir(id, principal, versao);
            redirect.addFlashAttribute("avisoExclusao", aviso);
            redirect.addFlashAttribute("publicadoExcluido", publicado);
            redirect.addFlashAttribute("anuncioExcluido", true);
            return "redirect:/minha-conta/anuncios";
        } catch (DataAccessException ex) {
            model.addAttribute("anuncio", service.buscar(id, principal.getId()));
            model.addAttribute("erroExclusao", "Não foi possível excluir agora. Tente novamente em instantes.");
            model.addAttribute("avisoPublicacao", publicacao.aviso(principal));
            return "excluir-anuncio";
        }
    }

    @GetMapping("/{id}") String detalhes(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado principal, Model model) {
        model.addAttribute("anuncio", service.buscar(id, principal.getId()));
        model.addAttribute("evidencia", evidencias.resumo(id, principal.getId()));
        model.addAttribute("relatorio", verificacoes.ultimo(id, principal.getId()));
        model.addAttribute("fotosAtuais", imagensAnuncio.ids(id));
        return "detalhe-anuncio";
    }
}
