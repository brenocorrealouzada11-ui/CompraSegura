package CompraSegura.reputacao;

import java.util.Optional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

public interface AvaliacaoVendedorRepository extends JpaRepository<AvaliacaoVendedor, Long> {
    Optional<AvaliacaoVendedor> findByVendedorIdAndAutorId(Long vendedorId, Long autorId);
    @EntityGraph(attributePaths = "autor")
    Page<AvaliacaoVendedor> findByVendedorId(Long vendedorId, Pageable pagina);
    interface Estatistica { long getTotal(); Double getMedia(); }
    @Query("select count(a) as total, avg(a.nota) as media from AvaliacaoVendedor a where a.vendedor.id = :id")
    Estatistica estatistica(Long id);
}
