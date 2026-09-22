package CompraSegura.usuario;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

@Service
@Validated
public class PerfilService {
    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;

    public PerfilService(UsuarioRepository usuarios, PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.encoder = encoder;
    }

    @Transactional(readOnly = true)
    public PerfilForm carregar(UsuarioAutenticado principal) {
        var usuario = usuarioDaSessao(principal);
        var form = new PerfilForm();
        form.setNome(usuario.getNome());
        form.setEmail(usuario.getEmail());
        return form;
    }

    @Transactional
    public boolean atualizar(UsuarioAutenticado principal, @Valid PerfilForm form) {
        var usuario = usuarioDaSessao(principal);
        boolean mudouEmail = !usuario.getEmail().equals(form.getEmail());
        if (mudouEmail) {
            verificarSenha(form.getSenhaAtual(), usuario);
            if (usuarios.existsByEmail(form.getEmail())) {
                throw new PerfilException("email", "Já existe uma conta com esse e-mail.");
            }
        }
        usuario.atualizarPerfil(form.getNome(), form.getEmail());
        usuarios.flush();
        return mudouEmail;
    }

    @Transactional
    public void alterarSenha(UsuarioAutenticado principal, @Valid SenhaForm form) {
        var usuario = usuarioDaSessao(principal);
        verificarSenha(form.getSenhaAtual(), usuario);
        if (!form.getNovaSenha().equals(form.getConfirmacaoSenha())) {
            throw new PerfilException("confirmacaoSenha", "A confirmação não corresponde à nova senha.");
        }
        if (encoder.matches(form.getNovaSenha(), usuario.getSenhaHash())) {
            throw new PerfilException("novaSenha", "Escolha uma senha diferente da atual.");
        }
        usuario.alterarSenha(encoder.encode(form.getNovaSenha()));
        usuarios.flush();
    }

    private Usuario usuarioDaSessao(UsuarioAutenticado principal) {
        var usuario = usuarios.findById(principal.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (usuario.getVersaoCredenciais() != principal.getVersaoCredenciais()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Entre novamente na conta.");
        }
        return usuario;
    }

    private void verificarSenha(String senha, Usuario usuario) {
        if (senha == null || !encoder.matches(senha, usuario.getSenhaHash())) {
            throw new PerfilException("senhaAtual", "A senha atual está incorreta.");
        }
    }
}
