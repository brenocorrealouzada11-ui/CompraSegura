package CompraSegura.evidencia;

import CompraSegura.anuncio.AnuncioRepository;
import CompraSegura.anuncio.Anuncio;
import CompraSegura.confiabilidade.AvaliacaoRepository;
import CompraSegura.confiabilidade.RegistroAvaliacao;
import CompraSegura.usuario.UsuarioAutenticado;
import CompraSegura.usuario.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EvidenciaService {
    private final AnuncioRepository anuncios;
    private final EvidenciaRepository evidencias;
    private final UsuarioRepository usuarios;
    private final ValidadorImagemEvidencia validador;
    private final AvaliacaoRepository avaliacoes;

    public EvidenciaService(AnuncioRepository anuncios, EvidenciaRepository evidencias,
                           UsuarioRepository usuarios, ValidadorImagemEvidencia validador, AvaliacaoRepository avaliacoes) {
        this.anuncios = anuncios; this.evidencias = evidencias;
        this.usuarios = usuarios; this.validador = validador; this.avaliacoes = avaliacoes;
    }

    @Transactional
    public void enviar(Long anuncioId, UsuarioAutenticado principal, MultipartFile arquivo) {
        var anuncio = obterRascunho(anuncioId, principal);
        salvar(anuncio, principal, validador.validar(arquivo));
    }

    @Transactional
    public void salvarImagem(Long anuncioId, UsuarioAutenticado principal, ImagemEvidencia imagem) {
        salvar(obterRascunho(anuncioId, principal), principal, imagem);
    }

    private Anuncio obterRascunho(Long anuncioId, UsuarioAutenticado principal) {
        var usuario = usuarios.findById(principal.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (usuario.getVersaoCredenciais() != principal.getVersaoCredenciais()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        var anuncio = anuncios.findAutorizadoParaAtualizacao(anuncioId, principal.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!"RASCUNHO".equals(anuncio.getStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT);
        return anuncio;
    }

    private void salvar(Anuncio anuncio, UsuarioAutenticado principal, ImagemEvidencia imagem) {
        Long anuncioId = anuncio.getId();
        var evidencia = evidencias.findByAnuncioIdAndAnuncioVendedorId(anuncioId, principal.getId()).orElse(null);
        if (evidencia == null) evidencia = new EvidenciaIMEI(anuncio, imagem);
        else evidencia.atualizar(imagem);
        evidencias.saveAndFlush(evidencia);
        avaliacoes.findByAnuncioIdAndAtualTrue(anuncioId).forEach(RegistroAvaliacao::desatualizar);
    }

    @Transactional(readOnly = true)
    public EvidenciaResumo resumo(Long anuncioId, Long vendedorId) {
        return evidencias.resumo(anuncioId, vendedorId).orElse(null);
    }

    @Transactional(readOnly = true)
    public ImagemEvidencia carregar(Long anuncioId, Long vendedorId) {
        var evidencia = evidencias.findByAnuncioIdAndAnuncioVendedorId(anuncioId, vendedorId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return new ImagemEvidencia(evidencia.getConteudo(), evidencia.getTipoConteudo());
    }
}
