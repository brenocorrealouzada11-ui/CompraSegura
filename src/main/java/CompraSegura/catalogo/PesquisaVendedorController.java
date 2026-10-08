package CompraSegura.catalogo;

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
public class PesquisaVendedorController {
    private final PesquisaVendedorService pesquisa;
    public PesquisaVendedorController(PesquisaVendedorService pesquisa) { this.pesquisa = pesquisa; }
    @InitBinder("filtroVendedor") void campos(WebDataBinder binder) { binder.setAllowedFields("nome", "pagina"); }
    @GetMapping("/vendedores")
    String pesquisar(@Valid @ModelAttribute("filtroVendedor") FiltroVendedor filtro, BindingResult erros,
            @AuthenticationPrincipal UsuarioAutenticado principal, Model model) {
        model.addAttribute("conectado", principal != null);
        model.addAttribute("errosBusca", erros.getAllErrors().stream().map(e ->
            e instanceof org.springframework.validation.FieldError f && f.isBindingFailure() ? "Informe uma página válida." : e.getDefaultMessage()).toList());
        model.addAttribute("pagina", erros.hasErrors() ? Page.empty() : pesquisa.pesquisar(filtro));
        return "pesquisa-vendedores";
    }
}
