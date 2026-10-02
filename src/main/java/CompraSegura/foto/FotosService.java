package CompraSegura.foto;
import CompraSegura.anuncio.AnuncioRepository;
import CompraSegura.evidencia.*;
import CompraSegura.usuario.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
@Service
public class FotosService {
    private final FotoPerfilRepository perfis;
    private final FotoAnuncioRepository fotos;
    private final AnuncioRepository anuncios;
    private final UsuarioRepository usuarios;
    private final ValidadorImagemEvidencia validador;
    public FotosService(FotoPerfilRepository perfis, FotoAnuncioRepository fotos, AnuncioRepository anuncios, UsuarioRepository usuarios, ValidadorImagemEvidencia validador) {
        this.perfis = perfis; this.fotos = fotos; this.anuncios = anuncios; this.usuarios = usuarios; this.validador = validador;
    }
    @Transactional
    public void perfil(UsuarioAutenticado principal, MultipartFile arquivo, boolean remover) {
        var usuario = usuarios.findParaAtualizacao(principal.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (usuario.getVersaoCredenciais() != principal.getVersaoCredenciais()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        if (remover) { perfis.deleteByUsuarioId(usuario.getId()); return; }
        if (arquivo == null || arquivo.isEmpty()) throw new IllegalArgumentException("Escolha uma foto de perfil.");
        var imagem = validador.validar(arquivo);
        var foto = perfis.findByUsuarioId(usuario.getId()).orElse(null);
        if (foto == null) foto = new FotoPerfil(usuario, imagem); else foto.atualizar(imagem);
        perfis.saveAndFlush(foto);
    }
    @Transactional(readOnly = true)
    public ImagemEvidencia aparelho(Long anuncioId, Long fotoId, Long donoId) {
        var anuncio = donoId == null ? anuncios.findByIdAndStatusIn(anuncioId, java.util.List.of("PUBLICADO", "VENDIDO")) : anuncios.findByIdAndVendedorId(anuncioId, donoId);
        anuncio.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return fotos.findByIdAndAnuncioId(fotoId, anuncioId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)).imagem();
    }
    @Transactional(readOnly = true)
    public ImagemEvidencia perfil(Long id, boolean privado) {
        if (!privado && !anuncios.existsByVendedorIdAndStatusIn(id, java.util.List.of("PUBLICADO", "VENDIDO"))) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return perfis.findByUsuarioId(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)).imagem();
    }
}
