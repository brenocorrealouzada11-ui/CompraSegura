package CompraSegura.catalogo;

import CompraSegura.anuncio.AnuncioRepository;
import CompraSegura.foto.FotoPerfilRepository;
import CompraSegura.usuario.UsuarioRepository;
import java.util.Locale;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional(readOnly = true)
public class PesquisaVendedorService {
    private final UsuarioRepository usuarios;
    private final AnuncioRepository anuncios;
    private final FotoPerfilRepository fotos;
    private final CompraSegura.reputacao.AvaliacaoVendedorRepository avaliacoes;
    public PesquisaVendedorService(UsuarioRepository usuarios, AnuncioRepository anuncios, FotoPerfilRepository fotos, CompraSegura.reputacao.AvaliacaoVendedorRepository avaliacoes) {
        this.usuarios = usuarios; this.anuncios = anuncios; this.fotos = fotos; this.avaliacoes = avaliacoes;
    }
    public record Vendedor(Long id, String nome, boolean foto, long disponiveis, long vendidos, CompraSegura.reputacao.AvaliacaoPerfilService.Resumo avaliacao) { }
    public Page<Vendedor> pesquisar(FiltroVendedor filtro) {
        String nome = "%" + filtro.getNome().toLowerCase(Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        return usuarios.pesquisarVendedores(nome, PageRequest.of(Math.max(0, Math.min(10000, filtro.getPagina())), 12, Sort.by("nome", "id")))
            .map(u -> new Vendedor(u.getId(), u.getNome(), fotos.existsByUsuarioId(u.getId()),
                anuncios.countByVendedorIdAndStatus(u.getId(), "PUBLICADO"), anuncios.countByVendedorIdAndStatus(u.getId(), "VENDIDO"), CompraSegura.reputacao.AvaliacaoPerfilService.resumo(avaliacoes.estatistica(u.getId()))));
    }
}
