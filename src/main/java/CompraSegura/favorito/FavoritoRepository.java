package CompraSegura.favorito;

import java.util.Optional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

public interface FavoritoRepository extends JpaRepository<Favorito, Long> {
    boolean existsByUsuarioIdAndAnuncioId(Long usuarioId, Long anuncioId);
    Optional<Favorito> findByIdAndUsuarioId(Long id, Long usuarioId);
    @EntityGraph(attributePaths = {"anuncio", "anuncio.vendedor"})
    Page<Favorito> findByUsuarioId(Long usuarioId, Pageable pagina);
}
