package CompraSegura.consulta;

import java.time.Instant;
import org.springframework.stereotype.Component;

/** Cenarios explicitamente ficticios; nao consulta nem envia IMEIs a terceiros. */
@Component
public class ProvedorSimuladoIMEI implements ProvedorConsultaIMEI {
    @Override public OrigemConsulta origem() { return OrigemConsulta.SIMULADA; }
    @Override public String nome() { return "Simulador local v1"; }
    @Override
    public ResultadoConsultaIMEI consultar(String imei) {
        var situacao = switch (imei) {
            case "000000000000001" -> SituacaoIMEI.SEM_RESTRICAO;
            case "000000000000002" -> SituacaoIMEI.COM_RESTRICAO;
            case "000000000000003" -> SituacaoIMEI.INCONCLUSIVA;
            case "000000000000004" -> SituacaoIMEI.INDISPONIVEL;
            default -> SituacaoIMEI.INCONCLUSIVA;
        };
        String observacao = switch (imei) {
            case "000000000000001" -> "Cenário fictício: consulta sem restrição.";
            case "000000000000002" -> "Cenário fictício: restrição encontrada.";
            case "000000000000003" -> "Cenário fictício: dados insuficientes para concluir.";
            case "000000000000004" -> "Cenário fictício: serviço indisponível.";
            default -> "Não há cenário de demonstração para este número. Nenhuma consulta real foi realizada.";
        };
        return new ResultadoConsultaIMEI(imei, situacao, OrigemConsulta.SIMULADA, "Simulador local v1", Instant.now(), observacao);
    }
}
