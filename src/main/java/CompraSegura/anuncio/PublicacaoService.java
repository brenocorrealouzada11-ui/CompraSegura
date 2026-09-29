package CompraSegura.anuncio;

import CompraSegura.usuario.*;
import jakarta.validation.Validator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PublicacaoService {
    private final AnuncioRepository anuncios;
    private final UsuarioRepository usuarios;
    private final EdicaoRascunhoService edicao;
    private final Validator validador;
    private final java.time.Clock relogio;
    private final CompraSegura.foto.FotoAnuncioRepository fotos;
    public PublicacaoService(AnuncioRepository anuncios, UsuarioRepository usuarios, EdicaoRascunhoService edicao, Validator validador, java.time.Clock relogio, CompraSegura.foto.FotoAnuncioRepository fotos) {
        this.anuncios = anuncios; this.usuarios = usuarios; this.edicao = edicao; this.validador = validador; this.relogio = relogio; this.fotos = fotos;
    }
    @Transactional
    public String alterar(Long id, UsuarioAutenticado principal, long versao, boolean publicar) {
        var usuario = usuarios.findParaAtualizacao(principal.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (usuario.getVersaoCredenciais() != principal.getVersaoCredenciais()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        var anuncio = anuncios.findAutorizadoParaAtualizacao(id, principal.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (anuncio.getVersao() != versao) throw new EdicaoDesatualizadaException();
        if (!(publicar ? "RASCUNHO" : "PUBLICADO").equals(anuncio.getStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT);
        var agora = java.time.LocalDateTime.now(relogio);
        usuario.expirarBloqueio(agora);
        if (publicar) {
            if (usuario.publicacaoBloqueada(agora)) throw new IllegalArgumentException(usuario.avisoPublicacao(agora));
            if (fotos.countByAnuncioId(id) < 1) throw new IllegalArgumentException("Adicione pelo menos uma foto do aparelho antes de publicar.");
            // Confere os dados salvos; resultados simulados e restricoes nao impedem a publicacao.
            var form = edicao.carregar(id, principal);
            if (!validador.validate(form).isEmpty()) throw new IllegalArgumentException("Revise os campos do rascunho antes de publicar.");
            NormalizadorIMEIs.normalizar(form.getImeis());
            anuncio.publicar(agora);
        } else anuncio.retirar();
        anuncios.flush();
        return usuario.avisoPublicacao(agora);
    }
    @Transactional(readOnly = true)
    public String aviso(UsuarioAutenticado principal) {
        return usuarios.findById(principal.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED))
            .avisoPublicacao(java.time.LocalDateTime.now(relogio));
    }
}
