package CompraSegura.anuncio;

import CompraSegura.usuario.*;
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
    public AnuncioService(AnuncioRepository anuncios, UsuarioRepository usuarios) { this.anuncios = anuncios; this.usuarios = usuarios; }

    @Transactional
    public Long criar(UsuarioAutenticado principal, @Valid AnuncioForm form) {
        String normalizados = form.getImeis().replaceAll("(?<![0-9])([0-9]{8})[- ]([0-9]{6})[- ]([0-9])(?![0-9])", "$1$2$3");
        var numeros = Arrays.stream(normalizados.split("[,;\\s]+" )).filter(s -> !s.isBlank()).toList();
        if (numeros.isEmpty() || numeros.stream().anyMatch(n -> !n.matches("[0-9]{15}"))) {
            throw new IllegalArgumentException("Cada IMEI deve conter 15 dígitos. Use apenas números ou o formato 12345678-901234-5.");
        }
        if (new HashSet<>(numeros).size() != numeros.size()) {
            throw new IllegalArgumentException("Há IMEIs repetidos. Informe cada identificador apenas uma vez.");
        }
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
        var anuncio = anuncios.findByIdAndVendedorId(id, principal.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!"RASCUNHO".equals(anuncio.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Somente rascunhos podem ser excluídos por esta ação.");
        }
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
