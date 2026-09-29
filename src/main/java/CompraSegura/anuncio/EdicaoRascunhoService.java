package CompraSegura.anuncio;

import CompraSegura.confiabilidade.*;
import CompraSegura.evidencia.*;
import CompraSegura.usuario.*;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service @Validated
public class EdicaoRascunhoService {
    private final AnuncioRepository anuncios;
    private final UsuarioRepository usuarios;
    private final EvidenciaService evidencias;
    private final ValidadorImagemEvidencia imagens;
    private final AvaliacaoRepository avaliacoes;
    private final VerificacaoService verificacoes;
    public EdicaoRascunhoService(AnuncioRepository anuncios, UsuarioRepository usuarios, EvidenciaService evidencias,
                                ValidadorImagemEvidencia imagens, AvaliacaoRepository avaliacoes, VerificacaoService verificacoes) {
        this.anuncios = anuncios; this.usuarios = usuarios; this.evidencias = evidencias;
        this.imagens = imagens; this.avaliacoes = avaliacoes; this.verificacoes = verificacoes;
    }
    private Anuncio autorizado(Long id, UsuarioAutenticado principal, boolean bloquear) {
        var usuario = usuarios.findById(principal.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (usuario.getVersaoCredenciais() != principal.getVersaoCredenciais()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        var anuncio = (bloquear ? anuncios.findAutorizadoParaAtualizacao(id, principal.getId()) : anuncios.findByIdAndVendedorId(id, principal.getId()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!"RASCUNHO".equals(anuncio.getStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Somente rascunhos podem ser editados.");
        return anuncio;
    }
    private void conferirVersao(Anuncio anuncio, EdicaoRascunhoForm form) {
        if (!Objects.equals(form.getVersao(), anuncio.getVersao())) throw new EdicaoDesatualizadaException();
    }
    @Transactional(readOnly = true)
    public EdicaoRascunhoForm carregar(Long id, UsuarioAutenticado principal) {
        var anuncio = autorizado(id, principal, false);
        var aparelho = anuncio.getAparelho();
        var form = new EdicaoRascunhoForm();
        form.setVersao(anuncio.getVersao()); form.setTitulo(anuncio.getTitulo()); form.setPreco(anuncio.getPreco()); form.setDescricao(anuncio.getDescricao());
        form.setTipo(aparelho.getTipo()); form.setMarca(aparelho.getModelo().getMarca()); form.setModelo(aparelho.getModelo().getNome());
        form.setCondicao(Arrays.stream(EstadoConservacao.values()).filter(v -> v.getDescricao().equals(aparelho.getCondicao())).findFirst().orElse(null));
        form.setReparos(Arrays.stream(HistoricoReparos.values()).filter(v -> v.getDescricao().equals(aparelho.getAlteracoes())).findFirst().orElse(null));
        if (form.getCondicao() == null) form.setCondicaoAnterior(aparelho.getCondicao());
        if (form.getReparos() == null) form.setReparosAnteriores(aparelho.getAlteracoes());
        form.setImeis(String.join("\n", aparelho.getImeis().stream().map(IMEI::getNumero).toList()));
        return form;
    }
    private List<String> numeros(AnuncioForm form) {
        try { return NormalizadorIMEIs.normalizar(form.getImeis()); }
        catch (IllegalArgumentException ex) { throw new PreparacaoAnuncioException("imeis", ex.getMessage()); }
    }
    private ImagemEvidencia imagem(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) return null;
        try { return imagens.validar(arquivo); }
        catch (IllegalArgumentException ex) { throw new PreparacaoAnuncioException("arquivo", ex.getMessage()); }
    }
    @Transactional(readOnly = true)
    public RelatorioVerificacao previa(Long id, UsuarioAutenticado principal, @Valid EdicaoRascunhoForm form, MultipartFile arquivo) {
        conferirVersao(autorizado(id, principal, false), form);
        var numeros = numeros(form);
        boolean temImagem = imagem(arquivo) != null || evidencias.resumo(id, principal.getId()) != null;
        return verificacoes.avaliar(numeros, form.getCondicao().getDescricao(), form.getReparos().getDescricao(), form.getDescricao(), temImagem).relatorio();
    }
    @Transactional
    public boolean salvar(Long id, UsuarioAutenticado principal, @Valid EdicaoRascunhoForm form, MultipartFile arquivo) {
        var anuncio = autorizado(id, principal, true);
        conferirVersao(anuncio, form);
        var numeros = numeros(form);
        var imagem = imagem(arquivo); // Rejeita arquivo invalido antes de alterar qualquer dado.
        var aparelho = anuncio.getAparelho();
        var anteriores = aparelho.getImeis().stream().map(IMEI::getNumero).toList();
        boolean dadosRelevantes = aparelho.getTipo() != form.getTipo()
            || !aparelho.getModelo().getMarca().equals(form.getMarca()) || !aparelho.getModelo().getNome().equals(form.getModelo())
            || !aparelho.getCondicao().equals(form.getCondicao().getDescricao()) || !aparelho.getAlteracoes().equals(form.getReparos().getDescricao())
            || !anuncio.getDescricao().equals(form.getDescricao()) || !new HashSet<>(anteriores).equals(new HashSet<>(numeros));
        if (!anteriores.equals(numeros)) {
            // Remove antes de inserir para permitir trocar a ordem sem violar o indice unico.
            aparelho.limparImeis();
            anuncios.flush();
            aparelho.adicionarImeis(numeros);
        }
        aparelho.atualizar(form.getTipo(), new ModeloAparelho(form.getMarca(), form.getModelo()), form.getCondicao().getDescricao(), form.getReparos().getDescricao());
        anuncio.atualizar(form.getTitulo(), form.getDescricao(), form.getPreco());
        if (imagem != null) evidencias.salvarImagem(id, principal, imagem);
        if (dadosRelevantes) avaliacoes.findByAnuncioIdAndAtualTrue(id).forEach(RegistroAvaliacao::desatualizar);
        anuncios.flush();
        return dadosRelevantes || imagem != null;
    }
}
