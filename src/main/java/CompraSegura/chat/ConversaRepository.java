package CompraSegura.chat;

import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

public interface ConversaRepository extends JpaRepository<Conversa, Long> {
    Optional<Conversa> findByAnuncioIdAndCompradorId(Long anuncioId, Long compradorId);
    @EntityGraph(attributePaths = {"comprador", "vendedor", "anuncio"})
    @Query("select c from Conversa c where c.comprador.id = :usuarioId or c.vendedor.id = :usuarioId")
    Page<Conversa> listar(Long usuarioId, Pageable pagina);
    @EntityGraph(attributePaths = {"comprador", "vendedor", "anuncio"})
    @Query("select c from Conversa c where c.id = :id and (c.comprador.id = :usuarioId or c.vendedor.id = :usuarioId)")
    Optional<Conversa> autorizada(Long id, Long usuarioId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Conversa c where c.id = :id and (c.comprador.id = :usuarioId or c.vendedor.id = :usuarioId)")
    Optional<Conversa> autorizadaParaAtualizacao(Long id, Long usuarioId);
}
