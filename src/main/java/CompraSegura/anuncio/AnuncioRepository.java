package CompraSegura.anuncio;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnuncioRepository extends JpaRepository<Anuncio, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Anuncio> {
    @org.springframework.data.jpa.repository.Query("select a.vendedor.id from Anuncio a where a.id = :id and a.status = 'PUBLICADO'")
    Optional<Long> vendedorPublicado(Long id);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select a from Anuncio a where a.id = :id and a.status = 'PUBLICADO'")
    Optional<Anuncio> publicadoParaConversa(Long id);
    @Override
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"aparelho", "vendedor"})
    Page<Anuncio> findAll(org.springframework.data.jpa.domain.Specification<Anuncio> filtro, Pageable pagina);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"aparelho", "vendedor"})
    Optional<Anuncio> findByIdAndStatus(Long id, String status);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select a from Anuncio a where a.id = :id and a.vendedor.id = :vendedorId")
    Optional<Anuncio> findAutorizadoParaAtualizacao(Long id, Long vendedorId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"aparelho", "vendedor"})
    Optional<Anuncio> findByIdAndStatusIn(Long id, java.util.Collection<String> status);
    boolean existsByVendedorIdAndStatusIn(Long vendedorId, java.util.Collection<String> status);
    long countByVendedorIdAndStatus(Long vendedorId, String status);
    boolean existsByVendedorIdAndStatus(Long vendedorId, String status);
    long countByVendedorId(Long vendedorId);
    Page<Anuncio> findByVendedorIdOrderByIdDesc(Long vendedorId, Pageable pagina);
    Optional<Anuncio> findByIdAndVendedorId(Long id, Long vendedorId);
}
