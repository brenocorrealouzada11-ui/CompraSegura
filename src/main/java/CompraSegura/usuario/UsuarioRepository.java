package CompraSegura.usuario;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from Usuario u where u.id = :id")
    Optional<Usuario> findParaAtualizacao(Long id);
    @org.springframework.data.jpa.repository.Query("""
        select u from Usuario u where lower(u.nome) like :nome escape '!'
        and (exists (select a.id from Anuncio a where a.vendedor.id = u.id and a.status in ('PUBLICADO', 'VENDIDO'))
        or exists (select r.id from AvaliacaoVendedor r where r.vendedor.id = u.id))
        """)
    org.springframework.data.domain.Page<Usuario> pesquisarVendedores(String nome, org.springframework.data.domain.Pageable pagina);
    @org.springframework.data.jpa.repository.Query("""
        select (count(u) > 0) from Usuario u where u.id = :id and
        (exists (select a.id from Anuncio a where a.vendedor.id = u.id and a.status in ('PUBLICADO', 'VENDIDO'))
        or exists (select r.id from AvaliacaoVendedor r where r.vendedor.id = u.id))
        """)
    boolean perfilPublico(Long id);
    boolean existsByEmail(String email);
    Optional<Usuario> findByEmail(String email);
}
