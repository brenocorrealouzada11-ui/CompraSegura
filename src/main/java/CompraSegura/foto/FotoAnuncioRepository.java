package CompraSegura.foto;
import java.util.*;
import org.springframework.data.jpa.repository.*;
public interface FotoAnuncioRepository extends JpaRepository<FotoAnuncio, Long> {
    @Query("select f.id from FotoAnuncio f where f.anuncio.id = :id order by f.id")
    List<Long> ids(Long id);
    @Query("select min(f.id) from FotoAnuncio f where f.anuncio.id = :id")
    Long capa(Long id);
    long countByAnuncioId(Long id);
    Optional<FotoAnuncio> findByIdAndAnuncioId(Long id, Long anuncioId);
    void deleteByAnuncioId(Long id);
}
