package CompraSegura.confiabilidade;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AvaliacaoRepository extends JpaRepository<RegistroAvaliacao, Long> {
    Optional<RegistroAvaliacao> findFirstByAnuncioIdAndAnuncioVendedorIdOrderByIdDesc(Long anuncioId, Long vendedorId);
    List<RegistroAvaliacao> findByAnuncioIdAndAtualTrue(Long anuncioId);
    void deleteByAnuncioId(Long anuncioId);
}
