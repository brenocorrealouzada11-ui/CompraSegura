package CompraSegura.anuncio;

import CompraSegura.confiabilidade.*;
import CompraSegura.evidencia.*;
import CompraSegura.usuario.UsuarioAutenticado;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

@Service @Validated
public class PreparacaoAnuncioService {
    private final AnuncioService anuncios;
    private final EvidenciaService evidencias;
    private final ValidadorImagemEvidencia imagens;
    private final VerificacaoService verificacoes;
    public PreparacaoAnuncioService(AnuncioService anuncios, EvidenciaService evidencias,
                                    ValidadorImagemEvidencia imagens, VerificacaoService verificacoes) {
        this.anuncios = anuncios; this.evidencias = evidencias; this.imagens = imagens; this.verificacoes = verificacoes;
    }
    private record Preparacao(ImagemEvidencia imagem, VerificacaoPreparada verificacao) { }

    private Preparacao preparar(AnuncioForm form, MultipartFile arquivo) {
        java.util.List<String> imeis;
        try { imeis = NormalizadorIMEIs.normalizar(form.getImeis()); }
        catch (IllegalArgumentException ex) { throw new PreparacaoAnuncioException("imeis", ex.getMessage()); }
        ImagemEvidencia imagem = null;
        if (arquivo != null && !arquivo.isEmpty()) {
            try { imagem = imagens.validar(arquivo); }
            catch (IllegalArgumentException ex) { throw new PreparacaoAnuncioException("arquivo", ex.getMessage()); }
        }
        var verificacao = verificacoes.avaliar(imeis, form.getCondicao().getDescricao(), form.getReparos().getDescricao(), form.getDescricao(), imagem != null);
        return new Preparacao(imagem, verificacao);
    }

    // Previa sem escrita no banco e sem guardar imagem ou resultados na sessao.
    public RelatorioVerificacao previa(@Valid AnuncioForm form, MultipartFile arquivo) {
        return preparar(form, arquivo).verificacao().relatorio();
    }

    @Transactional
    public Long salvar(UsuarioAutenticado principal, @Valid AnuncioForm form, MultipartFile arquivo) {
        // Recalcula com os dados enviados: o cliente nunca fornece um resultado confiavel.
        var preparacao = preparar(form, arquivo);
        Long id = anuncios.criar(principal, form);
        if (preparacao.imagem() != null) evidencias.salvarImagem(id, principal, preparacao.imagem());
        verificacoes.registrar(id, principal.getId(), preparacao.verificacao());
        return id;
    }
}
