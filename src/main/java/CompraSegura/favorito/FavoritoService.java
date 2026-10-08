package CompraSegura.favorito;

import CompraSegura.anuncio.AnuncioRepository;
import CompraSegura.foto.FotoAnuncioRepository;
import CompraSegura.usuario.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.text.NumberFormat;
import java.util.Locale;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FavoritoService {
    private final FavoritoRepository favoritos;
    private final UsuarioRepository usuarios;
    private final AnuncioRepository anuncios;
    private final FotoAnuncioRepository fotos;
    private final Clock relogio;
    public FavoritoService(FavoritoRepository favoritos, UsuarioRepository usuarios, AnuncioRepository anuncios, FotoAnuncioRepository fotos, Clock relogio) {
        this.favoritos = favoritos; this.usuarios = usuarios; this.anuncios = anuncios; this.fotos = fotos; this.relogio = relogio;
    }
    private Usuario autenticar(UsuarioAutenticado principal, boolean bloquear) {
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        var u = (bloquear ? usuarios.findParaAtualizacao(principal.getId()) : usuarios.findById(principal.getId()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (u.getVersaoCredenciais() != principal.getVersaoCredenciais()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return u;
    }
    @Transactional(readOnly = true)
    public boolean salvo(Long anuncioId, UsuarioAutenticado principal) {
        if (principal == null) return false;
        autenticar(principal, false);
        return favoritos.existsByUsuarioIdAndAnuncioId(principal.getId(), anuncioId);
    }
    @Transactional
    public void adicionar(Long anuncioId, UsuarioAutenticado principal) {
        var usuario = autenticar(principal, true);
        var anuncio = anuncios.publicadoParaConversa(anuncioId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (anuncio.getVendedor().getId().equals(usuario.getId())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Seus anúncios já estão em Meus anúncios.");
        if (!favoritos.existsByUsuarioIdAndAnuncioId(usuario.getId(), anuncioId))
            favoritos.saveAndFlush(new Favorito(usuario, anuncio, LocalDateTime.now(relogio)));
    }
    @Transactional
    public void remover(Long id, UsuarioAutenticado principal) {
        autenticar(principal, true);
        var favorito = favoritos.findByIdAndUsuarioId(id, principal.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        favoritos.delete(favorito); favoritos.flush();
    }
    public record Item(Long id, Long anuncioId, String titulo, String situacao, String preco, String vendedor, Long foto, String salvoEm) { }
    @Transactional(readOnly = true)
    public Page<Item> listar(UsuarioAutenticado principal, int pagina) {
        autenticar(principal, false);
        return favoritos.findByUsuarioId(principal.getId(), PageRequest.of(Math.max(0, Math.min(10000, pagina)), 12, Sort.by("salvoEm", "id").descending())).map(f -> {
            var a = f.getAnuncio();
            boolean visivel = a != null && ("PUBLICADO".equals(a.getStatus()) || "VENDIDO".equals(a.getStatus()));
            var imagens = visivel ? fotos.ids(a.getId()) : java.util.List.<Long>of();
            return new Item(f.getId(), visivel ? a.getId() : null, visivel ? a.getTitulo() : f.getTituloSalvo(),
                !visivel ? "Indisponível" : "VENDIDO".equals(a.getStatus()) ? "Vendido" : "Disponível",
                visivel ? NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(a.getPreco()) : null,
                visivel ? a.getVendedor().getNome() : null, imagens.isEmpty() ? null : imagens.get(0),
                f.getSalvoEm().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        });
    }
}
