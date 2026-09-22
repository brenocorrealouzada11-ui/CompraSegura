package CompraSegura.config;

import CompraSegura.usuario.UsuarioAutenticado;
import CompraSegura.usuario.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.filter.OncePerRequestFilter;

// Registrado apenas na cadeia do Spring Security, depois de carregar a sessao.
public class SessaoValidaFilter extends OncePerRequestFilter {
    private final UsuarioRepository usuarios;

    public SessaoValidaFilter(UsuarioRepository usuarios) { this.usuarios = usuarios; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UsuarioAutenticado principal) {
            boolean valida = usuarios.findById(principal.getId())
                    .map(usuario -> usuario.getVersaoCredenciais() == principal.getVersaoCredenciais())
                    .orElse(false);
            if (!valida) {
                new SecurityContextLogoutHandler().logout(request, response, auth);
                response.sendRedirect(request.getContextPath() + "/login?atualizada");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
