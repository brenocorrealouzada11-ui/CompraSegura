package CompraSegura.confiabilidade;

import CompraSegura.consulta.*;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import static CompraSegura.confiabilidade.AvaliacaoConfiabilidade.NivelRisco.*;
import static CompraSegura.confiabilidade.ContextoAvaliacao.SituacaoEvidencia.*;
import static CompraSegura.confiabilidade.ContextoAvaliacao.HistoricoVendedor.*;

@Component
public class AvaliadorPorRegras implements AvaliadorConfiabilidade {
    public static final String VERSAO = "qualitativa-v1";
    @Override
    public AvaliacaoConfiabilidade avaliar(ContextoAvaliacao contexto) {
        Instant agora = Instant.now();
        var motivos = new ArrayList<String>();
        var pendencias = new ArrayList<String>();
        var esperados = new HashSet<>(contexto.imeisEsperados());
        var porImei = contexto.consultas().stream().collect(Collectors.groupingBy(ResultadoConsultaIMEI::imei));
        if (esperados.isEmpty() || esperados.size() != contexto.imeisEsperados().size()) pendencias.add("A relação de IMEIs está vazia ou contém duplicações.");
        if (porImei.keySet().stream().anyMatch(i -> !esperados.contains(i))) pendencias.add("Há resultados de IMEIs que não pertencem a este aparelho.");
        boolean restricao = false;
        for (String imei : contexto.imeisEsperados()) {
            var resultados = porImei.getOrDefault(imei, List.of());
            if (resultados.size() != 1) pendencias.add("IMEI " + imei + ": consulta ausente ou duplicada.");
            for (var consulta : resultados) {
                // Uma restricao conhecida nunca desaparece porque outra consulta falhou.
                if (consulta.situacao() == SituacaoIMEI.COM_RESTRICAO) {
                    restricao = true;
                    motivos.add("IMEI " + imei + ": restrição indicada" + (consulta.origem() == OrigemConsulta.SIMULADA ? " pelo cenário simulado." : " pelo provedor."));
                }
                if (consulta.consultadoEm().isBefore(agora.minus(Duration.ofHours(24))) || consulta.consultadoEm().isAfter(agora.plusSeconds(60))) {
                    pendencias.add("IMEI " + imei + ": a data da consulta exige uma nova verificação.");
                }
                if (consulta.situacao() == SituacaoIMEI.INCONCLUSIVA) pendencias.add("IMEI " + imei + ": resultado inconclusivo.");
                if (consulta.situacao() == SituacaoIMEI.INDISPONIVEL) pendencias.add("IMEI " + imei + ": consulta indisponível.");
            }
        }
        switch (contexto.evidencia()) {
            case NAO_ENVIADA -> pendencias.add("A evidência dos IMEIs ainda não foi enviada.");
            case AGUARDANDO_CONFERENCIA -> pendencias.add("A imagem foi enviada, mas a correspondência com os IMEIs e o modelo ainda não foi conferida.");
            case INCONCLUSIVA -> pendencias.add("A evidência não permitiu confirmar os dados do aparelho.");
            case DIVERGENTE -> motivos.add("A conferência da evidência indicou divergência nos dados do aparelho.");
            case COERENTE -> motivos.add("A evidência foi marcada como coerente após conferência.");
        }
        if (contexto.historicoVendedor() == NAO_DISPONIVEL) pendencias.add("O histórico do vendedor ainda não está disponível.");
        if (contexto.historicoVendedor() == SEM_REGISTROS) motivos.add("O vendedor ainda não tem histórico suficiente; isso não é prova de risco nem garantia.");
        boolean ocorrencias = contexto.historicoVendedor() == COM_OCORRENCIAS;
        if (ocorrencias) motivos.add("Existem ocorrências no histórico do vendedor que exigem atenção.");
        String condicao = Objects.toString(contexto.condicaoDeclarada(), "");
        boolean defeitos = condicao.equals("Funciona com defeitos") || condicao.equals("Para retirada de peças");
        if (defeitos) motivos.add("O vendedor declarou defeitos ou venda para retirada de peças.");
        else motivos.add("Estado de conservação: " + condicao + ". Informação declarada pelo vendedor.");
        if (Objects.toString(contexto.reparosDeclarados(), "").equals("Já passou por reparos ou alterações")) {
            motivos.add("Há reparos declarados. Confira os detalhes nas observações; o reparo, por si só, não comprova irregularidade.");
        }
        boolean simulada = contexto.consultas().stream().anyMatch(c -> c.origem() == OrigemConsulta.SIMULADA);
        if (simulada) pendencias.add("Consultas simuladas: é necessária uma consulta real antes de usar este resultado para decidir uma compra.");
        var risco = restricao || contexto.evidencia() == DIVERGENTE ? ALTO
            : !pendencias.isEmpty() ? INCONCLUSIVO : defeitos || ocorrencias ? MODERADO : BAIXO;
        return new AvaliacaoConfiabilidade(risco, motivos, pendencias, simulada, VERSAO, agora);
    }
}
