package CompraSegura.anuncio;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnuncioRepository extends JpaRepository<Anuncio, Long> {
    long countByVendedorId(Long vendedorId);
    Page<Anuncio> findByVendedorIdOrderByIdDesc(Long vendedorId, Pageable pagina);
    Optional<Anuncio> findByIdAndVendedorId(Long id, Long vendedorId);
}
