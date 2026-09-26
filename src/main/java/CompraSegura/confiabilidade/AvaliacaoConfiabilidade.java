package CompraSegura.confiabilidade;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Resultado explicavel. Nao e selo de autenticidade nem autorizacao para publicar. */
public record AvaliacaoConfiabilidade(NivelRisco risco, List<String> motivos,
                                      List<String> pendencias, boolean contemSimulacao,
                                      String versaoRegra, Instant avaliadaEm) {
    public enum NivelRisco { NAO_AVALIADO, INCONCLUSIVO, BAIXO, MODERADO, ALTO }
    public AvaliacaoConfiabilidade {
        Objects.requireNonNull(risco);
        motivos = List.copyOf(motivos);
        pendencias = List.copyOf(pendencias);
        Objects.requireNonNull(avaliadaEm);
        if (versaoRegra == null || versaoRegra.isBlank()) throw new IllegalArgumentException("Informe a versao da regra.");
    }
}
