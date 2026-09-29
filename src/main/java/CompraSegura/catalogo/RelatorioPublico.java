package CompraSegura.catalogo;

import CompraSegura.confiabilidade.*;
import CompraSegura.consulta.*;
import java.util.List;

public record RelatorioPublico(List<ConsultaPublica> consultas, AvaliacaoConfiabilidade avaliacao, boolean atual) {
    public record ConsultaPublica(String imei, SituacaoIMEI situacao, OrigemConsulta origem, String observacao) { }
    public static RelatorioPublico de(RelatorioVerificacao r) {
        if (r == null) return null;
        var a = r.avaliacao();
        var resumo = new AvaliacaoConfiabilidade(a.risco(), a.motivos().stream().map(RelatorioPublico::textoPublico).toList(),
            a.pendencias().stream().map(RelatorioPublico::textoPublico).toList(), a.contemSimulacao(), a.versaoRegra(), a.avaliadaEm());
        return new RelatorioPublico(r.consultas().stream().map(c -> new ConsultaPublica("•••••••••••" + c.imei().substring(11), c.situacao(), c.origem(), "")).toList(), resumo, r.atual());
    }
    private static String textoPublico(String texto) {
        return texto.replaceAll("(?<![0-9])[0-9]{11}([0-9]{4})(?![0-9])", "•••••••••••$1")
            .replace("O histórico do vendedor ainda não está disponível.", "O histórico de compras concluídas e avaliações de compradores ainda não está disponível.");
    }
    public String getRiscoDescricao() { return new RelatorioVerificacao(List.of(), avaliacao, atual).getRiscoDescricao(); }
}
