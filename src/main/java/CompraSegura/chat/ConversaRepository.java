package CompraSegura.chat;

import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

public interface ConversaRepository extends JpaRepository<Conversa, Long> {
    @Query("""
        select (count(c) > 0) from Conversa c where c.vendedor.id = :vendedorId and c.comprador.id = :autorId
        and exists (select m.id from Mensagem m where m.conversa.id = c.id and m.remetenteId = :autorId)
        and exists (select m.id from Mensagem m where m.conversa.id = c.id and m.remetenteId = :vendedorId)
        """)
    boolean houveTrocaDeMensagens(Long vendedorId, Long autorId);
    @EntityGraph(attributePaths = {"comprador", "vendedor", "anuncio"})
    @Query("""
        select c from Conversa c left join c.anuncio a
        where (c.comprador.id = :usuarioId or c.vendedor.id = :usuarioId)
        and (:papel = 'TODOS' or (:papel = 'COMPRANDO' and c.comprador.id = :usuarioId) or (:papel = 'VENDENDO' and c.vendedor.id = :usuarioId))
        and (:situacao = 'TODAS' or (:situacao = 'EM_ANDAMENTO' and a.status = 'PUBLICADO')
          or (:situacao = 'ANUNCIO_VENDIDO' and a.status = 'VENDIDO')
          or (:situacao = 'INDISPONIVEL' and (a.id is null or a.status = 'RASCUNHO')))
        """)
    Page<Conversa> negociacoes(Long usuarioId, String papel, String situacao, Pageable pagina);
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
