package CompraSegura.foto;
import CompraSegura.anuncio.*;
import CompraSegura.evidencia.*;
import CompraSegura.usuario.UsuarioAutenticado;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
@Service
public class FotosAnuncioService {
    private final FotoAnuncioRepository fotos;
    private final AnuncioRepository anuncios;
    private final ValidadorImagemEvidencia imagens;
    private final PreparacaoAnuncioService preparacao;
    private final EdicaoRascunhoService edicao;
    public FotosAnuncioService(FotoAnuncioRepository fotos, AnuncioRepository anuncios, ValidadorImagemEvidencia imagens, PreparacaoAnuncioService preparacao, EdicaoRascunhoService edicao) {
        this.fotos = fotos; this.anuncios = anuncios; this.imagens = imagens; this.preparacao = preparacao; this.edicao = edicao;
    }
    private List<ImagemEvidencia> validar(List<MultipartFile> arquivos, int mantidas) {
        var selecionados = arquivos == null ? List.<MultipartFile>of() : arquivos.stream().filter(f -> !f.isEmpty()).toList();
        if (mantidas + selecionados.size() < 1) throw new PreparacaoAnuncioException("fotos", "Envie pelo menos uma foto do aparelho.");
        if (mantidas + selecionados.size() > 6) throw new PreparacaoAnuncioException("fotos", "Cada anúncio pode ter no máximo 6 fotos.");
        try { return selecionados.stream().map(imagens::validar).toList(); }
        catch (IllegalArgumentException ex) { throw new PreparacaoAnuncioException("fotos", ex.getMessage()); }
    }
    @Transactional
    public Long criar(UsuarioAutenticado principal, AnuncioForm form, MultipartFile evidencia, List<MultipartFile> arquivos) {
        var validadas = validar(arquivos, 0);
        Long id = preparacao.salvar(principal, form, evidencia);
        var anuncio = anuncios.findById(id).orElseThrow();
        validadas.forEach(imagem -> fotos.save(new FotoAnuncio(anuncio, imagem)));
        fotos.flush(); return id;
    }
    @Transactional
    public boolean editar(Long id, UsuarioAutenticado principal, EdicaoRascunhoForm form, MultipartFile evidencia, List<MultipartFile> arquivos, List<Long> remover) {
        // Autoriza antes de ler IDs privados; a edicao abaixo bloqueia e confere a versao.
        edicao.carregar(id, principal);
        var atuais = fotos.ids(id);
        var exclusoes = new HashSet<>(remover == null ? List.<Long>of() : remover);
        if (!atuais.containsAll(exclusoes)) throw new PreparacaoAnuncioException("fotos", "Reabra a edição para conferir as fotos deste anúncio.");
        var validadas = validar(arquivos, atuais.size() - exclusoes.size());
        boolean reavaliar = edicao.salvar(id, principal, form, evidencia);
        exclusoes.forEach(fotos::deleteById);
        var anuncio = anuncios.findById(id).orElseThrow();
        validadas.forEach(imagem -> fotos.save(new FotoAnuncio(anuncio, imagem)));
        fotos.flush(); return reavaliar;
    }
}
