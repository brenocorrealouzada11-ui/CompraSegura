package CompraSegura.anuncio;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnuncioRepository extends JpaRepository<Anuncio, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select a from Anuncio a where a.id = :id and a.vendedor.id = :vendedorId")
    Optional<Anuncio> findAutorizadoParaAtualizacao(Long id, Long vendedorId);

    long countByVendedorId(Long vendedorId);
    Page<Anuncio> findByVendedorIdOrderByIdDesc(Long vendedorId, Pageable pagina);
    Optional<Anuncio> findByIdAndVendedorId(Long id, Long vendedorId);
}
