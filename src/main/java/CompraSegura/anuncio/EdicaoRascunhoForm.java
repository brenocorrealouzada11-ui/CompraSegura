package CompraSegura.anuncio;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public class EdicaoRascunhoForm extends AnuncioForm {
    @NotNull(message = "Reabra a edição para carregar a versão atual do rascunho.")
    @PositiveOrZero(message = "A versão do rascunho é inválida. Reabra a edição.")
    private Long versao;
    // Apenas exibicao para rascunhos anteriores aos seletores; nao sao campos editaveis.
    private String condicaoAnterior;
    private String reparosAnteriores;
    public Long getVersao() { return versao; }
    public void setVersao(Long versao) { this.versao = versao; }
    public String getCondicaoAnterior() { return condicaoAnterior; }
    public void setCondicaoAnterior(String texto) { condicaoAnterior = texto; }
    public String getReparosAnteriores() { return reparosAnteriores; }
    public void setReparosAnteriores(String texto) { reparosAnteriores = texto; }
}
