package CompraSegura.catalogo;

import CompraSegura.anuncio.*;
import CompraSegura.confiabilidade.VerificacaoService;
import CompraSegura.usuario.UsuarioRepository;
import java.util.*;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service @Transactional(readOnly = true)
public class CatalogoService {
    private final AnuncioRepository anuncios;
    private final UsuarioRepository usuarios;
    private final VerificacaoService verificacoes;
    private final CompraSegura.foto.FotoAnuncioRepository fotos;
    private final CompraSegura.foto.FotoPerfilRepository perfis;
    public CatalogoService(AnuncioRepository anuncios, UsuarioRepository usuarios, VerificacaoService verificacoes, CompraSegura.foto.FotoAnuncioRepository fotos, CompraSegura.foto.FotoPerfilRepository perfis) {
        this.anuncios = anuncios; this.usuarios = usuarios; this.verificacoes = verificacoes; this.fotos = fotos; this.perfis = perfis;
    }
    private AnuncioPublico publico(Anuncio anuncio) { return AnuncioPublico.de(anuncio, fotos.ids(anuncio.getId()), perfis.existsByUsuarioId(anuncio.getVendedor().getId())); }
    private Specification<Anuncio> publicados() { return (r, q, cb) -> cb.equal(r.get("status"), "PUBLICADO"); }
    private Sort ordem(FiltroPesquisa.Ordem ordem) {
        return switch (ordem) {
            case MENOR_PRECO -> Sort.by("preco").ascending().and(Sort.by("id").descending());
            case MAIOR_PRECO -> Sort.by("preco").descending().and(Sort.by("id").descending());
            case RECENTES -> Sort.by("publicadoEm", "id").descending();
        };
    }
    public Page<AnuncioPublico> pesquisar(FiltroPesquisa filtro) {
        Specification<Anuncio> criterios = (r, q, cb) -> {
            var itens = new ArrayList<Predicate>();
            var aparelho = r.get("aparelho");
            if (!filtro.getQ().isBlank()) {
                String termo = "%" + filtro.getQ().toLowerCase(Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
                itens.add(cb.or(cb.like(cb.lower(r.get("titulo")), termo, '!'),
                    cb.like(cb.lower(aparelho.get("modelo").get("marca")), termo, '!'),
                    cb.like(cb.lower(aparelho.get("modelo").get("nome")), termo, '!')));
            }
            if (filtro.getTipo() != null) itens.add(cb.equal(aparelho.get("tipo"), filtro.getTipo()));
            if (filtro.getCondicao() != null) itens.add(cb.equal(aparelho.get("condicao"), filtro.getCondicao().getDescricao()));
            if (filtro.getMinimo() != null) itens.add(cb.greaterThanOrEqualTo(r.get("preco"), filtro.getMinimo()));
            if (filtro.getMaximo() != null) itens.add(cb.lessThanOrEqualTo(r.get("preco"), filtro.getMaximo()));
            return cb.and(itens.toArray(Predicate[]::new));
        };
        return anuncios.findAll(publicados().and(criterios), PageRequest.of(filtro.getPagina(), 12, ordem(filtro.getOrdem()))).map(this::publico);
    }
    public record Detalhes(AnuncioPublico anuncio, RelatorioPublico relatorio) { }
    public Detalhes detalhes(Long id) {
        var anuncio = anuncios.findByIdAndStatus(id, "PUBLICADO").orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return new Detalhes(publico(anuncio), RelatorioPublico.de(verificacoes.ultimo(id, anuncio.getVendedor().getId())));
    }
    public record Historico(Long id, String nome, boolean fotoPerfil, Page<AnuncioPublico> anuncios) { }
    public Historico vendedor(Long id, int pagina) {
        Specification<Anuncio> doVendedor = (r, q, cb) -> cb.equal(r.get("vendedor").get("id"), id);
        var lista = anuncios.findAll(publicados().and(doVendedor), PageRequest.of(Math.max(0, Math.min(pagina, 10000)), 12, ordem(FiltroPesquisa.Ordem.RECENTES)));
        if (lista.getTotalElements() == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        var usuario = usuarios.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return new Historico(id, usuario.getNome(), perfis.existsByUsuarioId(id), lista.map(this::publico));
    }
}
