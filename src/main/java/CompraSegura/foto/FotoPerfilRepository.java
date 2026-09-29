package CompraSegura.foto;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface FotoPerfilRepository extends JpaRepository<FotoPerfil, Long> {
    boolean existsByUsuarioId(Long id);
    Optional<FotoPerfil> findByUsuarioId(Long id);
    void deleteByUsuarioId(Long id);
}
