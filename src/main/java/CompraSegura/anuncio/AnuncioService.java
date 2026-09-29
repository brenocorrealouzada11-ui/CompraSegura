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
    private final CompraSegura.foto.FotoAnuncioRepository fotos;
    private final java.time.Clock relogio;
    public AnuncioService(AnuncioRepository anuncios, UsuarioRepository usuarios, EvidenciaRepository evidencias, AvaliacaoRepository avaliacoes, CompraSegura.foto.FotoAnuncioRepository fotos, java.time.Clock relogio) {
        this.anuncios = anuncios; this.usuarios = usuarios; this.evidencias = evidencias; this.avaliacoes = avaliacoes; this.fotos = fotos; this.relogio = relogio;
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
    public String excluir(Long id, UsuarioAutenticado principal) { return excluir(id, principal, null); }

    @Transactional
    public String excluir(Long id, UsuarioAutenticado principal, Long versao) {
        // Publicacao e exclusao bloqueiam primeiro o usuario, depois o anuncio.
        var usuario = usuarios.findParaAtualizacao(principal.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (usuario.getVersaoCredenciais() != principal.getVersaoCredenciais()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        var anuncio = anuncios.findAutorizadoParaAtualizacao(id, principal.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (versao != null && versao != anuncio.getVersao() || "PUBLICADO".equals(anuncio.getStatus()) && versao == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reabra a confirmação de exclusão: o anúncio foi alterado.");
        }
        var agora = java.time.LocalDateTime.now(relogio);
        usuario.registrarExclusao(anuncio.getPublicadoEm(), agora);
        String aviso = (anuncio.getPublicadoEm() == null ? "Este rascunho nunca foi publicado e não conta como exclusão rápida. " : "") + usuario.avisoPublicacao(agora);
        fotos.deleteByAnuncioId(id);
        // Remove tambem a entidade gerenciada; o FK em cascata protege a integridade no banco.
        avaliacoes.deleteByAnuncioId(id);
        evidencias.deleteByAnuncioId(id);
        anuncios.delete(anuncio);
        anuncios.flush();
        return aviso;
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
