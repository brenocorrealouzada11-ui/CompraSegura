package CompraSegura.catalogo;

import CompraSegura.anuncio.*;
import CompraSegura.usuario.UsuarioAutenticado;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

@Controller
public class CatalogoController {
    private final CatalogoService catalogo;
    public CatalogoController(CatalogoService catalogo) { this.catalogo = catalogo; }
    @InitBinder("filtro") void campos(WebDataBinder binder) { binder.setAllowedFields("q", "tipo", "condicao", "minimo", "maximo", "ordem", "pagina"); }
    @ModelAttribute("conectado") boolean conectado(@AuthenticationPrincipal UsuarioAutenticado principal) { return principal != null; }
    @GetMapping("/anuncios")
    String pesquisar(@Valid @ModelAttribute("filtro") FiltroPesquisa filtro, BindingResult erros, Model model) {
        model.addAttribute("tipos", TipoAparelho.values()); model.addAttribute("condicoes", EstadoConservacao.values());
        model.addAttribute("errosFiltro", erros.getAllErrors().stream().map(erro ->
            erro instanceof org.springframework.validation.FieldError campo && campo.isBindingFailure()
                ? "Um dos filtros tem um valor inválido. Selecione as opções e informe preços numéricos."
                : erro.getDefaultMessage()).distinct().toList());
        model.addAttribute("pagina", erros.hasErrors() ? Page.empty() : catalogo.pesquisar(filtro));
        return "pesquisa-anuncios";
    }
    @GetMapping("/anuncios/{id}")
    String detalhes(@PathVariable Long id, Model model) {
        var detalhes = catalogo.detalhes(id);
        model.addAttribute("anuncio", detalhes.anuncio()); model.addAttribute("relatorio", detalhes.relatorio());
        return "anuncio-publico";
    }
    @GetMapping("/vendedores/{id}")
    String vendedor(@PathVariable Long id, @RequestParam(defaultValue = "0") int pagina, Model model) {
        var vendedor = catalogo.vendedor(id, pagina);
        model.addAttribute("vendedor", vendedor); model.addAttribute("pagina", vendedor.anuncios());
        return "historico-vendedor";
    }
}
