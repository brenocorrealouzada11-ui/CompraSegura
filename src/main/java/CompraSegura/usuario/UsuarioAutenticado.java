package CompraSegura.usuario;

import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

public class UsuarioAutenticado extends User {
    private static final long serialVersionUID = 1L;
    private final Long id;
    private final long versaoCredenciais;

    public UsuarioAutenticado(Usuario usuario) {
        super(usuario.getEmail(), usuario.getSenhaHash(), List.of(new SimpleGrantedAuthority("ROLE_USUARIO")));
        this.id = usuario.getId();
        this.versaoCredenciais = usuario.getVersaoCredenciais();
    }

    public Long getId() { return id; }
    public long getVersaoCredenciais() { return versaoCredenciais; }
}
