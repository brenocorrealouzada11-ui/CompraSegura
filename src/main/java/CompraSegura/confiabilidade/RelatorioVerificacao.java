package CompraSegura.confiabilidade;

import CompraSegura.consulta.ResultadoConsultaIMEI;
import java.util.List;

public record RelatorioVerificacao(List<ResultadoConsultaIMEI> consultas, AvaliacaoConfiabilidade avaliacao, boolean atual) {
    public RelatorioVerificacao { consultas = List.copyOf(consultas); }
    public String getRiscoDescricao() {
        return switch (avaliacao.risco()) {
            case ALTO -> "Risco alto identificado";
            case MODERADO -> "Pontos de atenção identificados";
            case BAIXO -> "Risco baixo nos dados analisados";
            case INCONCLUSIVO -> "Avaliação inconclusiva";
            case NAO_AVALIADO -> "Ainda não avaliado";
        };
    }
}
