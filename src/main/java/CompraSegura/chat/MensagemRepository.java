package CompraSegura.chat;

import java.util.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;

public interface MensagemRepository extends JpaRepository<Mensagem, Long> {
    List<Mensagem> findByConversaIdAndIdLessThanOrderByIdDesc(Long conversaId, Long antes, Pageable pagina);
    List<Mensagem> findByConversaIdAndIdGreaterThanOrderByIdAsc(Long conversaId, Long apos, Pageable pagina);
    Optional<Mensagem> findByConversaIdAndRemetenteIdAndChaveEnvio(Long conversaId, Long remetenteId, String chaveEnvio);
    boolean existsByIdAndConversaId(Long id, Long conversaId);
    long countByConversaIdAndRemetenteIdNotAndIdGreaterThan(Long conversaId, Long usuarioId, long lidaAte);
    @Query("""
        select count(m) from Mensagem m join m.conversa c
        where m.remetenteId <> :usuarioId and
        ((c.comprador.id = :usuarioId and m.id > c.lidaComprador)
        or (c.vendedor.id = :usuarioId and m.id > c.lidaVendedor))
        """)
    long naoLidas(Long usuarioId);
}
