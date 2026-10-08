package CompraSegura.catalogo;

import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ComparacaoService {
    private final CatalogoService catalogo;
    public ComparacaoService(CatalogoService catalogo) { this.catalogo = catalogo; }
    @Transactional(readOnly = true)
    public List<CatalogoService.Detalhes> comparar(List<String> valores) {
        if (valores == null || valores.size() < 2 || valores.size() > 3)
            throw new IllegalArgumentException("Selecione dois ou três anúncios diferentes para comparar.");
        var ids = new LinkedHashSet<Long>();
        for (String valor : valores) {
            try {
                if (valor == null || !valor.matches("[1-9][0-9]{0,18}")) throw new NumberFormatException();
                if (!ids.add(Long.parseLong(valor))) throw new IllegalArgumentException("Selecione anúncios diferentes para comparar.");
            } catch (NumberFormatException ex) { throw new IllegalArgumentException("A seleção contém um anúncio inválido. Selecione novamente em Explorar."); }
        }
        var itens = new ArrayList<CatalogoService.Detalhes>();
        for (Long id : ids) {
            // Reutiliza somente a projeção pública: rascunhos e evidências privadas não entram.
            try { itens.add(catalogo.detalhes(id)); }
            catch (ResponseStatusException ex) {
                if (ex.getStatusCode().value() != 404) throw ex;
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Um dos anúncios não está mais disponível. Volte à seleção e escolha outros aparelhos.");
            }
        }
        return List.copyOf(itens);
    }
}
