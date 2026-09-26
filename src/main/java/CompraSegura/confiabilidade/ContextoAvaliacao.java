package CompraSegura.confiabilidade;

import CompraSegura.consulta.ResultadoConsultaIMEI;
import java.util.List;
import java.util.Objects;

/** Informacoes ausentes devem permanecer ausentes, sem valores favoraveis presumidos. */
public record ContextoAvaliacao(List<String> imeisEsperados, List<ResultadoConsultaIMEI> consultas,
                               SituacaoEvidencia evidencia, String condicaoDeclarada,
                               String reparosDeclarados, String observacoes,
                               HistoricoVendedor historicoVendedor) {
    public enum SituacaoEvidencia { NAO_ENVIADA, AGUARDANDO_CONFERENCIA, COERENTE, DIVERGENTE, INCONCLUSIVA }
    public enum HistoricoVendedor { NAO_DISPONIVEL, SEM_REGISTROS, SEM_OCORRENCIAS, COM_OCORRENCIAS }
    public ContextoAvaliacao {
        imeisEsperados = List.copyOf(imeisEsperados);
        consultas = List.copyOf(consultas);
        Objects.requireNonNull(evidencia);
        Objects.requireNonNull(historicoVendedor);
    }
}
