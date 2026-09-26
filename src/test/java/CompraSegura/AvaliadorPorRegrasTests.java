package CompraSegura;

import CompraSegura.confiabilidade.*;
import CompraSegura.consulta.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static CompraSegura.confiabilidade.AvaliacaoConfiabilidade.NivelRisco.*;

class AvaliadorPorRegrasTests {
    private final AvaliadorPorRegras avaliador = new AvaliadorPorRegras();
    private static final String A="000000000000001", B="000000000000002";
    private ResultadoConsultaIMEI consulta(String imei, SituacaoIMEI situacao, OrigemConsulta origem) {
        return new ResultadoConsultaIMEI(imei, situacao, origem, "Provedor ficticio do teste", Instant.now(), "Teste");
    }
    private ContextoAvaliacao contexto(List<String> imeis, List<ResultadoConsultaIMEI> consultas) {
        return new ContextoAvaliacao(imeis, consultas, ContextoAvaliacao.SituacaoEvidencia.COERENTE,
            "Em boas condições", "Sem reparos ou alterações", "", ContextoAvaliacao.HistoricoVendedor.SEM_REGISTROS);
    }
    @Test void restricaoPrevaleceSobreFalhaDeOutroImei() {
        var resultado = avaliador.avaliar(contexto(List.of(A,B), List.of(
            consulta(A,SituacaoIMEI.INDISPONIVEL,OrigemConsulta.SIMULADA), consulta(B,SituacaoIMEI.COM_RESTRICAO,OrigemConsulta.SIMULADA))));
        assertThat(resultado.risco()).isEqualTo(ALTO);
        assertThat(resultado.pendencias()).anyMatch(p -> p.contains("indisponível"));
        assertThat(resultado.contemSimulacao()).isTrue();
    }
    @Test void simulacaoNaoSeTornaAprovacaoReal() {
        var resultado = avaliador.avaliar(contexto(List.of(A),List.of(consulta(A,SituacaoIMEI.SEM_RESTRICAO,OrigemConsulta.SIMULADA))));
        assertThat(resultado.risco()).isEqualTo(INCONCLUSIVO);
        assertThat(resultado.pendencias()).anyMatch(p -> p.contains("Consultas simuladas"));
    }
    @Test void coberturaIncompletaDuplicadaOuEstranhaNuncaDaBaixoRisco() {
        var regular = consulta(A,SituacaoIMEI.SEM_RESTRICAO,OrigemConsulta.REAL);
        for (var entradas : List.of(List.of(regular), List.of(regular,regular), List.of(consulta(B,SituacaoIMEI.SEM_RESTRICAO,OrigemConsulta.REAL)))) {
            assertThat(avaliador.avaliar(contexto(List.of(A,B),entradas)).risco()).isEqualTo(INCONCLUSIVO);
        }
    }
    @Test void consultaAntigaExigeAtualizacao() {
        var antiga = new ResultadoConsultaIMEI(A,SituacaoIMEI.SEM_RESTRICAO,OrigemConsulta.REAL,"Teste",Instant.now().minus(Duration.ofDays(2)),"");
        assertThat(avaliador.avaliar(contexto(List.of(A),List.of(antiga))).risco()).isEqualTo(INCONCLUSIVO);
    }
}
