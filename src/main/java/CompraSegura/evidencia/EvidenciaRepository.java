package CompraSegura.evidencia;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EvidenciaRepository extends JpaRepository<EvidenciaIMEI, Long> {
    Optional<EvidenciaIMEI> findByAnuncioIdAndAnuncioVendedorId(Long anuncioId, Long vendedorId);
    void deleteByAnuncioId(Long anuncioId);

    // Metadados sem carregar a imagem na pagina de detalhes.
    @Query("select new CompraSegura.evidencia.EvidenciaResumo(e.tipoConteudo, e.tamanhoBytes, e.enviadaEm) "
        + "from EvidenciaIMEI e where e.anuncio.id = :anuncioId and e.anuncio.vendedor.id = :vendedorId")
    Optional<EvidenciaResumo> resumo(Long anuncioId, Long vendedorId);
}
