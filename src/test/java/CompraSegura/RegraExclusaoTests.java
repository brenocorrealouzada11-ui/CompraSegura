package CompraSegura;
import CompraSegura.usuario.Usuario;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class RegraExclusaoTests {
    @Test void publicacaoNaoArredondaHorarioParaOFuturoNoMysql() {
        var agora = LocalDateTime.of(2026, 9, 29, 12, 0, 0, 900_000_000);
        var u = new Usuario("Teste", "teste@example.invalid", "hash");
        var a = new CompraSegura.anuncio.Anuncio(u, null, "Teste", "", java.math.BigDecimal.ONE);
        a.publicar(agora);
        assertThat(a.getPublicadoEm()).isEqualTo(agora.withNano(0));
        u.registrarExclusao(a.getPublicadoEm(), agora.plusNanos(1));
        assertThat(u.getExclusoesRapidas()).isEqualTo(1);
    }
    @Test void terceiraExclusaoRapidaBloqueiaPorVinteQuatroHorasEAposPrazoReinicia() {
        var u = new Usuario("Teste", "teste@example.invalid", "hash");
        var agora = LocalDateTime.of(2026, 9, 29, 12, 0);
        u.registrarExclusao(agora.minusMinutes(5), agora);
        u.registrarExclusao(agora.minusMinutes(1), agora);
        assertThat(u.publicacaoBloqueada(agora)).isFalse();
        assertThat(u.getExclusoesRapidas()).isEqualTo(2);
        u.registrarExclusao(agora.minusMinutes(2), agora);
        assertThat(u.getBloqueadoAte()).isEqualTo(agora.plusHours(24));
        assertThat(u.publicacaoBloqueada(agora.plusHours(24).minusNanos(1))).isTrue();
        u.registrarExclusao(agora, agora.plusMinutes(1));
        assertThat(u.getBloqueadoAte()).isEqualTo(agora.plusHours(24));
        u.expirarBloqueio(agora.plusHours(24));
        assertThat(u.publicacaoBloqueada(agora.plusHours(24))).isFalse();
        assertThat(u.getExclusoesRapidas()).isZero();
    }
    @Test void rascunhoNuncaPublicadoNaoAlteraSequenciaEExclusaoForaDaJanelaReinicia() {
        var u = new Usuario("Teste", "teste@example.invalid", "hash");
        var agora = LocalDateTime.of(2026, 9, 29, 12, 0);
        u.registrarExclusao(agora, agora);
        u.registrarExclusao(null, agora);
        assertThat(u.getExclusoesRapidas()).isEqualTo(1);
        u.registrarExclusao(agora.minusMinutes(5).minusSeconds(1), agora);
        assertThat(u.getExclusoesRapidas()).isZero();
    }
}
