package CompraSegura.usuario;

import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class CadastroService {
    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;

    public CadastroService(UsuarioRepository usuarios, PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.encoder = encoder;
    }

    public void cadastrar(@Valid CadastroForm form) {
        if (usuarios.existsByEmail(form.getEmail())) {
            throw new EmailJaCadastradoException();
        }
        try {
            usuarios.saveAndFlush(new Usuario(form.getNome(), form.getEmail(), encoder.encode(form.getSenha())));
        } catch (DataIntegrityViolationException ex) {
            // A restricao UNIQUE protege tambem contra cadastros simultaneos.
            if (usuarios.existsByEmail(form.getEmail())) {
                throw new EmailJaCadastradoException();
            }
            throw ex;
        }
    }
}
