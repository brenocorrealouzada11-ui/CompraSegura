package CompraSegura.anuncio;

import CompraSegura.usuario.*;
import java.time.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class VendaService {
    private final AnuncioRepository anuncios;
    private final UsuarioRepository usuarios;
    private final Clock relogio;
    public VendaService(AnuncioRepository anuncios, UsuarioRepository usuarios, Clock relogio) {
        this.anuncios = anuncios; this.usuarios = usuarios; this.relogio = relogio;
    }
    @Transactional
    public void concluir(Long id, UsuarioAutenticado principal, long versao) {
        // Mesma ordem de bloqueios da publicação e exclusão: usuário, depois anúncio.
        var usuario = usuarios.findParaAtualizacao(principal.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (usuario.getVersaoCredenciais() != principal.getVersaoCredenciais()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        var anuncio = anuncios.findAutorizadoParaAtualizacao(id, principal.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        // Uma repetição do envio não altera a data nem duplica a venda.
        if ("VENDIDO".equals(anuncio.getStatus())) return;
        if (anuncio.getVersao() != versao) throw new EdicaoDesatualizadaException();
        if (!"PUBLICADO".equals(anuncio.getStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Publique o anúncio antes de registrar a venda.");
        anuncio.marcarVendido(LocalDateTime.now(relogio));
        anuncios.flush();
    }
}
