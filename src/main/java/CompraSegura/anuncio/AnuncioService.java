package CompraSegura.anuncio;

import CompraSegura.usuario.*;
import CompraSegura.evidencia.EvidenciaRepository;
import CompraSegura.confiabilidade.AvaliacaoRepository;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

@Service @Validated
public class AnuncioService {
    private final AnuncioRepository anuncios;
    private final UsuarioRepository usuarios;
    private final EvidenciaRepository evidencias;
    private final AvaliacaoRepository avaliacoes;
    public AnuncioService(AnuncioRepository anuncios, UsuarioRepository usuarios, EvidenciaRepository evidencias, AvaliacaoRepository avaliacoes) {
        this.anuncios = anuncios; this.usuarios = usuarios; this.evidencias = evidencias; this.avaliacoes = avaliacoes;
    }

    @Transactional
    public Long criar(UsuarioAutenticado principal, @Valid AnuncioForm form) {
        var numeros = NormalizadorIMEIs.normalizar(form.getImeis());
        var vendedor = usuarios.findById(principal.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (vendedor.getVersaoCredenciais() != principal.getVersaoCredenciais()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        var aparelho = new Aparelho(form.getTipo(), new ModeloAparelho(form.getMarca(), form.getModelo()), form.getCondicao().getDescricao(), form.getReparos().getDescricao(), numeros);
        return anuncios.saveAndFlush(new Anuncio(vendedor, aparelho, form.getTitulo(), form.getDescricao(), form.getPreco())).getId();
    }

    @Transactional
    public void excluir(Long id, UsuarioAutenticado principal) {
        var usuario = usuarios.findById(principal.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (usuario.getVersaoCredenciais() != principal.getVersaoCredenciais()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        var anuncio = anuncios.findAutorizadoParaAtualizacao(id, principal.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!"RASCUNHO".equals(anuncio.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Somente rascunhos podem ser excluídos por esta ação.");
        }
        // Remove tambem a entidade gerenciada; o FK em cascata protege a integridade no banco.
        avaliacoes.deleteByAnuncioId(id);
        evidencias.deleteByAnuncioId(id);
        anuncios.delete(anuncio);
        anuncios.flush();
    }

    @Transactional(readOnly = true)
    public Page<AnuncioDetalhes> listar(Long vendedorId, int pagina) {
        return anuncios.findByVendedorIdOrderByIdDesc(vendedorId, PageRequest.of(Math.max(0, pagina), 12)).map(AnuncioDetalhes::de);
    }

    @Transactional(readOnly = true)
    public AnuncioDetalhes buscar(Long id, Long vendedorId) {
        return AnuncioDetalhes.de(anuncios.findByIdAndVendedorId(id, vendedorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
    }
}
